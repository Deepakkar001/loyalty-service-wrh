package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.model.ReferralMilestoneRule;
import com.loyaltyos.referrals.model.ReferralRuleCriteria;
import com.loyaltyos.referrals.model.ReferralRuleTrigger;
import com.loyaltyos.referrals.support.ReferralProgressTracker.ProgressState;
import com.loyaltyos.referrals.support.ReferralProgressTracker.PurchaseEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReferralRuleEvaluator {

    private ReferralRuleEvaluator() {}

    public static boolean isSatisfied(
        ReferralMilestoneRule rule,
        Referral referral,
        ProgressState progress,
        Instant now
    ) {
        if (rule == null) {
            return false;
        }
        return switch (rule.getTrigger()) {
            case LINK -> true;
            case PURCHASE -> isSatisfiedForPurchase(rule, referral, progress, now);
            case INTEGRATION_EVENT -> false;
        };
    }

    public static boolean isSatisfiedForIntegrationEvent(
        ReferralMilestoneRule rule,
        String eventType,
        Map<String, Object> eventMetadata
    ) {
        if (rule == null || rule.getTrigger() != ReferralRuleTrigger.INTEGRATION_EVENT) {
            return false;
        }
        if (eventType == null || eventType.isBlank()) {
            return false;
        }
        if ("PURCHASE".equalsIgnoreCase(eventType.trim())) {
            return false;
        }
        ReferralRuleCriteria criteria = rule.getCriteria();
        List<String> allowed = criteria.getEventTypes();
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        String normalized = eventType.trim().toUpperCase(Locale.ROOT);
        for (String raw : allowed) {
            if (raw != null && normalized.equals(raw.trim().toUpperCase(Locale.ROOT))) {
                return ReferralCriteriaMatcher.matchesEventMetadata(criteria, eventMetadata);
            }
        }
        return false;
    }

    private static boolean isSatisfiedForPurchase(
        ReferralMilestoneRule rule,
        Referral referral,
        ProgressState progress,
        Instant now
    ) {
        ReferralRuleCriteria criteria = rule.getCriteria();
        List<PurchaseEvent> purchases = progress.getPurchases() != null ? progress.getPurchases() : List.of();

        Integer windowDays = criteria.getWindowDays();
        List<PurchaseEvent> scoped = windowDays != null && windowDays > 0
            ? ReferralProgressTracker.purchasesInWindow(purchases, windowDays, now)
            : purchases;
        scoped = filterPurchases(scoped, criteria);

        int purchaseCount = windowDays != null && windowDays > 0
            ? scoped.size()
            : referral.getPurchaseCount();

        if (Boolean.TRUE.equals(criteria.getFirstPurchaseOnly())) {
            return purchaseCount >= 1 || referral.getPurchaseCount() >= 1;
        }

        int minPurchases = criteria.getMinPurchaseCount() != null && criteria.getMinPurchaseCount() > 0
            ? criteria.getMinPurchaseCount()
            : 1;
        if (purchaseCount < minPurchases) {
            return false;
        }

        if (criteria.getMinSpend() != null && criteria.getMinSpend().signum() > 0) {
            BigDecimal spend = windowDays != null && windowDays > 0
                ? ReferralProgressTracker.spendInWindow(scoped)
                : referral.getTotalSpend() != null ? referral.getTotalSpend() : BigDecimal.ZERO;
            if (spend.compareTo(criteria.getMinSpend()) < 0) {
                return false;
            }
        }

        return true;
    }

    private static List<PurchaseEvent> filterPurchases(List<PurchaseEvent> purchases, ReferralRuleCriteria criteria) {
        if (!ReferralCriteriaMatcher.hasAnyMetadataFilter(criteria)) {
            return purchases;
        }
        return purchases.stream()
            .filter(p -> ReferralCriteriaMatcher.matchesPurchaseEvent(p, criteria))
            .toList();
    }
}

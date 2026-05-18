package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.model.CampaignAwardType;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Proxy reward estimates for conflict resolution (BEST_OFFER) before rule evaluation runs.
 */
public final class CampaignRewardEstimateHelper {

    private CampaignRewardEstimateHelper() {}

    public static BigDecimal estimate(Campaign campaign, CampaignOfferConfig offer, CampaignEventContext event) {
        Objects.requireNonNull(campaign, "campaign");
        if (offer == null || offer.awardType() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = event.amount() == null ? BigDecimal.ZERO : event.amount();
        return switch (offer.awardType().trim()) {
            case CampaignAwardType.POINTS_BONUS -> offer.bonusPoints() != null ? offer.bonusPoints() : BigDecimal.ZERO;
            case CampaignAwardType.MULTIPLIER_ON_RULE_POINTS -> {
                if (offer.multiplierOnRulePoints() == null) {
                    yield BigDecimal.ZERO;
                }
                BigDecimal base = amount.max(BigDecimal.ONE);
                yield base.multiply(offer.multiplierOnRulePoints().subtract(BigDecimal.ONE)).max(BigDecimal.ZERO);
            }
            case CampaignAwardType.FLAT_CASHBACK -> offer.cashbackValue() != null ? offer.cashbackValue() : BigDecimal.ZERO;
            case CampaignAwardType.PERCENT_CASHBACK -> {
                if (offer.cashbackValue() == null) {
                    yield BigDecimal.ZERO;
                }
                yield amount.multiply(offer.cashbackValue())
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            }
            default -> BigDecimal.ZERO;
        };
    }
}

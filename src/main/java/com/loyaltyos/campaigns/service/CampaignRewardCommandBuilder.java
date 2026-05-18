package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.model.CampaignAwardType;
import com.loyaltyos.campaigns.model.CampaignBuiltAward;
import com.loyaltyos.campaigns.model.CampaignEventContext;
import com.loyaltyos.campaigns.model.CampaignOfferConfig;
import com.loyaltyos.onboarding.rewards.dto.RewardIssueCommandDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CampaignRewardCommandBuilder {

    private final CampaignJsonSupport jsonSupport;

    public CampaignRewardCommandBuilder(CampaignJsonSupport jsonSupport) {
        this.jsonSupport = Objects.requireNonNull(jsonSupport, "jsonSupport");
    }

    /**
     * Computes award amounts and optional ledger commands for each applying campaign.
     *
     * @param finalRulePoints {@link com.loyaltyos.onboarding.rules.dto.RuleEvaluationResponse#getFinalPointsAwarded()}
     */
    public List<CampaignBuiltAward> build(
        List<Campaign> applyingCampaigns,
        CampaignEventContext event,
        String eventId,
        BigDecimal finalRulePoints,
        Instant defaultCreditExpiresAt
    ) {
        Objects.requireNonNull(applyingCampaigns, "applyingCampaigns");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(eventId, "eventId");

        BigDecimal ruleBase = finalRulePoints == null ? BigDecimal.ZERO : finalRulePoints;
        List<CampaignBuiltAward> awards = new ArrayList<>();

        for (Campaign campaign : applyingCampaigns) {
            CampaignOfferConfig offer = jsonSupport.parseOfferConfig(campaign.getOfferConfig());
            if (offer == null || offer.awardType() == null || offer.awardType().isBlank()) {
                continue;
            }

            String awardType = offer.awardType().trim();
            String offerLine = awardType;
            BigDecimal points = BigDecimal.ZERO;
            BigDecimal cashback = BigDecimal.ZERO;

            switch (awardType) {
                case CampaignAwardType.POINTS_BONUS -> {
                    if (offer.bonusPoints() != null && offer.bonusPoints().signum() > 0) {
                        points = offer.bonusPoints().setScale(4, RoundingMode.HALF_UP);
                    }
                }
                case CampaignAwardType.MULTIPLIER_ON_RULE_POINTS -> {
                    if (offer.multiplierOnRulePoints() != null
                        && offer.multiplierOnRulePoints().compareTo(BigDecimal.ONE) > 0
                        && ruleBase.signum() > 0) {
                        BigDecimal bonusFactor = offer.multiplierOnRulePoints().subtract(BigDecimal.ONE);
                        points = ruleBase.multiply(bonusFactor).setScale(4, RoundingMode.HALF_UP);
                    }
                }
                case CampaignAwardType.FLAT_CASHBACK -> {
                    if (offer.cashbackValue() != null && offer.cashbackValue().signum() > 0) {
                        cashback = offer.cashbackValue().setScale(4, RoundingMode.HALF_UP);
                    }
                }
                case CampaignAwardType.PERCENT_CASHBACK -> {
                    if (offer.cashbackValue() != null && offer.cashbackValue().signum() > 0) {
                        BigDecimal amount = event.amount() == null ? BigDecimal.ZERO : event.amount();
                        cashback = amount.multiply(offer.cashbackValue())
                            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    }
                }
                default -> { }
            }

            if (points.signum() <= 0 && cashback.signum() <= 0) {
                continue;
            }

            BigDecimal budgetCost = points.signum() > 0 ? points : cashback;
            RewardIssueCommandDto command = null;
            if (points.signum() > 0) {
                command = new RewardIssueCommandDto();
                command.setIdempotencyKey(campaignIdempotencyKey(campaign.getCampaignUid(), eventId, offerLine));
                command.setSourceCampaignUid(campaign.getCampaignUid());
                command.setPointsToAward(points);
                command.setExpiresAt(resolveExpiresAt(offer, defaultCreditExpiresAt));
                command.setActionType("AWARD_POINTS");
            }

            awards.add(new CampaignBuiltAward(
                campaign.getCampaignUid(),
                campaign.getName(),
                offerLine,
                points,
                cashback,
                budgetCost,
                command
            ));
        }

        return awards;
    }

    public static String campaignIdempotencyKey(String campaignUid, String eventId, String offerLine) {
        String line = offerLine == null || offerLine.isBlank() ? "0" : offerLine.trim();
        String key = "camp:" + campaignUid + ":" + eventId + ":" + line;
        if (key.length() > 128) {
            return key.substring(0, 128);
        }
        return key;
    }

    private static Instant resolveExpiresAt(CampaignOfferConfig offer, Instant defaultCreditExpiresAt) {
        if (offer.expiryDays() != null && offer.expiryDays() > 0) {
            return Instant.now().atZone(ZoneOffset.UTC).plusDays(offer.expiryDays()).toInstant();
        }
        return defaultCreditExpiresAt;
    }
}

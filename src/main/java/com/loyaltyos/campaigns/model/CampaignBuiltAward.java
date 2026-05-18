package com.loyaltyos.campaigns.model;

import com.loyaltyos.onboarding.rewards.dto.RewardIssueCommandDto;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One resolved campaign offer line after award calculation (points and/or cashback).
 */
public record CampaignBuiltAward(
    String campaignUid,
    String campaignName,
    String offerLine,
    BigDecimal pointsAwarded,
    BigDecimal cashbackAmount,
    BigDecimal budgetCost,
    RewardIssueCommandDto issueCommand
) {
    public CampaignBuiltAward {
        Objects.requireNonNull(campaignUid, "campaignUid");
        Objects.requireNonNull(offerLine, "offerLine");
        if (pointsAwarded == null) {
            pointsAwarded = BigDecimal.ZERO;
        }
        if (cashbackAmount == null) {
            cashbackAmount = BigDecimal.ZERO;
        }
        if (budgetCost == null) {
            budgetCost = BigDecimal.ZERO;
        }
    }

    public boolean hasPoints() {
        return pointsAwarded.signum() > 0;
    }

    public boolean hasCashback() {
        return cashbackAmount.signum() > 0;
    }
}

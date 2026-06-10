package com.loyaltyos.campaigns.dto;

import java.math.BigDecimal;

public record CampaignParticipationTrendRow(
    String period,
    long participations,
    BigDecimal pointsIssued,
    BigDecimal cashbackRecorded
) {}

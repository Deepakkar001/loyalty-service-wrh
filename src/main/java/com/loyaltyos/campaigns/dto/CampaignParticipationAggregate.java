package com.loyaltyos.campaigns.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CampaignParticipationAggregate(
    String campaignUid,
    long participations,
    long uniqueCustomers,
    BigDecimal pointsIssued,
    BigDecimal cashbackRecorded,
    Instant firstParticipationAt,
    Instant lastParticipationAt
) {}

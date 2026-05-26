package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record DashboardVolumePoint(
    String date,
    BigDecimal issued,
    BigDecimal redeemed
) {}

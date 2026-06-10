package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record TenantFinanceContext(BigDecimal pointsCurrencyRate, String currency) {}

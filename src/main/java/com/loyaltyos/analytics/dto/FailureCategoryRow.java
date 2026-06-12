package com.loyaltyos.analytics.dto;

public record FailureCategoryRow(
    String category,
    String transactionType,
    long failureCount
) {}

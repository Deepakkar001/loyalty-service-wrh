package com.loyaltyos.analytics.dto;

public record EnrollmentSummary(
    long newEnrollmentsInPeriod,
    long newEnrollmentsPriorPeriod,
    long totalEnrolledMembers,
    long returningActiveInPeriod,
    long newEnrollmentsYtd
) {}

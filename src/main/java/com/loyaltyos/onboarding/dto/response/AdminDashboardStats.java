package com.loyaltyos.onboarding.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardStats {
    private long totalTenants;
    private long pendingEmailVerification;
    private long emailVerified;
    private long agreementPending;
    private long agreementSigned;
    private long activeTenants;
    private long suspendedTenants;
    private long terminatedTenants;

    private long totalAgreements;
    private long pendingApprovalAgreements;
    private long approvedAgreements;
    private long rejectedAgreements;

    private long registrationsToday;
    private long registrationsThisWeek;
    private long registrationsThisMonth;
}

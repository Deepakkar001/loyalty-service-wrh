package com.loyaltyos.onboarding.dto.response;

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

    public AdminDashboardStats() {}

    public AdminDashboardStats(
        long totalTenants,
        long pendingEmailVerification,
        long emailVerified,
        long agreementPending,
        long agreementSigned,
        long activeTenants,
        long suspendedTenants,
        long terminatedTenants,
        long totalAgreements,
        long pendingApprovalAgreements,
        long approvedAgreements,
        long rejectedAgreements,
        long registrationsToday,
        long registrationsThisWeek,
        long registrationsThisMonth
    ) {
        this.totalTenants = totalTenants;
        this.pendingEmailVerification = pendingEmailVerification;
        this.emailVerified = emailVerified;
        this.agreementPending = agreementPending;
        this.agreementSigned = agreementSigned;
        this.activeTenants = activeTenants;
        this.suspendedTenants = suspendedTenants;
        this.terminatedTenants = terminatedTenants;
        this.totalAgreements = totalAgreements;
        this.pendingApprovalAgreements = pendingApprovalAgreements;
        this.approvedAgreements = approvedAgreements;
        this.rejectedAgreements = rejectedAgreements;
        this.registrationsToday = registrationsToday;
        this.registrationsThisWeek = registrationsThisWeek;
        this.registrationsThisMonth = registrationsThisMonth;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
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

        private Builder() {}

        public Builder totalTenants(long totalTenants) { this.totalTenants = totalTenants; return this; }
        public Builder pendingEmailVerification(long pendingEmailVerification) { this.pendingEmailVerification = pendingEmailVerification; return this; }
        public Builder emailVerified(long emailVerified) { this.emailVerified = emailVerified; return this; }
        public Builder agreementPending(long agreementPending) { this.agreementPending = agreementPending; return this; }
        public Builder agreementSigned(long agreementSigned) { this.agreementSigned = agreementSigned; return this; }
        public Builder activeTenants(long activeTenants) { this.activeTenants = activeTenants; return this; }
        public Builder suspendedTenants(long suspendedTenants) { this.suspendedTenants = suspendedTenants; return this; }
        public Builder terminatedTenants(long terminatedTenants) { this.terminatedTenants = terminatedTenants; return this; }
        public Builder totalAgreements(long totalAgreements) { this.totalAgreements = totalAgreements; return this; }
        public Builder pendingApprovalAgreements(long pendingApprovalAgreements) { this.pendingApprovalAgreements = pendingApprovalAgreements; return this; }
        public Builder approvedAgreements(long approvedAgreements) { this.approvedAgreements = approvedAgreements; return this; }
        public Builder rejectedAgreements(long rejectedAgreements) { this.rejectedAgreements = rejectedAgreements; return this; }
        public Builder registrationsToday(long registrationsToday) { this.registrationsToday = registrationsToday; return this; }
        public Builder registrationsThisWeek(long registrationsThisWeek) { this.registrationsThisWeek = registrationsThisWeek; return this; }
        public Builder registrationsThisMonth(long registrationsThisMonth) { this.registrationsThisMonth = registrationsThisMonth; return this; }

        public AdminDashboardStats build() {
            return new AdminDashboardStats(
                totalTenants,
                pendingEmailVerification,
                emailVerified,
                agreementPending,
                agreementSigned,
                activeTenants,
                suspendedTenants,
                terminatedTenants,
                totalAgreements,
                pendingApprovalAgreements,
                approvedAgreements,
                rejectedAgreements,
                registrationsToday,
                registrationsThisWeek,
                registrationsThisMonth
            );
        }
    }

    public long getTotalTenants() { return totalTenants; }
    public long getPendingEmailVerification() { return pendingEmailVerification; }
    public long getEmailVerified() { return emailVerified; }
    public long getAgreementPending() { return agreementPending; }
    public long getAgreementSigned() { return agreementSigned; }
    public long getActiveTenants() { return activeTenants; }
    public long getSuspendedTenants() { return suspendedTenants; }
    public long getTerminatedTenants() { return terminatedTenants; }
    public long getTotalAgreements() { return totalAgreements; }
    public long getPendingApprovalAgreements() { return pendingApprovalAgreements; }
    public long getApprovedAgreements() { return approvedAgreements; }
    public long getRejectedAgreements() { return rejectedAgreements; }
    public long getRegistrationsToday() { return registrationsToday; }
    public long getRegistrationsThisWeek() { return registrationsThisWeek; }
    public long getRegistrationsThisMonth() { return registrationsThisMonth; }
}

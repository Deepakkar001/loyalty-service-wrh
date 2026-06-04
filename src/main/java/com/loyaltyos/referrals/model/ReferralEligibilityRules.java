package com.loyaltyos.referrals.model;

/**
 * Admin-configurable eligibility (defaults preserve Phase 1 behaviour).
 */
public class ReferralEligibilityRules {

    /** BRD §6.1: block link when referee already has ledger activity. */
    private boolean refereeMustBeNewCustomer = true;

    /** Require referrer to have at least one ledger row before issuing a code / linking. */
    private boolean referrerMustHaveLedgerActivity = false;

    public boolean isRefereeMustBeNewCustomer() {
        return refereeMustBeNewCustomer;
    }

    public void setRefereeMustBeNewCustomer(boolean refereeMustBeNewCustomer) {
        this.refereeMustBeNewCustomer = refereeMustBeNewCustomer;
    }

    public boolean isReferrerMustHaveLedgerActivity() {
        return referrerMustHaveLedgerActivity;
    }

    public void setReferrerMustHaveLedgerActivity(boolean referrerMustHaveLedgerActivity) {
        this.referrerMustHaveLedgerActivity = referrerMustHaveLedgerActivity;
    }
}

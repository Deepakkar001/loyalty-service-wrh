package com.loyaltyos.campaigns.enums;

/**
 * How campaign points are earned at runtime.
 * {@link #RULE_GATED} requires an ACTIVE CAMPAIGN earn rule; {@link #LEGACY_OFFER} uses offer_config (migration bridge).
 */
public enum CampaignExecutionMode {
    RULE_GATED,
    LEGACY_OFFER
}

package com.loyaltyos.onboarding.event;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Published to Kafka topic: platform.config.updates
 * Kafka key: tenantId
 */
@Getter
@Builder
public class TenantActivatedEvent {

    private final String schemaVersion = "1.0";

    private String tenantId;
    private String slug;
    private String companyName;
    private IdentityMode identityMode;
    private SubscriptionTier subscriptionTier;
    private DataResidencyRegion dataResidencyRegion;
    private String businessCategory;
    private int maxActiveRules;
    private List<String> enabledFeatures;
    private Instant activatedAt;
    private String activatedByAdminId;
}


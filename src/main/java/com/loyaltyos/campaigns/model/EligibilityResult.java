package com.loyaltyos.campaigns.model;

import com.loyaltyos.campaigns.entity.Campaign;
import java.util.List;

public record EligibilityResult(
    List<Campaign> qualifying,
    List<DroppedCampaign> dropped
) {
}

package com.loyaltyos.campaigns.model;

import com.loyaltyos.campaigns.entity.Campaign;
import java.util.List;

public record CampaignResolutionResult(
    List<Campaign> applying,
    List<DroppedCampaign> dropped,
    boolean capApplied,
    String resolutionMode
) {
}

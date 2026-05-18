package com.loyaltyos.campaigns.model;

import com.loyaltyos.campaigns.enums.DropReason;

public record DroppedCampaign(
    String campaignUid,
    String campaignName,
    DropReason dropReason
) {
}

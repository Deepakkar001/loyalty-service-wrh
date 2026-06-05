package com.loyaltyos.campaigns.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CampaignTargetCustomersBulkRequest {

    @NotEmpty(message = "customerIds must not be empty")
    @Size(max = 50000, message = "customerIds exceeds maximum batch size")
    private List<@Size(min = 1, max = 128) String> customerIds;

    public List<String> getCustomerIds() {
        return customerIds;
    }

    public void setCustomerIds(List<String> customerIds) {
        this.customerIds = customerIds;
    }
}

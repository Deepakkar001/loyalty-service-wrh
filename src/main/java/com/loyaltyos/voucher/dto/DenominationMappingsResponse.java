package com.loyaltyos.voucher.dto;

import java.util.List;

public class DenominationMappingsResponse {

    private String catalogRewardUid;
    private boolean mixedDenominationEnabled;
    private List<DenominationMappingDto> mappings;

    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public boolean isMixedDenominationEnabled() { return mixedDenominationEnabled; }
    public void setMixedDenominationEnabled(boolean mixedDenominationEnabled) {
        this.mixedDenominationEnabled = mixedDenominationEnabled;
    }
    public List<DenominationMappingDto> getMappings() { return mappings; }
    public void setMappings(List<DenominationMappingDto> mappings) { this.mappings = mappings; }
}

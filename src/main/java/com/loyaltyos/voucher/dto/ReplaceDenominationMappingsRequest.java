package com.loyaltyos.voucher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ReplaceDenominationMappingsRequest {

    @NotBlank
    @Size(max = 64)
    private String catalogRewardUid;

    @NotEmpty
    @Valid
    private List<DenominationMappingItemRequest> mappings;

    public String getCatalogRewardUid() { return catalogRewardUid; }
    public void setCatalogRewardUid(String catalogRewardUid) { this.catalogRewardUid = catalogRewardUid; }
    public List<DenominationMappingItemRequest> getMappings() { return mappings; }
    public void setMappings(List<DenominationMappingItemRequest> mappings) { this.mappings = mappings; }
}

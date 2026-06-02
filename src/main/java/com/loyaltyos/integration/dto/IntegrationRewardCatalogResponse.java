package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class IntegrationRewardCatalogResponse {

    private String tenantId;
    private String programmeUid;
    private int version;
    private List<RewardTypeDto> rewardTypes;
    private List<RewardItemDto> items;

    public static class RewardTypeDto {
        private String typeCode;
        private String label;
        private String description;

        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class RewardItemDto {
        private String rewardUid;
        private String name;
        private String rewardType;
        private String status;
        private BigDecimal pointsCost;
        private int displayOrder;
        private String description;
        private Map<String, Object> metadata;
        /** Populated for VOUCHER items when inventory module is enabled. */
        private Long availableCount;

        public String getRewardUid() { return rewardUid; }
        public void setRewardUid(String rewardUid) { this.rewardUid = rewardUid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRewardType() { return rewardType; }
        public void setRewardType(String rewardType) { this.rewardType = rewardType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getPointsCost() { return pointsCost; }
        public void setPointsCost(BigDecimal pointsCost) { this.pointsCost = pointsCost; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public Long getAvailableCount() { return availableCount; }
        public void setAvailableCount(Long availableCount) { this.availableCount = availableCount; }
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public List<RewardTypeDto> getRewardTypes() { return rewardTypes; }
    public void setRewardTypes(List<RewardTypeDto> rewardTypes) { this.rewardTypes = rewardTypes; }
    public List<RewardItemDto> getItems() { return items; }
    public void setItems(List<RewardItemDto> items) { this.items = items; }
}

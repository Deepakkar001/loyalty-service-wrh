package com.loyaltyos.onboarding.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class PortalRewardCatalogResponse {

    private String tenantId;
    private String programmeUid;
    private int activeConfigVersion;
    private JsonNode rewardCatalog;
    private List<Integer> mergedConfigVersions;
    private List<String> synthesizedRewardUids;
    private int itemCount;
    private int voucherBatchCount;

    public PortalRewardCatalogResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String tenantId;
        private String programmeUid;
        private int activeConfigVersion;
        private JsonNode rewardCatalog;
        private List<Integer> mergedConfigVersions;
        private List<String> synthesizedRewardUids;
        private int itemCount;
        private int voucherBatchCount;

        private Builder() {}

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder programmeUid(String programmeUid) {
            this.programmeUid = programmeUid;
            return this;
        }

        public Builder activeConfigVersion(int activeConfigVersion) {
            this.activeConfigVersion = activeConfigVersion;
            return this;
        }

        public Builder rewardCatalog(JsonNode rewardCatalog) {
            this.rewardCatalog = rewardCatalog;
            return this;
        }

        public Builder mergedConfigVersions(List<Integer> mergedConfigVersions) {
            this.mergedConfigVersions = mergedConfigVersions;
            return this;
        }

        public Builder synthesizedRewardUids(List<String> synthesizedRewardUids) {
            this.synthesizedRewardUids = synthesizedRewardUids;
            return this;
        }

        public Builder itemCount(int itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public Builder voucherBatchCount(int voucherBatchCount) {
            this.voucherBatchCount = voucherBatchCount;
            return this;
        }

        public PortalRewardCatalogResponse build() {
            PortalRewardCatalogResponse r = new PortalRewardCatalogResponse();
            r.tenantId = tenantId;
            r.programmeUid = programmeUid;
            r.activeConfigVersion = activeConfigVersion;
            r.rewardCatalog = rewardCatalog;
            r.mergedConfigVersions = mergedConfigVersions;
            r.synthesizedRewardUids = synthesizedRewardUids;
            r.itemCount = itemCount;
            r.voucherBatchCount = voucherBatchCount;
            return r;
        }
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public int getActiveConfigVersion() {
        return activeConfigVersion;
    }

    public JsonNode getRewardCatalog() {
        return rewardCatalog;
    }

    public List<Integer> getMergedConfigVersions() {
        return mergedConfigVersions;
    }

    public List<String> getSynthesizedRewardUids() {
        return synthesizedRewardUids;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getVoucherBatchCount() {
        return voucherBatchCount;
    }
}

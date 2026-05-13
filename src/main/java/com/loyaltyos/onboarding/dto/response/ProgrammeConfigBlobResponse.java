package com.loyaltyos.onboarding.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public class ProgrammeConfigBlobResponse {
    private String tenantId;
    private String programmeUid;
    private Integer configVersion;
    private JsonNode config;

    public ProgrammeConfigBlobResponse() {}

    public ProgrammeConfigBlobResponse(String tenantId, String programmeUid, Integer configVersion, JsonNode config) {
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.configVersion = configVersion;
        this.config = config;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String programmeUid;
        private Integer configVersion;
        private JsonNode config;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder configVersion(Integer configVersion) { this.configVersion = configVersion; return this; }
        public Builder config(JsonNode config) { this.config = config; return this; }

        public ProgrammeConfigBlobResponse build() {
            return new ProgrammeConfigBlobResponse(tenantId, programmeUid, configVersion, config);
        }
    }

    public String getTenantId() { return tenantId; }
    public String getProgrammeUid() { return programmeUid; }
    public Integer getConfigVersion() { return configVersion; }
    public JsonNode getConfig() { return config; }
}


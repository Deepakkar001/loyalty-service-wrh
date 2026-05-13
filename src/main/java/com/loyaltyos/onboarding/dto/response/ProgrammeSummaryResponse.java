package com.loyaltyos.onboarding.dto.response;

public class ProgrammeSummaryResponse {
    private String programmeUid;
    private String name;
    private String status;
    private Integer activeConfigVersion;

    public ProgrammeSummaryResponse() {}

    public ProgrammeSummaryResponse(String programmeUid, String name, String status, Integer activeConfigVersion) {
        this.programmeUid = programmeUid;
        this.name = name;
        this.status = status;
        this.activeConfigVersion = activeConfigVersion;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String programmeUid;
        private String name;
        private String status;
        private Integer activeConfigVersion;

        private Builder() {}

        public Builder programmeUid(String programmeUid) { this.programmeUid = programmeUid; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder activeConfigVersion(Integer activeConfigVersion) { this.activeConfigVersion = activeConfigVersion; return this; }

        public ProgrammeSummaryResponse build() {
            return new ProgrammeSummaryResponse(programmeUid, name, status, activeConfigVersion);
        }
    }

    public String getProgrammeUid() { return programmeUid; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Integer getActiveConfigVersion() { return activeConfigVersion; }
}


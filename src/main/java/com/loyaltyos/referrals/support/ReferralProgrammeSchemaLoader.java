package com.loyaltyos.referrals.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.service.ProgrammeService;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ReferralProgrammeSchemaLoader {

    private final ProgrammeService programmeService;
    private final ObjectMapper objectMapper;

    public ReferralProgrammeSchemaLoader(ProgrammeService programmeService, ObjectMapper objectMapper) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public ReferralProgrammeEventSchemaCatalog loadForProgramme(String tenantId, String programmeUid) {
        String uid = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
        ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(tenantId, uid);
        if (cfg == null || cfg.getConfigJson() == null || cfg.getConfigJson().isBlank()) {
            return ReferralProgrammeEventSchemaCatalog.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(cfg.getConfigJson());
            return ReferralProgrammeEventSchemaCatalog.fromProgrammeConfigJson(root);
        } catch (Exception e) {
            return ReferralProgrammeEventSchemaCatalog.empty();
        }
    }
}

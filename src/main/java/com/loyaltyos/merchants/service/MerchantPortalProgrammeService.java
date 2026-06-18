package com.loyaltyos.merchants.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.dto.ProgrammeConfigBlobResponse;
import com.loyaltyos.onboarding.dto.ProgrammeSummaryResponse;
import com.loyaltyos.onboarding.entity.Programme;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.service.ProgrammeService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantPortalProgrammeService {

    private final ProgrammeService programmeService;
    private final ObjectMapper objectMapper;

    public MerchantPortalProgrammeService(ProgrammeService programmeService, ObjectMapper objectMapper) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional(readOnly = true)
    public List<ProgrammeSummaryResponse> listProgrammes(String tenantId) {
        return programmeService.listProgrammes(tenantId).stream()
            .filter(p -> p.getStatus() == Programme.ProgrammeStatus.ACTIVE)
            .map(p -> ProgrammeSummaryResponse.builder()
                .programmeUid(p.getProgrammeUid())
                .name(p.getName())
                .status(p.getStatus().name())
                .activeConfigVersion(p.getActiveConfigVersion())
                .build())
            .toList();
    }

    @Transactional(readOnly = true)
    public ProgrammeConfigBlobResponse getProgrammeConfig(String tenantId, String programmeUid) {
        ProgrammeConfig cfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
        if (cfg == null) {
            return ProgrammeConfigBlobResponse.builder()
                .tenantId(tenantId)
                .programmeUid(programmeUid)
                .configVersion(0)
                .config(objectMapper.createObjectNode())
                .build();
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(cfg.getConfigJson());
        } catch (Exception e) {
            node = objectMapper.createObjectNode();
        }
        return ProgrammeConfigBlobResponse.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .configVersion(cfg.getConfigVersion())
            .config(node)
            .build();
    }
}

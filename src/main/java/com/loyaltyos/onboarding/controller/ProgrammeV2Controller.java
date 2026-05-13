package com.loyaltyos.onboarding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.dto.request.CreateProgrammeRequest;
import com.loyaltyos.onboarding.dto.request.UpsertProgrammeConfigRequest;
import com.loyaltyos.onboarding.dto.response.ProgrammeConfigBlobResponse;
import com.loyaltyos.onboarding.dto.response.ProgrammeSummaryResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.ProgrammeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Programmes (v2)", description = "Multi-programme management and per-programme configuration")
public class ProgrammeV2Controller {

    private final ProgrammeService programmeService;
    private final ObjectMapper objectMapper;

    public ProgrammeV2Controller(ProgrammeService programmeService, ObjectMapper objectMapper) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @GetMapping("/api/v2/programmes")
    @Operation(summary = "List programmes for my tenant")
    public ResponseEntity<List<ProgrammeSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        var rows = programmeService.listProgrammes(tenantId);
        var out = rows.stream().map(p -> ProgrammeSummaryResponse.builder()
            .programmeUid(p.getProgrammeUid())
            .name(p.getName())
            .status(p.getStatus().name())
            .activeConfigVersion(p.getActiveConfigVersion())
            .build()
        ).toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping("/api/v2/programmes")
    @Operation(summary = "Create a new programme for my tenant")
    public ResponseEntity<ProgrammeSummaryResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateProgrammeRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var p = programmeService.createProgramme(tenantId, request.getName());
        return ResponseEntity.ok(ProgrammeSummaryResponse.builder()
            .programmeUid(p.getProgrammeUid())
            .name(p.getName())
            .status(p.getStatus().name())
            .activeConfigVersion(p.getActiveConfigVersion())
            .build());
    }

    @GetMapping("/api/v2/programmes/{programmeUid}/config")
    @Operation(summary = "Get active config for programme")
    public ResponseEntity<ProgrammeConfigBlobResponse> getConfig(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var cfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
        if (cfg == null) {
            return ResponseEntity.ok(ProgrammeConfigBlobResponse.builder()
                .tenantId(tenantId)
                .programmeUid(programmeUid)
                .configVersion(0)
                .config(objectMapper.createObjectNode())
                .build());
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(cfg.getConfigJson());
        } catch (Exception e) {
            node = objectMapper.createObjectNode();
        }
        return ResponseEntity.ok(ProgrammeConfigBlobResponse.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .configVersion(cfg.getConfigVersion())
            .config(node)
            .build());
    }

    @PutMapping("/api/v2/programmes/{programmeUid}/config")
    @Operation(summary = "Upsert programme config (versioned)")
    public ResponseEntity<ProgrammeConfigBlobResponse> upsertConfig(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid,
        @Valid @RequestBody UpsertProgrammeConfigRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var saved = programmeService.saveConfig(tenantId, programmeUid, request.getConfig(), tenantId, "TENANT");
        return ResponseEntity.ok(ProgrammeConfigBlobResponse.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .configVersion(saved.getConfigVersion())
            .config(request.getConfig())
            .build());
    }
}


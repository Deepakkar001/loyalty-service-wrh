package com.loyaltyos.onboarding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.dto.CreateProgrammeRequest;
import com.loyaltyos.onboarding.dto.EventDefinitionRequest;
import com.loyaltyos.onboarding.dto.EventSchemaSettingsPatchRequest;
import com.loyaltyos.onboarding.dto.ProgrammeStatusPatchRequest;
import com.loyaltyos.onboarding.dto.UpdateProgrammeRequest;
import com.loyaltyos.onboarding.dto.UpsertProgrammeConfigRequest;
import com.loyaltyos.onboarding.dto.PortalRewardCatalogResponse;
import com.loyaltyos.onboarding.dto.ProgrammeConfigBlobResponse;
import com.loyaltyos.onboarding.dto.ProgrammeSummaryResponse;
import com.loyaltyos.onboarding.dto.RewardCatalogRecoveryResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.ProgrammeService;
import com.loyaltyos.rewards.catalog.RewardCatalogDbMergeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Programmes (v2)", description = "Multi-programme management and per-programme configuration")
public class ProgrammeV2Controller {

    private final ProgrammeService programmeService;
    private final RewardCatalogDbMergeService rewardCatalogDbMergeService;
    private final ObjectMapper objectMapper;

    public ProgrammeV2Controller(
        ProgrammeService programmeService,
        RewardCatalogDbMergeService rewardCatalogDbMergeService,
        ObjectMapper objectMapper
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.rewardCatalogDbMergeService = Objects.requireNonNull(rewardCatalogDbMergeService);
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

    @PatchMapping("/api/v2/programmes/{programmeUid}/status")
    @Operation(summary = "Activate or deactivate programme",
        description = "ACTIVE programmes accept integration traffic; DRAFT programmes are inactive. "
            + "Configuration must be saved before activating. ARCHIVED programmes cannot be changed here.")
    public ResponseEntity<ProgrammeSummaryResponse> patchStatus(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid,
        @Valid @RequestBody ProgrammeStatusPatchRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var p = programmeService.updateProgrammeStatus(
            tenantId, programmeUid, request.getStatus(), tenantId, "TENANT"
        );
        return ResponseEntity.ok(ProgrammeSummaryResponse.builder()
            .programmeUid(p.getProgrammeUid())
            .name(p.getName())
            .status(p.getStatus().name())
            .activeConfigVersion(p.getActiveConfigVersion())
            .build());
    }

    @PatchMapping("/api/v2/programmes/{programmeUid}")
    @Operation(summary = "Rename programme",
        description = "Updates programmes.name and programmeIdentity.programmeName in the active config (new version when config exists).")
    public ResponseEntity<ProgrammeSummaryResponse> rename(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid,
        @Valid @RequestBody UpdateProgrammeRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var p = programmeService.renameProgramme(tenantId, programmeUid, request.getName(), tenantId, "TENANT");
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

    @PatchMapping("/api/v2/programmes/{programmeUid}/config/event-schema/events/{eventType}")
    @Operation(summary = "Patch a single event definition in programme eventSchema")
    public ResponseEntity<ProgrammeConfigBlobResponse> patchEventDefinition(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid,
        @PathVariable("eventType") String eventType,
        @Valid @RequestBody EventDefinitionRequest body
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var saved = programmeService.patchEventDefinition(tenantId, programmeUid, eventType, body, tenantId, "TENANT");
        return ResponseEntity.ok(readConfigBlob(tenantId, programmeUid, saved));
    }

    @PostMapping("/api/v2/programmes/{programmeUid}/config/event-schema/events")
    @Operation(summary = "Add a new event definition to programme eventSchema")
    public ResponseEntity<ProgrammeConfigBlobResponse> addEventDefinition(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid,
        @Valid @RequestBody EventDefinitionRequest body
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var saved = programmeService.addEventDefinition(tenantId, programmeUid, body, tenantId, "TENANT");
        return ResponseEntity.ok(readConfigBlob(tenantId, programmeUid, saved));
    }

    @DeleteMapping("/api/v2/programmes/{programmeUid}/config/event-schema/events/{eventType}")
    @Operation(summary = "Remove an event definition from programme eventSchema")
    public ResponseEntity<ProgrammeConfigBlobResponse> removeEventDefinition(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid,
        @PathVariable("eventType") String eventType
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var saved = programmeService.removeEventDefinition(tenantId, programmeUid, eventType, tenantId, "TENANT");
        return ResponseEntity.ok(readConfigBlob(tenantId, programmeUid, saved));
    }

    @PatchMapping("/api/v2/programmes/{programmeUid}/config/event-schema/settings")
    @Operation(summary = "Patch programme-wide event schema settings (version, backward compatibility, custom fields)")
    public ResponseEntity<ProgrammeConfigBlobResponse> patchEventSchemaSettings(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid,
        @Valid @RequestBody EventSchemaSettingsPatchRequest body
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var saved = programmeService.patchEventSchemaSettings(tenantId, programmeUid, body, tenantId, "TENANT");
        return ResponseEntity.ok(readConfigBlob(tenantId, programmeUid, saved));
    }

    @GetMapping("/api/v2/programmes/{programmeUid}/reward-catalog")
    @Operation(summary = "Merged reward catalog from MySQL (all programme_config versions + voucher inventory)")
    public ResponseEntity<PortalRewardCatalogResponse> getMergedRewardCatalog(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var merged = rewardCatalogDbMergeService.mergeFromDatabase(tenantId, programmeUid);
        return ResponseEntity.ok(PortalRewardCatalogResponse.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .activeConfigVersion(merged.activeConfigVersion())
            .rewardCatalog(merged.rewardCatalogJson())
            .mergedConfigVersions(merged.mergedConfigVersions())
            .synthesizedRewardUids(merged.synthesizedRewardUids())
            .itemCount(merged.catalog().items().size())
            .voucherBatchCount(merged.voucherBatchCount())
            .build());
    }

    @GetMapping("/api/v2/programmes/{programmeUid}/config/reward-catalog/recovery")
    @Operation(summary = "Preview recoverable reward catalog from configuration history")
    public ResponseEntity<RewardCatalogRecoveryResponse> previewRewardCatalogRecovery(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(programmeService.previewRewardCatalogRecovery(tenantId, programmeUid));
    }

    @PostMapping("/api/v2/programmes/{programmeUid}/config/reward-catalog/restore")
    @Operation(summary = "Restore reward catalog from the most recent config version that had catalog items")
    public ResponseEntity<ProgrammeConfigBlobResponse> restoreRewardCatalog(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        var saved = programmeService.restoreRewardCatalogFromHistory(tenantId, programmeUid, tenantId, "TENANT");
        return ResponseEntity.ok(readConfigBlob(tenantId, programmeUid, saved));
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

    @DeleteMapping("/api/v2/programmes/{programmeUid}")
    @Operation(summary = "Remove programme from configuration list (soft archive)",
        description = "Marks the programme ARCHIVED and cascades: archives linked rules, ends linked campaigns. "
            + "Portal lists hide archived programmes and their related rules/campaigns. "
            + "Ledger and config history are retained. Blocked for default programme or last remaining programme.")
    public ResponseEntity<Void> archive(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("programmeUid") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        programmeService.archiveProgramme(tenantId, programmeUid, tenantId, "TENANT");
        return ResponseEntity.noContent().build();
    }

    private ProgrammeConfigBlobResponse readConfigBlob(
        String tenantId,
        String programmeUid,
        com.loyaltyos.onboarding.entity.ProgrammeConfig saved
    ) {
        JsonNode node;
        try {
            node = objectMapper.readTree(saved.getConfigJson());
        } catch (Exception e) {
            node = objectMapper.createObjectNode();
        }
        return ProgrammeConfigBlobResponse.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .configVersion(saved.getConfigVersion())
            .config(node)
            .build();
    }
}


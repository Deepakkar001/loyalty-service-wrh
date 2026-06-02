package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loyaltyos.onboarding.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.entity.Programme;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
// import com.loyaltyos.onboarding.event.ProgrammeConfigUpdatedEvent; // with Kafka publish
import com.loyaltyos.onboarding.enums.OnboardingStatus;
import com.loyaltyos.onboarding.exception.ProgrammeArchiveBlockedException;
import com.loyaltyos.onboarding.exception.ProgrammeInactiveException;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.entity.Programme.ProgrammeStatus;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.ProgrammeConfigRepository;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.rules.service.RuleCacheService;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.rules.entity.EarnRule;
import com.loyaltyos.rules.enums.RuleStatus;
import com.loyaltyos.rules.enums.RuleType;
import com.loyaltyos.rules.repository.EarnRuleRepository;
// import org.springframework.kafka.core.KafkaTemplate; // re-enable with Kafka
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProgrammeService {

    private static final String DEFAULT_PROGRAMME_UID = "default";

    private final ProgrammeRepository programmeRepository;
    private final ProgrammeConfigRepository programmeConfigRepository;
    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final ProgrammeConfigSchemaValidator schemaValidator;
    // private final KafkaTemplate<String, Object> kafkaTemplate; // re-enable with Kafka
    private final OnboardingAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final RuleCacheService ruleCacheService;
    private final OnboardingStateMachine stateMachine;
    private final EarnRuleRepository earnRuleRepository;
    private final CampaignRepository campaignRepository;

    public ProgrammeService(
        ProgrammeRepository programmeRepository,
        ProgrammeConfigRepository programmeConfigRepository,
        TenantOnboardingRepository tenantOnboardingRepository,
        ProgrammeConfigSchemaValidator schemaValidator,
        OnboardingAuditLogRepository auditLogRepository,
        ObjectMapper objectMapper,
        RuleCacheService ruleCacheService,
        OnboardingStateMachine stateMachine,
        EarnRuleRepository earnRuleRepository,
        CampaignRepository campaignRepository
    ) {
        this.programmeRepository = Objects.requireNonNull(programmeRepository, "programmeRepository");
        this.programmeConfigRepository = Objects.requireNonNull(programmeConfigRepository, "programmeConfigRepository");
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository, "tenantOnboardingRepository");
        this.schemaValidator = Objects.requireNonNull(schemaValidator, "schemaValidator");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.ruleCacheService = Objects.requireNonNull(ruleCacheService, "ruleCacheService");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
    }

    @Transactional(readOnly = true)
    public List<Programme> listProgrammes(String tenantId) {
        return programmeRepository.findByTenantIdAndStatusNotOrderByCreatedAtAsc(
            tenantId, Programme.ProgrammeStatus.ARCHIVED
        );
    }

    @Transactional(readOnly = true)
    public boolean isProgrammeArchived(String tenantId, String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return false;
        }
        return programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid.trim())
            .map(p -> p.getStatus() == Programme.ProgrammeStatus.ARCHIVED)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public Set<String> archivedProgrammeUids(String tenantId) {
        return programmeRepository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
            .filter(p -> p.getStatus() == Programme.ProgrammeStatus.ARCHIVED)
            .map(Programme::getProgrammeUid)
            .collect(Collectors.toSet());
    }

    @Transactional
    public Programme createProgramme(String tenantId, String name) {
        // Tenant existence guard: onboarding table is the source of truth in Step 4+
        tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        Programme p = Programme.builder()
            .tenantId(tenantId)
            .programmeUid(UUID.randomUUID().toString())
            .name(name.trim())
            .status(Programme.ProgrammeStatus.DRAFT)
            .activeConfigVersion(0)
            .build();
        Programme saved = programmeRepository.save(Objects.requireNonNull(p, "programme"));

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("PROGRAMME_CREATED")
            .actorId(tenantId)
            .actorRole("TENANT")
            .afterState(Map.of("programmeUid", saved.getProgrammeUid(), "name", saved.getName()))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        return saved;
    }

    /**
     * Renames a programme for portal lists and keeps {@code programmeIdentity.programmeName} in sync
     * when an active configuration exists (new versioned config row).
     */
    @Transactional
    public Programme renameProgramme(String tenantId, String programmeUid, String name, String actorId, String actorRole) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("Programme name must be at least 2 characters");
        }

        Programme programme = programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid)
            .orElseThrow(() -> new IllegalArgumentException("Programme not found"));

        assertProgrammeEditable(programme);

        programme.setName(trimmed);
        programme.setUpdatedAt(Instant.now());

        ProgrammeConfig active = getActiveConfigOrNull(tenantId, programmeUid);
        if (active != null && active.getConfigJson() != null && !active.getConfigJson().isBlank()) {
            try {
                JsonNode config = objectMapper.readTree(active.getConfigJson());
                if (config.isObject()) {
                    ObjectNode root = (ObjectNode) config;
                    ObjectNode identity;
                    JsonNode identityNode = root.get("programmeIdentity");
                    if (identityNode != null && identityNode.isObject()) {
                        identity = (ObjectNode) identityNode;
                    } else {
                        identity = objectMapper.createObjectNode();
                        root.set("programmeIdentity", identity);
                    }
                    identity.put("programmeName", trimmed);
                    saveConfig(tenantId, programmeUid, root, actorId, actorRole);
                    return programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid)
                        .orElse(programme);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to update programme name in configuration", e);
            }
        }

        Programme saved = programmeRepository.save(programme);

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("PROGRAMME_RENAMED")
            .actorId(actorId)
            .actorRole(actorRole)
            .afterState(Map.of("programmeUid", programmeUid, "name", trimmed))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        return saved;
    }

    @Transactional(readOnly = true)
    public ProgrammeConfig getActiveConfigOrNull(String tenantId, String programmeUid) {
        return programmeConfigRepository.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc(tenantId, programmeUid)
            .orElse(null);
    }

    /**
     * Persists a new row in {@code programme_config} (versioned JSON in {@code config_json}) for the tenant's programme.
     * <p><b>Event schema {@code required} flags:</b> Each core/custom field object in {@code eventSchema} may include
     * {@code "required": true|false}. That value is stored verbatim in {@code config_json}. At ingestion time,
     * {@link EventSchemaPayloadValidator} reads those flags: when {@code required} is true, the incoming event map must
     * contain a non-null value for that field name or validation fails. Toggling Required in the tenant UI and saving
     * configuration therefore updates the database on the next successful save (new {@code config_version} row).</p>
     * <p>The next {@code config_version} is {@code max(latest row in programme_config, programmes.active_config_version) + 1}
     * so inserts never collide with {@code uk_programme_version} when the programme row was not updated in sync.</p>
     */
    @Transactional
    public ProgrammeConfig saveConfig(String tenantId, String programmeUid, JsonNode config, String actorId, String actorRole) {
        Programme p = programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid)
            .orElseThrow(() -> new IllegalArgumentException("Programme not found"));

        assertProgrammeEditable(p);

        schemaValidator.validate(config);

        // programme_config is the version source of truth; programmes.active_config_version can lag (e.g. legacy
        // onboarding paths that wrote programme_config without updating the programme row). Always take the max
        // so we never insert a duplicate (tenant_id, programme_uid, config_version).
        int latestPersisted = programmeConfigRepository
            .findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc(tenantId, programmeUid)
            .map(c -> c.getConfigVersion() == null ? 0 : c.getConfigVersion())
            .orElse(0);
        int activeOnProgrammeRow = p.getActiveConfigVersion() == null ? 0 : p.getActiveConfigVersion();
        int nextVersion = Math.max(latestPersisted, activeOnProgrammeRow) + 1;
        String json;
        try {
            json = objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize programme config", e);
        }

        ProgrammeConfig row = ProgrammeConfig.builder()
            .tenantId(tenantId)
            .programmeUid(programmeUid)
            .configVersion(nextVersion)
            .configJson(json)
            .effectiveFrom(Instant.now())
            .createdByActorId(actorId)
            .createdByRole(actorRole)
            .build();
        ProgrammeConfig saved = programmeConfigRepository.save(Objects.requireNonNull(row, "programmeConfig"));

        p.setActiveConfigVersion(nextVersion);
        // Keep programmes.name aligned with programmeIdentity.programmeName so list APIs and admin UIs
        // show the same label tenants edit in configuration (avoids stale "Programme 2" style rows).
        JsonNode identity = config.path("programmeIdentity");
        if (!identity.isMissingNode() && identity.hasNonNull("programmeName")) {
            String displayName = identity.get("programmeName").asText("").trim();
            if (!displayName.isEmpty()) {
                p.setName(displayName);
            }
        }
        p.setUpdatedAt(Instant.now());
        programmeRepository.save(p);

        ruleCacheService.invalidateProgramme(tenantId, programmeUid);

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("PROGRAMME_CONFIG_SAVED")
            .actorId(actorId)
            .actorRole(actorRole)
            .afterState(Map.of("programmeUid", programmeUid, "configVersion", nextVersion))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        // Guided setup UX: v2 config saves must not strand tenants in AGREEMENT_SIGNED.
        // If a tenant saves any programme configuration successfully, unlock the next step (Rules Setup).
        tenantOnboardingRepository.findByTenantId(tenantId).ifPresent(t -> {
            if (t.getOnboardingStatus() == OnboardingStatus.AGREEMENT_SIGNED) {
                stateMachine.transition(t, OnboardingStatus.CONFIGURED, actorId, actorRole);
                tenantOnboardingRepository.save(t);
            }
        });

        // --- Kafka publish (disabled) — topic platform.config.updates ---
        // ProgrammeConfigUpdatedEvent event = ProgrammeConfigUpdatedEvent.builder()
        //     .tenantId(tenantId)
        //     .programmeId(programmeUid)
        //     .configVersion(nextVersion)
        //     .changedSections(List.of("all"))
        //     .changedAt(Instant.now())
        //     .changedByActorId(actorId)
        //     .changedByActorRole(actorRole)
        //     .build();
        // kafkaTemplate.send("platform.config.updates", tenantId, event);

        return saved;
    }

    /**
     * Enables or disables a programme for integration and live processing.
     * {@link ProgrammeStatus#ACTIVE} accepts integration traffic; {@link ProgrammeStatus#DRAFT} does not.
     * {@link ProgrammeStatus#ARCHIVED} cannot be changed here (use {@link #archiveProgramme}).
     */
    @Transactional
    public Programme updateProgrammeStatus(
        String tenantId,
        String programmeUid,
        ProgrammeStatus nextStatus,
        String actorId,
        String actorRole
    ) {
        if (nextStatus == null || nextStatus == ProgrammeStatus.ARCHIVED) {
            throw new IllegalArgumentException("Status must be ACTIVE or DRAFT");
        }

        Programme programme = programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid)
            .orElseThrow(() -> new IllegalArgumentException("Programme not found"));

        assertProgrammeEditable(programme);

        ProgrammeStatus before = programme.getStatus();
        if (before == nextStatus) {
            return programme;
        }

        if (nextStatus == ProgrammeStatus.ACTIVE) {
            boolean hasConfig = (programme.getActiveConfigVersion() != null && programme.getActiveConfigVersion() > 0)
                || getActiveConfigOrNull(tenantId, programmeUid) != null;
            if (!hasConfig) {
                throw new IllegalArgumentException(
                    "Save programme configuration before activating this programme for integration."
                );
            }
        }

        programme.setStatus(nextStatus);
        programme.setUpdatedAt(Instant.now());
        Programme saved = programmeRepository.save(programme);
        ruleCacheService.invalidateProgramme(tenantId, programmeUid);

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("PROGRAMME_STATUS_CHANGED")
            .actorId(actorId)
            .actorRole(actorRole)
            .beforeState(Map.of("programmeUid", programmeUid, "status", String.valueOf(before)))
            .afterState(Map.of("programmeUid", programmeUid, "status", String.valueOf(saved.getStatus())))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        return saved;
    }

    /**
     * Ensures the programme exists and is {@link ProgrammeStatus#ACTIVE} before integration APIs process requests.
     */
    @Transactional(readOnly = true)
    public void assertProgrammeActiveForIntegration(String tenantId, String programmeUid) {
        String uid = programmeUid == null || programmeUid.isBlank() ? DEFAULT_PROGRAMME_UID : programmeUid.trim();
        Programme programme = programmeRepository.findByTenantIdAndProgrammeUid(tenantId, uid)
            .orElseThrow(() -> new ProgrammeInactiveException(
                uid,
                null,
                "Unknown programme: " + uid
            ));

        if (programme.getStatus() == ProgrammeStatus.ARCHIVED) {
            throw new ProgrammeInactiveException(
                uid,
                programme.getStatus(),
                "Programme is archived and cannot be used for integration."
            );
        }
        if (programme.getStatus() != ProgrammeStatus.ACTIVE) {
            throw new ProgrammeInactiveException(
                uid,
                programme.getStatus(),
                "Programme is inactive. Activate it in My Configurations before sending integration traffic."
            );
        }
    }

    /**
     * Soft-deletes a programme by marking it {@link Programme.ProgrammeStatus#ARCHIVED}.
     * Historical config, ledger, and audit rows are retained; the programme is hidden from portal lists.
     */
    @Transactional
    public void archiveProgramme(String tenantId, String programmeUid, String actorId, String actorRole) {
        tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        Programme programme = programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid)
            .orElseThrow(() -> new IllegalArgumentException("Programme not found"));

        if (programme.getStatus() == Programme.ProgrammeStatus.ARCHIVED) {
            throw ProgrammeArchiveBlockedException.single(
                "programmeUid",
                "This programme is already removed from your configuration list."
            );
        }

        Map<String, String> reasons = new LinkedHashMap<>();

        if (DEFAULT_PROGRAMME_UID.equals(programmeUid)) {
            reasons.put(
                "programmeUid",
                "The default programme cannot be removed. It is required for integration defaults."
            );
        }

        if (programmeRepository.countByTenantIdAndStatusNot(tenantId, Programme.ProgrammeStatus.ARCHIVED) <= 1) {
            reasons.put(
                "programmeUid",
                "At least one programme must remain in your tenant account."
            );
        }

        if (!reasons.isEmpty()) {
            throw new ProgrammeArchiveBlockedException(
                "This programme cannot be removed yet. Resolve the listed items and try again.",
                reasons
            );
        }

        CascadeArchiveResult cascade = cascadeArchiveDependencies(tenantId, programmeUid);

        programme.setStatus(Programme.ProgrammeStatus.ARCHIVED);
        programme.setUpdatedAt(Instant.now());
        programmeRepository.save(programme);
        ruleCacheService.invalidateProgramme(tenantId, programmeUid);

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("PROGRAMME_ARCHIVED")
            .actorId(actorId)
            .actorRole(actorRole)
            .afterState(Map.of(
                "programmeUid", programmeUid,
                "name", programme.getName(),
                "rulesArchived", cascade.rulesArchived(),
                "campaignsEnded", cascade.campaignsEnded()
            ))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));
    }

    private CascadeArchiveResult cascadeArchiveDependencies(String tenantId, String programmeUid) {
        Instant now = Instant.now();
        int campaignsEnded = 0;
        int rulesArchived = 0;

        for (Campaign campaign : campaignRepository.findByTenantIdAndProgrammeUidOrderByPriorityDescCreatedAtDesc(
            tenantId, programmeUid
        )) {
            if (!isTerminalCampaignStatus(campaign.getStatus())) {
                campaign.setStatus(CampaignStatus.ENDED);
                campaignRepository.save(campaign);
                campaignsEnded++;
            }
        }

        List<String> campaignUids = campaignRepository
            .findByTenantIdAndProgrammeUidOrderByPriorityDescCreatedAtDesc(tenantId, programmeUid)
            .stream()
            .map(Campaign::getCampaignUid)
            .filter(uid -> uid != null && !uid.isBlank())
            .distinct()
            .toList();

        for (EarnRule rule : earnRuleRepository.findByTenantIdAndProgrammeUidAndStatusNot(
            tenantId, programmeUid, RuleStatus.ARCHIVED
        )) {
            rule.setStatus(RuleStatus.ARCHIVED);
            rule.setArchivedAt(now);
            earnRuleRepository.save(rule);
            rulesArchived++;
        }

        if (!campaignUids.isEmpty()) {
            for (EarnRule rule : earnRuleRepository.findByTenantIdAndRuleTypeAndCampaignUidInOrderByPriorityDesc(
                tenantId, RuleType.CAMPAIGN, campaignUids
            )) {
                if (rule.getStatus() != RuleStatus.ARCHIVED) {
                    rule.setStatus(RuleStatus.ARCHIVED);
                    rule.setArchivedAt(now);
                    earnRuleRepository.save(rule);
                    rulesArchived++;
                }
            }
        }

        return new CascadeArchiveResult(rulesArchived, campaignsEnded);
    }

    private static boolean isTerminalCampaignStatus(CampaignStatus status) {
        return status == CampaignStatus.ENDED
            || status == CampaignStatus.EXHAUSTED
            || status == CampaignStatus.EXPIRED;
    }

    private record CascadeArchiveResult(int rulesArchived, int campaignsEnded) {}

    private void assertProgrammeEditable(Programme programme) {
        if (programme.getStatus() == Programme.ProgrammeStatus.ARCHIVED) {
            throw new IllegalStateException(
                "Programme is archived and cannot be modified. Create a new programme instead."
            );
        }
    }
}


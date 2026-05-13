package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.Programme;
import com.loyaltyos.onboarding.domain.entity.ProgrammeConfig;
// import com.loyaltyos.onboarding.event.ProgrammeConfigUpdatedEvent; // with Kafka publish
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.ProgrammeConfigRepository;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.rules.service.RuleCacheService;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
// import org.springframework.kafka.core.KafkaTemplate; // re-enable with Kafka
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;
    private final ProgrammeConfigRepository programmeConfigRepository;
    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final ProgrammeConfigSchemaValidator schemaValidator;
    // private final KafkaTemplate<String, Object> kafkaTemplate; // re-enable with Kafka
    private final OnboardingAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final RuleCacheService ruleCacheService;
    private final OnboardingStateMachine stateMachine;

    public ProgrammeService(
        ProgrammeRepository programmeRepository,
        ProgrammeConfigRepository programmeConfigRepository,
        TenantOnboardingRepository tenantOnboardingRepository,
        ProgrammeConfigSchemaValidator schemaValidator,
        OnboardingAuditLogRepository auditLogRepository,
        ObjectMapper objectMapper,
        RuleCacheService ruleCacheService,
        OnboardingStateMachine stateMachine
    ) {
        this.programmeRepository = Objects.requireNonNull(programmeRepository, "programmeRepository");
        this.programmeConfigRepository = Objects.requireNonNull(programmeConfigRepository, "programmeConfigRepository");
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository, "tenantOnboardingRepository");
        this.schemaValidator = Objects.requireNonNull(schemaValidator, "schemaValidator");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.ruleCacheService = Objects.requireNonNull(ruleCacheService, "ruleCacheService");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
    }

    @Transactional(readOnly = true)
    public List<Programme> listProgrammes(String tenantId) {
        return programmeRepository.findByTenantIdOrderByCreatedAtAsc(tenantId);
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

    @Transactional(readOnly = true)
    public ProgrammeConfig getActiveConfigOrNull(String tenantId, String programmeUid) {
        return programmeConfigRepository.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc(tenantId, programmeUid)
            .orElse(null);
    }

    @Transactional
    public ProgrammeConfig saveConfig(String tenantId, String programmeUid, JsonNode config, String actorId, String actorRole) {
        Programme p = programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid)
            .orElseThrow(() -> new IllegalArgumentException("Programme not found"));

        schemaValidator.validate(config);

        int nextVersion = (p.getActiveConfigVersion() == null ? 0 : p.getActiveConfigVersion()) + 1;
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
}


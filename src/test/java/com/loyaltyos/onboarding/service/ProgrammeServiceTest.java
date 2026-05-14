package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.domain.entity.Programme;
import com.loyaltyos.onboarding.domain.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.ProgrammeConfigRepository;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.rules.service.RuleCacheService;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgrammeServiceTest {

    @Test
    void saveConfig_validSchema_incrementsVersion() throws Exception {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var tenantOnboardingRepo = mock(TenantOnboardingRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var ruleCacheService = mock(RuleCacheService.class);
        var objectMapper = new ObjectMapper();
        var schemaValidator = new ProgrammeConfigSchemaValidator(objectMapper);
        var stateMachine = mock(OnboardingStateMachine.class);
        // KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class); // when Kafka re-enabled

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .name("My Programme")
            .activeConfigVersion(0)
            .status(Programme.ProgrammeStatus.DRAFT)
            .build();

        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "p1")).thenReturn(Optional.of(p));
        when(programmeConfigRepo.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc("t1", "p1"))
            .thenReturn(Optional.empty());
        when(programmeConfigRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(programmeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgrammeService svc = new ProgrammeService(
            programmeRepo,
            programmeConfigRepo,
            tenantOnboardingRepo,
            schemaValidator,
            auditRepo,
            objectMapper,
            ruleCacheService,
            stateMachine
        );

        var config = objectMapper.readTree("""
          {
            "programmeIdentity": {"programmeName":"Prog","pointsName":"Pts","pointsSymbol":"pts","baseCurrency":"INR"},
            "pointsEconomics": {"pointsMonetaryValue":0.01,"basePointsRate":1},
            "conflictPolicy": {"defaultStrategy":"BEST_FOR_CUSTOMER","allowRuleOverride":true},
            "tiers": {"enabled": false, "tiers":[{"tierUid":"standard","name":"Standard","rank":1,"entryThreshold":0,"maintenanceThreshold":0,"multiplier":1}]},
            "expiry": {"model":"ROLLING","rollingMonths":24,"tierExtensionsEnabled":true,"notificationScheduleDays":[60,7,1],"processMode":"OVERNIGHT_BATCH",
              "breakage":{"enabled":true,"reportFrequency":"MONTHLY","accountingCutoffTimezone":"Asia/Kolkata","exportEnabled":true}
            },
            "eventSchema": {"version":1,"standardFields":[{"name":"eventType","type":"string","required":true}],"customFields":[],"backwardCompatibilityDays":30}
          }
        """);

        ProgrammeConfig saved = svc.saveConfig("t1", "p1", config, "t1", "TENANT");

        assertEquals(1, saved.getConfigVersion());
        // verify(kafkaTemplate).send(eq("platform.config.updates"), eq("t1"), any()); // when Kafka re-enabled
    }

    @Test
    void saveConfig_whenProgrammeVersionLagsBehindProgrammeConfig_usesMaxPlusOne() throws Exception {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var tenantOnboardingRepo = mock(TenantOnboardingRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var ruleCacheService = mock(RuleCacheService.class);
        var objectMapper = new ObjectMapper();
        var schemaValidator = new ProgrammeConfigSchemaValidator(objectMapper);
        var stateMachine = mock(OnboardingStateMachine.class);

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .name("My Programme")
            .activeConfigVersion(0)
            .status(Programme.ProgrammeStatus.DRAFT)
            .build();

        ProgrammeConfig latestRow = ProgrammeConfig.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .configVersion(5)
            .configJson("{}")
            .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
            .createdByActorId("t1")
            .createdByRole("TENANT")
            .build();

        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "p1")).thenReturn(Optional.of(p));
        when(programmeConfigRepo.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc("t1", "p1"))
            .thenReturn(Optional.of(latestRow));
        when(programmeConfigRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(programmeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgrammeService svc = new ProgrammeService(
            programmeRepo,
            programmeConfigRepo,
            tenantOnboardingRepo,
            schemaValidator,
            auditRepo,
            objectMapper,
            ruleCacheService,
            stateMachine
        );

        var config = objectMapper.readTree("""
          {
            "programmeIdentity": {"programmeName":"Prog","pointsName":"Pts","pointsSymbol":"pts","baseCurrency":"INR"},
            "pointsEconomics": {"pointsMonetaryValue":0.01,"basePointsRate":1},
            "conflictPolicy": {"defaultStrategy":"BEST_FOR_CUSTOMER","allowRuleOverride":true},
            "tiers": {"enabled": false, "tiers":[{"tierUid":"standard","name":"Standard","rank":1,"entryThreshold":0,"maintenanceThreshold":0,"multiplier":1}]},
            "expiry": {"model":"ROLLING","rollingMonths":24,"tierExtensionsEnabled":true,"notificationScheduleDays":[60,7,1],"processMode":"OVERNIGHT_BATCH",
              "breakage":{"enabled":true,"reportFrequency":"MONTHLY","accountingCutoffTimezone":"Asia/Kolkata","exportEnabled":true}
            },
            "eventSchema": {"version":1,"standardFields":[{"name":"eventType","type":"string","required":true}],"customFields":[],"backwardCompatibilityDays":30}
          }
        """);

        ProgrammeConfig saved = svc.saveConfig("t1", "p1", config, "t1", "TENANT");

        assertEquals(6, saved.getConfigVersion());
    }
}


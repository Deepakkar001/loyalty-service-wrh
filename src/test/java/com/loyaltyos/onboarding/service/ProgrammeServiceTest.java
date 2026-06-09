package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.entity.Programme;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.enums.OnboardingStatus;
import com.loyaltyos.onboarding.exception.ProgrammeArchiveBlockedException;
import com.loyaltyos.onboarding.exception.ProgrammeInactiveException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.ProgrammeConfigRepository;
import com.loyaltyos.onboarding.repository.ProgrammeRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.rules.service.RuleCacheService;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.rules.enums.RuleStatus;
import com.loyaltyos.rules.repository.EarnRuleRepository;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgrammeServiceTest {

    private ProgrammeService newService(
        ProgrammeRepository programmeRepo,
        ProgrammeConfigRepository programmeConfigRepo
    ) {
        return new ProgrammeService(
            programmeRepo,
            programmeConfigRepo,
            mock(TenantOnboardingRepository.class),
            new ProgrammeConfigSchemaValidator(new ObjectMapper()),
            mock(OnboardingAuditLogRepository.class),
            new ObjectMapper(),
            mock(RuleCacheService.class),
            mock(OnboardingStateMachine.class),
            mock(EarnRuleRepository.class),
            mock(CampaignRepository.class)
        );
    }

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

        ProgrammeService svc = newService(programmeRepo, programmeConfigRepo);

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

        ProgrammeService svc = newService(programmeRepo, programmeConfigRepo);

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

    @Test
    void archiveProgramme_marksArchivedWhenNoDependencies() {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var tenantOnboardingRepo = mock(TenantOnboardingRepository.class);
        var earnRuleRepo = mock(EarnRuleRepository.class);
        var campaignRepo = mock(CampaignRepository.class);
        var ruleCache = mock(RuleCacheService.class);

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("extra-1")
            .name("Extra")
            .status(Programme.ProgrammeStatus.DRAFT)
            .build();

        when(tenantOnboardingRepo.findByTenantId("t1")).thenReturn(Optional.of(mock(TenantOnboarding.class)));
        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "extra-1")).thenReturn(Optional.of(p));
        when(programmeRepo.countByTenantIdAndStatusNot("t1", Programme.ProgrammeStatus.ARCHIVED)).thenReturn(2L);
        when(campaignRepo.findByTenantIdAndProgrammeUidOrderByPriorityDescCreatedAtDesc("t1", "extra-1"))
            .thenReturn(List.of());
        when(earnRuleRepo.findByTenantIdAndProgrammeUidAndStatusNot("t1", "extra-1", RuleStatus.ARCHIVED))
            .thenReturn(List.of());
        when(programmeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgrammeService svc = new ProgrammeService(
            programmeRepo,
            programmeConfigRepo,
            tenantOnboardingRepo,
            new ProgrammeConfigSchemaValidator(new ObjectMapper()),
            mock(OnboardingAuditLogRepository.class),
            new ObjectMapper(),
            ruleCache,
            mock(OnboardingStateMachine.class),
            earnRuleRepo,
            campaignRepo
        );

        svc.archiveProgramme("t1", "extra-1", "t1", "TENANT");

        assertEquals(Programme.ProgrammeStatus.ARCHIVED, p.getStatus());
        verify(ruleCache).invalidateProgramme("t1", "extra-1");
    }

    @Test
    void renameProgramme_updatesProgrammeRowWhenNoConfig() {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .name("Old Name")
            .status(Programme.ProgrammeStatus.DRAFT)
            .activeConfigVersion(0)
            .build();

        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "p1")).thenReturn(Optional.of(p));
        when(programmeConfigRepo.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc("t1", "p1"))
            .thenReturn(Optional.empty());
        when(programmeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgrammeService svc = newService(programmeRepo, programmeConfigRepo);
        Programme renamed = svc.renameProgramme("t1", "p1", "New Name", "t1", "TENANT");

        assertEquals("New Name", renamed.getName());
        verify(programmeRepo).save(any());
    }

    @Test
    void archiveProgramme_blocksDefaultProgramme() {
        var programmeRepo = mock(ProgrammeRepository.class);
        var tenantOnboardingRepo = mock(TenantOnboardingRepository.class);
        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("default")
            .name("Default")
            .status(Programme.ProgrammeStatus.ACTIVE)
            .build();

        when(tenantOnboardingRepo.findByTenantId("t1")).thenReturn(Optional.of(mock(TenantOnboarding.class)));
        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "default")).thenReturn(Optional.of(p));
        when(programmeRepo.countByTenantIdAndStatusNot("t1", Programme.ProgrammeStatus.ARCHIVED)).thenReturn(2L);

        ProgrammeService svc = new ProgrammeService(
            programmeRepo,
            mock(ProgrammeConfigRepository.class),
            tenantOnboardingRepo,
            new ProgrammeConfigSchemaValidator(new ObjectMapper()),
            mock(OnboardingAuditLogRepository.class),
            new ObjectMapper(),
            mock(RuleCacheService.class),
            mock(OnboardingStateMachine.class),
            mock(EarnRuleRepository.class),
            mock(CampaignRepository.class)
        );

        assertThrows(ProgrammeArchiveBlockedException.class,
            () -> svc.archiveProgramme("t1", "default", "t1", "TENANT"));
        verify(programmeRepo, never()).save(any());
    }

    @Test
    void updateProgrammeStatus_activate_requiresConfig() {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .name("Prog")
            .status(Programme.ProgrammeStatus.DRAFT)
            .activeConfigVersion(0)
            .build();

        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "p1")).thenReturn(Optional.of(p));
        when(programmeConfigRepo.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc("t1", "p1"))
            .thenReturn(Optional.empty());

        ProgrammeService svc = newService(programmeRepo, programmeConfigRepo);

        assertThrows(IllegalArgumentException.class,
            () -> svc.updateProgrammeStatus("t1", "p1", Programme.ProgrammeStatus.ACTIVE, "t1", "TENANT"));
    }

    @Test
    void updateProgrammeStatus_activate_whenConfigExists() {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var ruleCache = mock(RuleCacheService.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .name("Prog")
            .status(Programme.ProgrammeStatus.DRAFT)
            .activeConfigVersion(1)
            .build();

        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "p1")).thenReturn(Optional.of(p));
        when(programmeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgrammeService svc = new ProgrammeService(
            programmeRepo,
            programmeConfigRepo,
            mock(TenantOnboardingRepository.class),
            new ProgrammeConfigSchemaValidator(new ObjectMapper()),
            auditRepo,
            new ObjectMapper(),
            ruleCache,
            mock(OnboardingStateMachine.class),
            mock(EarnRuleRepository.class),
            mock(CampaignRepository.class)
        );

        Programme updated = svc.updateProgrammeStatus("t1", "p1", Programme.ProgrammeStatus.ACTIVE, "t1", "TENANT");
        assertEquals(Programme.ProgrammeStatus.ACTIVE, updated.getStatus());
        verify(ruleCache).invalidateProgramme("t1", "p1");
    }

    @Test
    void saveConfig_duringGuidedSetup_autoActivatesDraftProgramme() throws Exception {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var tenantOnboardingRepo = mock(TenantOnboardingRepository.class);
        var auditRepo = mock(OnboardingAuditLogRepository.class);
        var ruleCache = mock(RuleCacheService.class);
        var objectMapper = new ObjectMapper();

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("default")
            .name("Default")
            .activeConfigVersion(0)
            .status(Programme.ProgrammeStatus.DRAFT)
            .build();

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("t1")
            .onboardingStatus(OnboardingStatus.AGREEMENT_SIGNED)
            .build();

        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "default")).thenReturn(Optional.of(p));
        when(programmeConfigRepo.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc("t1", "default"))
            .thenReturn(Optional.empty());
        when(programmeConfigRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(programmeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantOnboardingRepo.findByTenantId("t1")).thenReturn(Optional.of(tenant));

        ProgrammeService svc = new ProgrammeService(
            programmeRepo,
            programmeConfigRepo,
            tenantOnboardingRepo,
            new ProgrammeConfigSchemaValidator(objectMapper),
            auditRepo,
            objectMapper,
            ruleCache,
            mock(OnboardingStateMachine.class),
            mock(EarnRuleRepository.class),
            mock(CampaignRepository.class)
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

        svc.saveConfig("t1", "default", config, "t1", "TENANT");

        assertEquals(Programme.ProgrammeStatus.ACTIVE, p.getStatus());
        verify(ruleCache, org.mockito.Mockito.atLeastOnce()).invalidateProgramme("t1", "default");
    }

    @Test
    void saveConfig_afterGoLive_doesNotAutoActivateProgramme() throws Exception {
        var programmeRepo = mock(ProgrammeRepository.class);
        var programmeConfigRepo = mock(ProgrammeConfigRepository.class);
        var tenantOnboardingRepo = mock(TenantOnboardingRepository.class);
        var objectMapper = new ObjectMapper();

        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .name("Prog")
            .activeConfigVersion(0)
            .status(Programme.ProgrammeStatus.DRAFT)
            .build();

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("t1")
            .onboardingStatus(OnboardingStatus.ACTIVE)
            .build();

        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "p1")).thenReturn(Optional.of(p));
        when(programmeConfigRepo.findTopByTenantIdAndProgrammeUidOrderByConfigVersionDesc("t1", "p1"))
            .thenReturn(Optional.empty());
        when(programmeConfigRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(programmeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantOnboardingRepo.findByTenantId("t1")).thenReturn(Optional.of(tenant));

        ProgrammeService svc = new ProgrammeService(
            programmeRepo,
            programmeConfigRepo,
            tenantOnboardingRepo,
            new ProgrammeConfigSchemaValidator(objectMapper),
            mock(OnboardingAuditLogRepository.class),
            objectMapper,
            mock(RuleCacheService.class),
            mock(OnboardingStateMachine.class),
            mock(EarnRuleRepository.class),
            mock(CampaignRepository.class)
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

        svc.saveConfig("t1", "p1", config, "t1", "TENANT");

        assertEquals(Programme.ProgrammeStatus.DRAFT, p.getStatus());
    }

    @Test
    void assertProgrammeActiveForIntegration_rejectsDraft() {
        var programmeRepo = mock(ProgrammeRepository.class);
        Programme p = Programme.builder()
            .tenantId("t1")
            .programmeUid("p1")
            .name("Prog")
            .status(Programme.ProgrammeStatus.DRAFT)
            .build();
        when(programmeRepo.findByTenantIdAndProgrammeUid("t1", "p1")).thenReturn(Optional.of(p));

        ProgrammeService svc = newService(programmeRepo, mock(ProgrammeConfigRepository.class));

        assertThrows(ProgrammeInactiveException.class,
            () -> svc.assertProgrammeActiveForIntegration("t1", "p1"));
    }
}


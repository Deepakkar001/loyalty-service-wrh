package com.loyaltyos.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.integration.repository.ApiRequestAuditLogRepository;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.enums.SubscriptionTier;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.support.dto.CreateSupportCaseRequest;
import com.loyaltyos.support.dto.UpdateSupportCaseStatusRequest;
import com.loyaltyos.support.enums.SupportCaseStatus;
import com.loyaltyos.support.enums.SupportCaseCategory;
import com.loyaltyos.support.enums.SupportCasePriority;
import com.loyaltyos.support.exception.SupportException;
import com.loyaltyos.support.repository.SupportCaseRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.mockito.ArgumentCaptor;

class SupportCaseServiceTest {

    private SupportCaseRepository caseRepository;
    private TenantOnboardingRepository tenantRepository;
    private ApiRequestAuditLogRepository auditRepository;
    private SupportCaseService service;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        caseRepository = mock(SupportCaseRepository.class);
        tenantRepository = mock(TenantOnboardingRepository.class);
        auditRepository = mock(ApiRequestAuditLogRepository.class);
        service = new SupportCaseService(caseRepository, tenantRepository, auditRepository, new ObjectMapper());

        jwt = Jwt.withTokenValue("t")
            .header("alg", "none")
            .claim("tenantId", "tenant-1")
            .claim("email", "ops@acme.com")
            .subject("user-1")
            .build();

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId("tenant-1")
            .companyName("Acme")
            .subscriptionTier(SubscriptionTier.PROFESSIONAL)
            .build();
        when(tenantRepository.findByTenantId("tenant-1")).thenReturn(Optional.of(tenant));
        when(auditRepository.findTop10ByTenantIdOrderByCreatedAtDesc("tenant-1")).thenReturn(List.of());
        when(caseRepository.countByTenantIdAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createCase_persistsTenantScopedRow() {
        CreateSupportCaseRequest req = new CreateSupportCaseRequest();
        req.setCategory(SupportCaseCategory.INTEGRATION);
        req.setPriority(SupportCasePriority.NORMAL);
        req.setSubject("Webhook failures");
        req.setDescription("Events return 500 since yesterday.");
        req.setCorrelationId("req-abc");

        var response = service.createCase(jwt, req);

        assertThat(response.getCaseUid()).startsWith("case_");
        assertThat(response.getTenantId()).isEqualTo("tenant-1");
        assertThat(response.getSubject()).isEqualTo("Webhook failures");
        assertThat(response.getSlaResponseHint()).contains("business day");

        ArgumentCaptor<com.loyaltyos.support.entity.SupportCase> captor =
            ArgumentCaptor.forClass(com.loyaltyos.support.entity.SupportCase.class);
        org.mockito.Mockito.verify(caseRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-1");
        assertThat(captor.getValue().getCreatedByEmail()).isEqualTo("ops@acme.com");
    }

    @Test
    void updateStatusForTenant_appliesTransition() {
        com.loyaltyos.support.entity.SupportCase existing = new com.loyaltyos.support.entity.SupportCase();
        existing.setTenantId("tenant-1");
        existing.setCaseUid("case_abc");
        existing.setStatus(SupportCaseStatus.OPEN);
        existing.setCategory(SupportCaseCategory.INTEGRATION);
        existing.setPriority(SupportCasePriority.NORMAL);
        existing.setSubject("S");
        existing.setDescription("D");
        existing.setCreatedByUserId("u1");
        when(caseRepository.findByTenantIdAndCaseUid("tenant-1", "case_abc"))
            .thenReturn(java.util.Optional.of(existing));
        when(caseRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        UpdateSupportCaseStatusRequest req = new UpdateSupportCaseStatusRequest();
        req.setStatus(SupportCaseStatus.CLOSED);

        var response = service.updateStatusForTenant("tenant-1", "case_abc", req, jwt);
        assertThat(response.getStatus()).isEqualTo(SupportCaseStatus.CLOSED);
    }

    @Test
    void createCase_enforcesDailyRateLimit() {
        when(caseRepository.countByTenantIdAndCreatedAtAfter(any(), any())).thenReturn(25L);
        CreateSupportCaseRequest req = new CreateSupportCaseRequest();
        req.setCategory(SupportCaseCategory.OTHER);
        req.setPriority(SupportCasePriority.NORMAL);
        req.setSubject("Test");
        req.setDescription("Test body with enough content.");

        assertThatThrownBy(() -> service.createCase(jwt, req))
            .isInstanceOf(SupportException.class)
            .extracting(e -> ((SupportException) e).getCode())
            .isEqualTo("RATE_LIMIT");
    }
}

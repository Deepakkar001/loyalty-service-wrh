package com.loyaltyos.merchants.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loyaltyos.merchants.dto.SubmitMerchantAgreementRequest;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantAgreement;
import com.loyaltyos.merchants.enums.MerchantAgreementStatus;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.enums.SettlementCycle;
import com.loyaltyos.merchants.repository.MerchantAgreementRepository;
import com.loyaltyos.merchants.repository.MerchantOnboardingAuditRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.service.statemachine.MerchantOnboardingStateMachine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MerchantAgreementServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantAgreementRepository agreementRepository;

    @Mock
    private MerchantOnboardingAuditRepository auditRepository;

    @Mock
    private MerchantOnboardingStateMachine stateMachine;

    @Mock
    private MerchantValidationService validationService;

    @Mock
    private MerchantOnboardingEmailGuard onboardingEmailGuard;

    private MerchantAgreementService agreementService;

    @BeforeEach
    void setUp() {
        agreementService = new MerchantAgreementService(
            merchantRepository,
            agreementRepository,
            auditRepository,
            stateMachine,
            validationService,
            onboardingEmailGuard
        );
    }

    @Test
    void submitAgreement_rejectsWhenTermsNotAccepted() {
        Merchant merchant = registrationMerchant();
        when(merchantRepository.findByTenantIdAndMerchantUid("tenant-1", "m-1"))
            .thenReturn(Optional.of(merchant));

        SubmitMerchantAgreementRequest request = validRequest();
        request.setTermsAccepted(false);

        assertThrows(
            ResponseStatusException.class,
            () -> agreementService.submitAgreement("tenant-1", "m-1", request, "finance@tenant.com")
        );
    }

    @Test
    void submitAgreement_createsAgreementAndAdvancesStage() {
        Merchant merchant = registrationMerchant();
        when(merchantRepository.findByTenantIdAndMerchantUid("tenant-1", "m-1"))
            .thenReturn(Optional.of(merchant));
        when(agreementRepository.findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
            "tenant-1", "m-1", MerchantAgreementStatus.APPROVED))
            .thenReturn(Optional.empty());
        when(agreementRepository.save(any(MerchantAgreement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(merchantRepository.save(any(Merchant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = agreementService.submitAgreement(
            "tenant-1", "m-1", validRequest(), "finance@tenant.com");

        assertNotNull(response.getAgreementUid());
        assertEquals("v1.0-merchant", response.getTermsVersion());
        assertEquals(MerchantAgreementStatus.APPROVED, response.getStatus());

        ArgumentCaptor<MerchantAgreement> agreementCaptor = ArgumentCaptor.forClass(MerchantAgreement.class);
        verify(agreementRepository).save(agreementCaptor.capture());
        MerchantAgreement saved = agreementCaptor.getValue();
        assertEquals(SettlementCycle.MONTHLY, saved.getSettlementCycle());
        assertEquals(new BigDecimal("2.50"), saved.getRevenueSharePct());

        verify(stateMachine).transition(
            eq(merchant),
            eq(MerchantOnboardingStage.AGREEMENT),
            eq("finance@tenant.com"),
            eq("AGREEMENT_SUBMITTED"),
            eq(null),
            any()
        );

        assertNotNull(merchant.getAgreementAcceptedAt());
        assertNull(merchant.getAgreementDocumentUrl());
        assertEquals(SettlementCycle.MONTHLY, merchant.getSettlementCycle());
        assertEquals(new BigDecimal("2.5"), merchant.getEarnRateMultiplier());
    }

    @Test
    void submitAgreement_supersedesExistingApprovedRow() {
        Merchant merchant = agreementStageMerchant();
        MerchantAgreement existing = new MerchantAgreement();
        existing.setAgreementUid("MAG_old");
        existing.setStatus(MerchantAgreementStatus.APPROVED);

        when(merchantRepository.findByTenantIdAndMerchantUid("tenant-1", "m-1"))
            .thenReturn(Optional.of(merchant));
        when(agreementRepository.findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
            "tenant-1", "m-1", MerchantAgreementStatus.APPROVED))
            .thenReturn(Optional.of(existing));
        when(agreementRepository.save(any(MerchantAgreement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(merchantRepository.save(any(Merchant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        agreementService.submitAgreement("tenant-1", "m-1", validRequest(), "finance@tenant.com");

        assertEquals(MerchantAgreementStatus.SUPERSEDED, existing.getStatus());
        verify(agreementRepository).save(existing);
    }

    @Test
    void submitAgreement_atAgreementStage_updatesWithoutStageTransition() {
        Merchant merchant = agreementStageMerchant();
        when(merchantRepository.findByTenantIdAndMerchantUid("tenant-1", "m-1"))
            .thenReturn(Optional.of(merchant));
        when(agreementRepository.findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
            "tenant-1", "m-1", MerchantAgreementStatus.APPROVED))
            .thenReturn(Optional.empty());
        when(agreementRepository.save(any(MerchantAgreement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(merchantRepository.save(any(Merchant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        agreementService.submitAgreement("tenant-1", "m-1", validRequest(), "finance@tenant.com");

        verify(stateMachine, never()).transition(
            any(), any(), any(), any(), any(), any());
        assertEquals(MerchantOnboardingStage.AGREEMENT, merchant.getOnboardingStage());
    }

    @Test
    void submitAgreement_updatesPortalContactEmailWhenProvided() {
        Merchant merchant = registrationMerchant();
        merchant.setContactEmail("old@acme.com");
        when(merchantRepository.findByTenantIdAndMerchantUid("tenant-1", "m-1"))
            .thenReturn(Optional.of(merchant));
        when(agreementRepository.findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
            "tenant-1", "m-1", MerchantAgreementStatus.APPROVED))
            .thenReturn(Optional.empty());
        when(agreementRepository.save(any(MerchantAgreement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(merchantRepository.save(any(Merchant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SubmitMerchantAgreementRequest request = validRequest();
        request.setPortalContactEmail("partner@acme.com");

        agreementService.submitAgreement("tenant-1", "m-1", request, "finance@tenant.com");

        verify(onboardingEmailGuard).assertPortalEmailAvailable("tenant-1", "partner@acme.com", "m-1");
        assertEquals("partner@acme.com", merchant.getContactEmail());
    }

    private static Merchant registrationMerchant() {
        Merchant merchant = new Merchant();
        merchant.setTenantId("tenant-1");
        merchant.setMerchantUid("m-1");
        merchant.setLegalName("Acme Retail");
        merchant.setContactEmail("partner@acme.com");
        merchant.setOnboardingStage(MerchantOnboardingStage.REGISTRATION);
        merchant.setAgreementDocumentUrl("https://legacy.example/doc.pdf");
        return merchant;
    }

    private static Merchant agreementStageMerchant() {
        Merchant merchant = registrationMerchant();
        merchant.setOnboardingStage(MerchantOnboardingStage.AGREEMENT);
        return merchant;
    }

    private static SubmitMerchantAgreementRequest validRequest() {
        SubmitMerchantAgreementRequest request = new SubmitMerchantAgreementRequest();
        request.setTermsVersion("v1.0-merchant");
        request.setEffectiveDate(LocalDate.of(2026, 6, 1));
        request.setRevenueSharePct(new BigDecimal("2.50"));
        request.setSettlementCycle("MONTHLY");
        request.setPointsCurrency("INR");
        request.setContractDurationMonths(12);
        request.setAutoRenewal(true);
        request.setProposedEarnRateMultiplier(new BigDecimal("2.5"));
        request.setMerchantFundedCampaignsAllowed(true);
        request.setSignedByName("Jane Doe");
        request.setSignedByEmail("jane@acme.com");
        request.setTermsAccepted(true);
        return request;
    }
}

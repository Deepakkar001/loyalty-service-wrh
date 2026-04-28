package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.TenantAgreement;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.dto.request.SubmitAgreementRequest;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantAgreementService {

    private final TenantOnboardingRepository tenantRepository;
    private final TenantAgreementRepository agreementRepository;
    private final OnboardingStateMachine stateMachine;

    @Transactional
    public void submitAgreement(String tenantId, SubmitAgreementRequest request) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        OnboardingStatus status = tenant.getOnboardingStatus();

        if (status == OnboardingStatus.EMAIL_VERIFIED) {
            stateMachine.transition(tenant, OnboardingStatus.AGREEMENT_PENDING, tenantId, "TENANT");
        } else if (status == OnboardingStatus.AGREEMENT_SIGNED) {
            // Resubmission after rejection: move back to AGREEMENT_PENDING
            boolean latestWasRejected = agreementRepository
                .findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .map(a -> a.getStatus() == AgreementStatus.REJECTED)
                .orElse(false);

            if (latestWasRejected) {
                stateMachine.transition(tenant, OnboardingStatus.AGREEMENT_PENDING, tenantId, "TENANT");
            }
        }

        status = tenant.getOnboardingStatus();
        if (status != OnboardingStatus.AGREEMENT_PENDING) {
            throw new IllegalStateException("Agreement can only be submitted after email verification.");
        }

        stateMachine.transition(tenant, OnboardingStatus.AGREEMENT_SIGNED, tenantId, "TENANT");
        tenantRepository.save(tenant);

        // Mark the previous rejected agreement as SUPERSEDED
        agreementRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
            .filter(a -> a.getStatus() == AgreementStatus.REJECTED)
            .ifPresent(prev -> {
                prev.setStatus(AgreementStatus.SUPERSEDED);
                agreementRepository.save(prev);
            });

        TenantAgreement agreement = TenantAgreement.builder()
            .tenantId(tenantId)
            .agreementUid(UUID.randomUUID().toString())
            .termsVersion(request.getTermsVersion())
            .effectiveDate(request.getEffectiveDate())
            .revenueSharePct(request.getRevenueSharePct())
            .settlementFrequency(request.getSettlementFrequency())
            .pointsCurrency(request.getPointsCurrency() != null ? request.getPointsCurrency() : "INR")
            .expectedDailyTxnVolume(request.getExpectedDailyTxnVolume())
            .billingContactName(request.getBillingContactName())
            .billingAddress(request.getBillingAddress())
            .paymentMethod(request.getPaymentMethod())
            .contractDurationMonths(request.getContractDurationMonths() != null ? request.getContractDurationMonths() : 12)
            .autoRenewal(request.getAutoRenewal() != null ? request.getAutoRenewal() : true)
            .signedByName(request.getSignedByName())
            .signedByEmail(request.getSignedByEmail())
            .signedByDesignation(request.getSignedByDesignation())
            .signedAt(Instant.now())
            .build();

        agreementRepository.save(agreement);
    }
}


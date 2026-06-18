package com.loyaltyos.merchants.service;

import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.dto.MerchantAgreementPrefillResponse;
import com.loyaltyos.merchants.dto.MerchantAgreementResponse;
import com.loyaltyos.merchants.dto.SubmitMerchantAgreementRequest;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantAgreement;
import com.loyaltyos.merchants.enums.MerchantAgreementStatus;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.enums.SettlementCycle;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.entity.MerchantOnboardingAudit;
import com.loyaltyos.merchants.repository.MerchantAgreementRepository;
import com.loyaltyos.merchants.repository.MerchantOnboardingAuditRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.service.statemachine.MerchantOnboardingStateMachine;
import com.loyaltyos.merchants.support.MerchantAgreementMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantAgreementService {

    private final MerchantRepository merchantRepository;
    private final MerchantAgreementRepository agreementRepository;
    private final MerchantOnboardingAuditRepository auditRepository;
    private final MerchantOnboardingStateMachine stateMachine;
    private final MerchantValidationService validationService;
    private final MerchantOnboardingEmailGuard onboardingEmailGuard;
    private final MerchantProperties merchantProperties;

    public MerchantAgreementService(
        MerchantRepository merchantRepository,
        MerchantAgreementRepository agreementRepository,
        MerchantOnboardingAuditRepository auditRepository,
        MerchantOnboardingStateMachine stateMachine,
        MerchantValidationService validationService,
        MerchantOnboardingEmailGuard onboardingEmailGuard,
        MerchantProperties merchantProperties
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.agreementRepository = Objects.requireNonNull(agreementRepository, "agreementRepository");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.validationService = Objects.requireNonNull(validationService, "validationService");
        this.onboardingEmailGuard = Objects.requireNonNull(onboardingEmailGuard, "onboardingEmailGuard");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
    }

    @Transactional(readOnly = true)
    public MerchantAgreementPrefillResponse getPrefill(String tenantId, String merchantUid) {
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        boolean hasAgreement = agreementRepository
            .findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
                tenantId, merchantUid, MerchantAgreementStatus.APPROVED)
            .isPresent();
        return MerchantAgreementMapper.toPrefill(merchant, hasAgreement);
    }

    @Transactional(readOnly = true)
    public MerchantAgreementResponse getCurrentAgreement(String tenantId, String merchantUid) {
        return agreementRepository
            .findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
                tenantId, merchantUid, MerchantAgreementStatus.APPROVED)
            .map(MerchantAgreementMapper::toResponse)
            .orElse(null);
    }

    @Transactional
    public MerchantAgreementResponse submitAgreement(
        String tenantId,
        String merchantUid,
        SubmitMerchantAgreementRequest request,
        String actorEmail
    ) {
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terms must be accepted to proceed");
        }

        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        String fromStage = merchant.getOnboardingStage().name();

        applyPortalContactEmailIfProvided(tenantId, merchantUid, merchant, request);

        if (request.getProposedEarnRateMultiplier() != null) {
            validationService.validateEarnRateMultiplier(request.getProposedEarnRateMultiplier());
        }

        agreementRepository
            .findTopByTenantIdAndMerchantUidAndStatusOrderByCreatedAtDesc(
                tenantId, merchantUid, MerchantAgreementStatus.APPROVED)
            .ifPresent(existing -> {
                existing.setStatus(MerchantAgreementStatus.SUPERSEDED);
                agreementRepository.save(existing);
            });

        MerchantAgreement agreement = new MerchantAgreement();
        agreement.setTenantId(tenantId);
        agreement.setMerchantUid(merchantUid);
        agreement.setAgreementUid("MAG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        agreement.setTermsVersion(request.getTermsVersion().trim());
        agreement.setEffectiveDate(request.getEffectiveDate());
        agreement.setRevenueSharePct(request.getRevenueSharePct());
        agreement.setSettlementCycle(parseSettlementCycle(request.getSettlementCycle()));
        agreement.setPointsCurrency(request.getPointsCurrency().trim().toUpperCase());
        agreement.setExpectedDailyTxnVolume(request.getExpectedDailyTxnVolume());
        agreement.setBillingContactName(blankToNull(request.getBillingContactName()));
        agreement.setBillingAddress(blankToNull(request.getBillingAddress()));
        agreement.setPaymentMethod(blankToNull(request.getPaymentMethod()));
        agreement.setContractDurationMonths(
            request.getContractDurationMonths() != null ? request.getContractDurationMonths() : 12);
        agreement.setAutoRenewal(request.getAutoRenewal() != null ? request.getAutoRenewal() : true);
        agreement.setProposedEarnRateMultiplier(request.getProposedEarnRateMultiplier());
        agreement.setMerchantFundedCampaignsAllowed(
            request.getMerchantFundedCampaignsAllowed() == null
                || request.getMerchantFundedCampaignsAllowed());
        agreement.setSignedByName(request.getSignedByName().trim());
        agreement.setSignedByEmail(request.getSignedByEmail().trim().toLowerCase());
        agreement.setSignedByDesignation(blankToNull(request.getSignedByDesignation()));
        agreement.setSignedAt(Instant.now());
        agreement.setSubmittedByEmail(actorEmail != null ? actorEmail : "system");
        boolean pendingFinance = merchantProperties.isFinanceAgreementGateEnabled();
        agreement.setStatus(
            pendingFinance ? MerchantAgreementStatus.PENDING_FINANCE : MerchantAgreementStatus.APPROVED);
        agreementRepository.save(agreement);

        if (!pendingFinance) {
            applyApprovedAgreement(merchant, agreement, actorEmail, fromStage);
        } else {
            recordAudit(
                merchant,
                fromStage,
                merchant.getOnboardingStage().name(),
                actorEmail,
                "AGREEMENT_PENDING_FINANCE",
                null
            );
            merchantRepository.save(merchant);
        }

        return MerchantAgreementMapper.toResponse(agreement);
    }

    @Transactional
    public MerchantAgreementResponse approveFinanceAgreement(
        String tenantId,
        String merchantUid,
        String agreementUid,
        String reviewerEmail
    ) {
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        MerchantAgreement agreement = agreementRepository
            .findByTenantIdAndAgreementUid(tenantId, agreementUid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agreement not found"));
        if (agreement.getStatus() != MerchantAgreementStatus.PENDING_FINANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agreement is not pending finance approval");
        }
        if (agreement.getSubmittedByEmail() != null
            && agreement.getSubmittedByEmail().equalsIgnoreCase(reviewerEmail)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Finance approver must differ from submitter"
            );
        }
        String fromStage = merchant.getOnboardingStage().name();
        agreement.setStatus(MerchantAgreementStatus.APPROVED);
        agreementRepository.save(agreement);
        applyApprovedAgreement(merchant, agreement, reviewerEmail, fromStage);
        return MerchantAgreementMapper.toResponse(agreement);
    }

    private void applyApprovedAgreement(
        Merchant merchant,
        MerchantAgreement agreement,
        String actorEmail,
        String fromStage
    ) {
        merchant.setAgreementAcceptedAt(Instant.now());
        merchant.setAgreementDocumentUrl(null);
        merchant.setSettlementCycle(agreement.getSettlementCycle());
        if (agreement.getProposedEarnRateMultiplier() != null) {
            merchant.setEarnRateMultiplier(agreement.getProposedEarnRateMultiplier());
        }

        MerchantOnboardingStage current = merchant.getOnboardingStage();
        if (current == MerchantOnboardingStage.REGISTRATION) {
            stateMachine.transition(
                merchant,
                MerchantOnboardingStage.AGREEMENT,
                actorEmail,
                "AGREEMENT_SUBMITTED",
                null,
                Map.of(
                    "agreementUid", agreement.getAgreementUid(),
                    "termsVersion", agreement.getTermsVersion()
                )
            );
            recordAudit(
                merchant,
                fromStage,
                MerchantOnboardingStage.AGREEMENT.name(),
                actorEmail,
                "AGREEMENT_RECORDED",
                null
            );
        } else {
            recordAudit(
                merchant,
                fromStage,
                current.name(),
                actorEmail,
                "AGREEMENT_UPDATED",
                null
            );
        }
        merchantRepository.save(merchant);
    }

    private void recordAudit(
        Merchant merchant,
        String fromStage,
        String toStage,
        String actorEmail,
        String action,
        String reason
    ) {
        MerchantOnboardingAudit audit = new MerchantOnboardingAudit();
        audit.setAuditUid(UUID.randomUUID().toString());
        audit.setTenantId(merchant.getTenantId());
        audit.setMerchantUid(merchant.getMerchantUid());
        audit.setFromStage(fromStage);
        audit.setToStage(toStage);
        audit.setActorEmail(actorEmail != null ? actorEmail : "system");
        audit.setAction(action);
        audit.setReason(reason);
        auditRepository.save(audit);
    }

    private void applyPortalContactEmailIfProvided(
        String tenantId,
        String merchantUid,
        Merchant merchant,
        SubmitMerchantAgreementRequest request
    ) {
        if (request.getPortalContactEmail() == null || request.getPortalContactEmail().isBlank()) {
            return;
        }
        String normalized = MerchantOnboardingEmailGuard.normalize(request.getPortalContactEmail());
        String current = MerchantOnboardingEmailGuard.normalize(merchant.getContactEmail());
        if (normalized.equals(current)) {
            return;
        }
        onboardingEmailGuard.assertPortalEmailAvailable(tenantId, normalized, merchantUid);
        merchant.setContactEmail(normalized);
    }

    private Merchant getMerchantOrThrow(String tenantId, String merchantUid) {
        return merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
    }

    private static SettlementCycle parseSettlementCycle(String raw) {
        if (raw == null || raw.isBlank()) {
            return SettlementCycle.MONTHLY;
        }
        try {
            return SettlementCycle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid settlement cycle: " + raw);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

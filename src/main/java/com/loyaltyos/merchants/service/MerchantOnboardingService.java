package com.loyaltyos.merchants.service;

import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.dto.CreateMerchantRequest;
import com.loyaltyos.merchants.dto.MerchantActivateResponse;
import com.loyaltyos.merchants.dto.MerchantInviteLinkResponse;
import com.loyaltyos.merchants.dto.MerchantResendInviteResponse;
import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.dto.SubmitMerchantAgreementRequest;
import com.loyaltyos.merchants.dto.UpdateMerchantConfigRequest;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantOnboardingAudit;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.enums.SettlementCycle;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantOnboardingAuditRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.service.statemachine.MerchantOnboardingStateMachine;
import com.loyaltyos.merchants.support.MerchantMapper;
import com.loyaltyos.merchants.support.MerchantUidGenerator;
import com.loyaltyos.onboarding.exception.InvalidStateException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantOnboardingService {

    private final MerchantRepository merchantRepository;
    private final MerchantOnboardingAuditRepository auditRepository;
    private final MerchantOnboardingStateMachine stateMachine;
    private final MerchantProperties merchantProperties;
    private final MerchantInviteService merchantInviteService;
    private final MerchantValidationService merchantValidationService;
    private final MerchantAgreementService merchantAgreementService;
    private final MerchantOnboardingEmailGuard onboardingEmailGuard;
    private final MerchantConfigApprovalService configApprovalService;

    public MerchantOnboardingService(
        MerchantRepository merchantRepository,
        MerchantOnboardingAuditRepository auditRepository,
        MerchantOnboardingStateMachine stateMachine,
        MerchantProperties merchantProperties,
        MerchantInviteService merchantInviteService,
        MerchantValidationService merchantValidationService,
        MerchantAgreementService merchantAgreementService,
        MerchantOnboardingEmailGuard onboardingEmailGuard,
        MerchantConfigApprovalService configApprovalService
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
        this.merchantInviteService = Objects.requireNonNull(merchantInviteService, "merchantInviteService");
        this.merchantValidationService = Objects.requireNonNull(
            merchantValidationService, "merchantValidationService");
        this.merchantAgreementService = Objects.requireNonNull(
            merchantAgreementService, "merchantAgreementService");
        this.onboardingEmailGuard = Objects.requireNonNull(onboardingEmailGuard, "onboardingEmailGuard");
        this.configApprovalService = Objects.requireNonNull(configApprovalService, "configApprovalService");
    }

    private void assertEnabled() {
        if (!merchantProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Merchant module is disabled");
        }
    }

    @Transactional
    public MerchantResponse register(String tenantId, CreateMerchantRequest request, String actorEmail) {
        assertEnabled();
        String contactEmail = MerchantOnboardingEmailGuard.normalize(request.getContactEmail());
        onboardingEmailGuard.assertPortalEmailAvailable(tenantId, contactEmail, null);

        Merchant merchant = new Merchant();
        merchant.setTenantId(tenantId);
        merchant.setMerchantUid(MerchantUidGenerator.generate());
        merchant.setLegalName(request.getLegalName().trim());
        merchant.setDisplayName(blankToNull(request.getDisplayName()));
        merchant.setCategory(request.getCategory().trim());
        merchant.setContactEmail(contactEmail);
        merchant.setContactPhone(blankToNull(request.getContactPhone()));
        merchant.setBankDetailsVaultRef(blankToNull(request.getBankDetailsVaultRef()));
        merchant.setTaxId(request.getTaxId().trim());
        merchant.setOnboardingStage(MerchantOnboardingStage.REGISTRATION);
        merchant.setEarnRateMultiplier(
            request.getEarnRateMultiplier() != null ? request.getEarnRateMultiplier() : BigDecimal.ONE);
        merchant.setSettlementCycle(parseSettlementCycle(request.getSettlementCycle()));
        merchant.setCreatedBy(actorEmail);
        merchantRepository.save(merchant);

        recordAudit(merchant, "INITIAL", MerchantOnboardingStage.REGISTRATION.name(),
            actorEmail, "REGISTRATION_INITIATED", null,
            Map.of("legalName", merchant.getLegalName()));
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional(readOnly = true)
    public Page<MerchantResponse> list(String tenantId, MerchantOnboardingStage stage, Pageable pageable) {
        assertEnabled();
        Page<Merchant> page = stage == null
            ? merchantRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
            : merchantRepository.findByTenantIdAndOnboardingStageOrderByCreatedAtDesc(tenantId, stage, pageable);
        return page.map(MerchantMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MerchantResponse get(String tenantId, String merchantUid) {
        assertEnabled();
        return MerchantMapper.toResponse(getMerchantOrThrow(tenantId, merchantUid));
    }

    @Transactional(readOnly = true)
    public List<MerchantOnboardingAudit> auditTrail(String tenantId, String merchantUid) {
        assertEnabled();
        getMerchantOrThrow(tenantId, merchantUid);
        return auditRepository.findByTenantIdAndMerchantUidOrderByCreatedAtDesc(tenantId, merchantUid);
    }

    @Transactional
    public MerchantResponse updateContactEmail(
        String tenantId,
        String merchantUid,
        String contactEmail,
        String actorEmail
    ) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        if (merchant.getOnboardingStage() == MerchantOnboardingStage.ACTIVE
            || merchant.getOnboardingStage() == MerchantOnboardingStage.SUSPENDED) {
            throw new InvalidStateException(
                "Contact email cannot be changed after the merchant is active",
                merchant.getOnboardingStage().name(),
                "CONTACT_UPDATE"
            );
        }
        String normalized = MerchantOnboardingEmailGuard.normalize(contactEmail);
        onboardingEmailGuard.assertPortalEmailAvailable(tenantId, normalized, merchantUid);
        merchant.setContactEmail(normalized);
        merchantRepository.save(merchant);
        recordAudit(
            merchant,
            merchant.getOnboardingStage().name(),
            merchant.getOnboardingStage().name(),
            actorEmail,
            "CONTACT_EMAIL_UPDATED",
            null,
            Map.of("contactEmail", normalized)
        );
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResponse submitAgreement(
        String tenantId,
        String merchantUid,
        SubmitMerchantAgreementRequest request,
        String actorEmail
    ) {
        assertEnabled();
        merchantAgreementService.submitAgreement(tenantId, merchantUid, request, actorEmail);
        return MerchantMapper.toResponse(getMerchantOrThrow(tenantId, merchantUid));
    }

    @Transactional
    public MerchantResponse configure(
        String tenantId,
        String merchantUid,
        UpdateMerchantConfigRequest request,
        String actorEmail
    ) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        if (request.getEarnRateMultiplier() != null) {
            merchantValidationService.validateEarnRateMultiplier(request.getEarnRateMultiplier());
        }
        if (request.getCommissionConfigJson() != null) {
            merchantValidationService.validateCommissionConfigJson(request.getCommissionConfigJson());
        }
        if (request.getEligibleCategoriesJson() != null) {
            merchantValidationService.validateEligibleCategoriesJson(request.getEligibleCategoriesJson());
        }
        if (request.getCapabilitiesJson() != null) {
            merchantValidationService.validateCapabilitiesJson(request.getCapabilitiesJson());
        }
        if (merchantProperties.isMakerCheckerConfigEnabled()) {
            return configApprovalService.submitConfigForApproval(tenantId, merchantUid, request, actorEmail);
        }
        applyConfig(merchant, request);
        stateMachine.transition(
            merchant,
            MerchantOnboardingStage.CONFIGURATION,
            actorEmail,
            "CONFIGURATION_APPLIED",
            null,
            null
        );
        merchantRepository.save(merchant);
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResponse completeIntegrationTest(String tenantId, String merchantUid, String actorEmail) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        merchant.setIntegrationTestPassedAt(Instant.now());
        stateMachine.transition(
            merchant,
            MerchantOnboardingStage.INTEGRATION,
            actorEmail,
            "INTEGRATION_TEST_PASSED",
            "Sandbox integration test completed",
            null
        );
        merchantRepository.save(merchant);
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantActivateResponse activate(String tenantId, String merchantUid, String actorEmail) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        merchantValidationService.requireContactEmailForActivation(merchant);
        onboardingEmailGuard.assertPortalEmailAvailable(
            tenantId, merchant.getContactEmail(), merchant.getMerchantUid());
        if (merchant.getIntegrationTestPassedAt() == null) {
            throw new InvalidStateException(
                "Integration test must be completed before activation",
                merchant.getOnboardingStage().name(),
                MerchantOnboardingStage.INTEGRATION.name()
            );
        }
        stateMachine.transition(
            merchant,
            MerchantOnboardingStage.ACTIVE,
            actorEmail,
            "ACTIVATED",
            "Merchant activated for live transactions",
            null
        );
        merchantRepository.save(merchant);

        MerchantInviteService.InviteIssueResult invite = merchantInviteService.issuePortalInvite(merchant);

        MerchantActivateResponse response = new MerchantActivateResponse();
        copyResponse(MerchantMapper.toResponse(merchant), response);
        response.setPortalUsername(invite.credentials().getUsername());
        response.setInviteEmailSent(invite.emailSent());
        response.setInviteUrl(invite.inviteUrl());
        if (invite.expiresAt() != null) {
            response.setInviteExpiresAt(invite.expiresAt().toString());
        }
        return response;
    }

    @Transactional
    public MerchantResponse suspend(String tenantId, String merchantUid, String reason, String actorEmail) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        merchant.setSettlementHold(true);
        stateMachine.transition(
            merchant,
            MerchantOnboardingStage.SUSPENDED,
            actorEmail,
            "SUSPENDED",
            reason,
            null
        );
        merchantRepository.save(merchant);
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResponse unsuspend(String tenantId, String merchantUid, String actorEmail) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        merchant.setSettlementHold(false);
        stateMachine.transition(
            merchant,
            MerchantOnboardingStage.ACTIVE,
            actorEmail,
            "UNSUSPENDED",
            null,
            null
        );
        merchantRepository.save(merchant);
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional
    public MerchantResendInviteResponse resendPortalInvite(String tenantId, String merchantUid, String actorEmail) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        MerchantInviteService.InviteIssueResult result = merchantInviteService.resendPortalInvite(merchant);
        recordAudit(
            merchant,
            merchant.getOnboardingStage().name(),
            merchant.getOnboardingStage().name(),
            actorEmail,
            "PORTAL_INVITE_RESENT",
            null,
            Map.of("inviteEmailSent", result.emailSent())
        );
        MerchantResendInviteResponse response = new MerchantResendInviteResponse();
        response.setInviteEmailSent(result.emailSent());
        response.setPortalUsername(result.credentials().getUsername());
        response.setAlreadyActivated(false);
        response.setInviteUrl(result.inviteUrl());
        if (result.expiresAt() != null) {
            response.setInviteExpiresAt(result.expiresAt().toString());
        }
        return response;
    }

    @Transactional
    public MerchantInviteLinkResponse issuePortalInviteLink(String tenantId, String merchantUid, String actorEmail) {
        assertEnabled();
        Merchant merchant = getMerchantOrThrow(tenantId, merchantUid);
        MerchantInviteService.InviteIssueResult result = merchantInviteService.issuePortalInviteLink(merchant);
        recordAudit(
            merchant,
            merchant.getOnboardingStage().name(),
            merchant.getOnboardingStage().name(),
            actorEmail,
            "PORTAL_INVITE_LINK_ISSUED",
            null,
            null
        );
        MerchantInviteLinkResponse response = new MerchantInviteLinkResponse();
        response.setInviteUrl(result.inviteUrl());
        response.setPortalUsername(result.credentials().getUsername());
        if (result.expiresAt() != null) {
            response.setInviteExpiresAt(result.expiresAt().toString());
        }
        return response;
    }

    Merchant getMerchantOrThrow(String tenantId, String merchantUid) {
        return merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
    }

    private static void applyConfig(Merchant merchant, UpdateMerchantConfigRequest request) {
        if (request.getEarnRateMultiplier() != null) {
            merchant.setEarnRateMultiplier(request.getEarnRateMultiplier());
        }
        if (request.getCommissionConfigJson() != null) {
            merchant.setCommissionConfig(request.getCommissionConfigJson());
        }
        if (request.getSettlementCycle() != null && !request.getSettlementCycle().isBlank()) {
            merchant.setSettlementCycle(parseSettlementCycle(request.getSettlementCycle()));
        }
        if (request.getEligibleCategoriesJson() != null) {
            merchant.setEligibleCategoriesJson(request.getEligibleCategoriesJson());
        }
        if (request.getCapabilitiesJson() != null) {
            merchant.setCapabilitiesJson(request.getCapabilitiesJson());
        }
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
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void recordAudit(
        Merchant merchant,
        String fromStage,
        String toStage,
        String actorEmail,
        String action,
        String reason,
        Map<String, Object> metadata
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

    private static void copyResponse(MerchantResponse from, MerchantResponse to) {
        to.setMerchantUid(from.getMerchantUid());
        to.setLegalName(from.getLegalName());
        to.setDisplayName(from.getDisplayName());
        to.setCategory(from.getCategory());
        to.setContactEmail(from.getContactEmail());
        to.setOnboardingStage(from.getOnboardingStage());
        to.setEarnRateMultiplier(from.getEarnRateMultiplier());
        to.setSettlementCycle(from.getSettlementCycle());
        to.setAgreementAcceptedAt(from.getAgreementAcceptedAt());
        to.setIntegrationTestPassedAt(from.getIntegrationTestPassedAt());
        to.setActive(from.isActive());
        to.setSuspended(from.isSuspended());
        to.setCreatedAt(from.getCreatedAt());
    }
}

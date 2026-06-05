package com.loyaltyos.support.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.integration.entity.ApiRequestAuditLog;
import com.loyaltyos.integration.repository.ApiRequestAuditLogRepository;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.enums.SubscriptionTier;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.support.dto.CreateSupportCaseRequest;
import com.loyaltyos.support.dto.SupportCaseResponse;
import com.loyaltyos.support.dto.SupportContextResponse;
import com.loyaltyos.support.dto.UpdateSupportCaseStatusRequest;
import com.loyaltyos.support.entity.SupportCase;
import com.loyaltyos.support.enums.SupportActorType;
import com.loyaltyos.support.enums.SupportCasePriority;
import com.loyaltyos.support.enums.SupportCaseStatus;
import com.loyaltyos.support.exception.SupportException;
import com.loyaltyos.support.repository.SupportCaseRepository;
import com.loyaltyos.support.support.SupportSlaSupport;
import com.loyaltyos.support.support.SupportStatusTransitionSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportCaseService {

    private static final int MAX_CASES_PER_DAY = 25;

    private final SupportCaseRepository supportCaseRepository;
    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final ApiRequestAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public SupportCaseService(
        SupportCaseRepository supportCaseRepository,
        TenantOnboardingRepository tenantOnboardingRepository,
        ApiRequestAuditLogRepository auditLogRepository,
        ObjectMapper objectMapper
    ) {
        this.supportCaseRepository = Objects.requireNonNull(supportCaseRepository);
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository);
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public SupportContextResponse getContext(Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        TenantOnboarding tenant = tenantOnboardingRepository.findByTenantId(tenantId).orElse(null);
        SubscriptionTier tier = tenant != null ? tenant.getSubscriptionTier() : SubscriptionTier.STANDARD;

        SupportContextResponse ctx = new SupportContextResponse();
        ctx.setTenantId(tenantId);
        ctx.setCompanyName(tenant != null ? tenant.getCompanyName() : null);
        ctx.setSubscriptionTier(tier.name());
        ctx.setUserEmail(TenantJwt.email(jwt));
        ctx.setSlaResponseHint(SupportSlaSupport.responseHint(tier, false));
        ctx.setRecentIntegrationErrors(recentIntegrationErrors(tenantId));
        return ctx;
    }

    @Transactional(readOnly = true)
    public List<SupportCaseResponse> listCasesForTenant(String tenantId) {
        return supportCaseRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .map(row -> toResponse(row, false, tenantLookup(row.getTenantId())))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportCaseResponse> listCasesForAdmin(SupportCaseStatus statusFilter) {
        List<SupportCase> rows = statusFilter == null
            ? supportCaseRepository.findAllByOrderByCreatedAtDesc()
            : supportCaseRepository.findByStatusOrderByCreatedAtDesc(statusFilter);
        Map<String, TenantOnboarding> tenantCache = new LinkedHashMap<>();
        return rows.stream()
            .map(row -> toResponse(row, true, tenantLookupCached(row.getTenantId(), tenantCache)))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupportCaseResponse getCaseForTenant(String tenantId, String caseUid) {
        SupportCase row = requireCaseForTenant(tenantId, caseUid);
        return toResponse(row, false, tenantLookup(tenantId));
    }

    @Transactional(readOnly = true)
    public SupportCaseResponse getCaseForAdmin(String caseUid) {
        SupportCase row = requireCaseByUid(caseUid);
        return toResponse(row, true, tenantLookup(row.getTenantId()));
    }

    @Transactional
    public SupportCaseResponse createCase(Jwt jwt, CreateSupportCaseRequest request) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        enforceDailyLimit(tenantId);

        TenantOnboarding tenant = tenantOnboardingRepository.findByTenantId(tenantId).orElse(null);
        SubscriptionTier tier = tenant != null ? tenant.getSubscriptionTier() : SubscriptionTier.STANDARD;
        boolean urgent = request.getPriority() == SupportCasePriority.URGENT;

        SupportCase row = new SupportCase();
        row.setTenantId(tenantId);
        row.setCaseUid("case_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        row.setCreatedByUserId(jwt.getSubject() != null ? jwt.getSubject() : "unknown");
        row.setCreatedByEmail(TenantJwt.email(jwt));
        row.setCategory(request.getCategory());
        row.setPriority(request.getPriority() != null ? request.getPriority() : SupportCasePriority.NORMAL);
        row.setSubject(request.getSubject().trim());
        row.setDescription(request.getDescription().trim());
        row.setCorrelationId(trimToNull(request.getCorrelationId()));
        row.setProgrammeUid(trimToNull(request.getProgrammeUid()));
        row.setPageUrl(trimToNull(request.getPageUrl()));
        row.setContextJson(buildContextJson(tenantId, tenant, jwt, request));
        row.setCreatedAt(Instant.now());
        row.setUpdatedAt(Instant.now());

        SupportCase saved = supportCaseRepository.save(row);
        return toResponse(saved, false, tenant);
    }

    @Transactional
    public SupportCaseResponse updateStatusForTenant(
        String tenantId,
        String caseUid,
        UpdateSupportCaseStatusRequest request,
        Jwt jwt
    ) {
        SupportCase row = requireCaseForTenant(tenantId, caseUid);
        applyStatusChange(
            row,
            request.getStatus(),
            false,
            actorLabel(jwt, SupportActorType.TENANT)
        );
        SupportCase saved = supportCaseRepository.save(row);
        return toResponse(saved, false, tenantLookup(tenantId));
    }

    @Transactional
    public SupportCaseResponse updateStatusForAdmin(
        String caseUid,
        UpdateSupportCaseStatusRequest request,
        Jwt jwt
    ) {
        SupportCase row = requireCaseByUid(caseUid);
        applyStatusChange(
            row,
            request.getStatus(),
            true,
            actorLabel(jwt, SupportActorType.PLATFORM_ADMIN)
        );
        SupportCase saved = supportCaseRepository.save(row);
        return toResponse(saved, true, tenantLookup(row.getTenantId()));
    }

    private void applyStatusChange(
        SupportCase row,
        SupportCaseStatus target,
        boolean platformAdmin,
        String actorLabel
    ) {
        SupportCaseStatus current = row.getStatus();
        if (!SupportStatusTransitionSupport.canTransition(current, target, platformAdmin)) {
            throw new SupportException(
                "INVALID_STATUS_TRANSITION",
                "Cannot change status from " + current + " to " + target
            );
        }
        row.setStatus(target);
        row.setStatusUpdatedAt(Instant.now());
        row.setStatusUpdatedBy(actorLabel);
        row.setStatusUpdatedByType(
            platformAdmin ? SupportActorType.PLATFORM_ADMIN : SupportActorType.TENANT
        );
        if (target == SupportCaseStatus.RESOLVED) {
            row.setResolvedAt(Instant.now());
        } else if (current == SupportCaseStatus.RESOLVED) {
            row.setResolvedAt(null);
        }
        row.setUpdatedAt(Instant.now());
    }

    private static String actorLabel(Jwt jwt, SupportActorType type) {
        if (type == SupportActorType.PLATFORM_ADMIN) {
            String adminUid = TenantJwt.adminUid(jwt);
            String email = TenantJwt.email(jwt);
            if (email != null && !email.isBlank()) {
                return email.trim();
            }
            return adminUid != null ? adminUid : "platform-admin";
        }
        String email = TenantJwt.email(jwt);
        return email != null && !email.isBlank() ? email.trim() : jwt.getSubject();
    }

    private SupportCase requireCaseForTenant(String tenantId, String caseUid) {
        return supportCaseRepository.findByTenantIdAndCaseUid(tenantId, caseUid.trim())
            .orElseThrow(() -> new SupportException("CASE_NOT_FOUND", "Support case not found"));
    }

    private SupportCase requireCaseByUid(String caseUid) {
        return supportCaseRepository.findByCaseUid(caseUid.trim())
            .orElseThrow(() -> new SupportException("CASE_NOT_FOUND", "Support case not found"));
    }

    private TenantOnboarding tenantLookup(String tenantId) {
        return tenantOnboardingRepository.findByTenantId(tenantId).orElse(null);
    }

    private TenantOnboarding tenantLookupCached(String tenantId, Map<String, TenantOnboarding> cache) {
        return cache.computeIfAbsent(tenantId, id -> tenantLookup(id));
    }

    private void enforceDailyLimit(String tenantId) {
        Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
        long count = supportCaseRepository.countByTenantIdAndCreatedAtAfter(tenantId, since);
        if (count >= MAX_CASES_PER_DAY) {
            throw new SupportException(
                "RATE_LIMIT",
                "Daily support case limit reached. Please try again tomorrow or contact your account manager."
            );
        }
    }

    private List<SupportContextResponse.IntegrationErrorSnippet> recentIntegrationErrors(String tenantId) {
        return auditLogRepository.findTop10ByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .filter(log -> log.getHttpStatus() >= 400)
            .limit(5)
            .map(this::toSnippet)
            .collect(Collectors.toList());
    }

    private SupportContextResponse.IntegrationErrorSnippet toSnippet(ApiRequestAuditLog log) {
        SupportContextResponse.IntegrationErrorSnippet s = new SupportContextResponse.IntegrationErrorSnippet();
        s.setRequestId(log.getRequestId());
        s.setHttpMethod(log.getHttpMethod());
        s.setRequestPath(log.getRequestPath());
        s.setHttpStatus(log.getHttpStatus());
        s.setErrorCode(log.getErrorCode());
        s.setErrorMessage(log.getErrorMessage());
        s.setCreatedAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
        return s;
    }

    private String buildContextJson(
        String tenantId,
        TenantOnboarding tenant,
        Jwt jwt,
        CreateSupportCaseRequest request
    ) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("tenantId", tenantId);
        ctx.put("companyName", tenant != null ? tenant.getCompanyName() : null);
        ctx.put(
            "subscriptionTier",
            tenant != null ? tenant.getSubscriptionTier().name() : SubscriptionTier.STANDARD.name()
        );
        ctx.put("userId", jwt.getSubject());
        ctx.put("userEmail", TenantJwt.email(jwt));
        ctx.put("role", TenantJwt.role(jwt));
        ctx.put("programmeUid", trimToNull(request.getProgrammeUid()));
        ctx.put("correlationId", trimToNull(request.getCorrelationId()));
        ctx.put("pageUrl", trimToNull(request.getPageUrl()));
        ctx.put("recentIntegrationErrors", recentIntegrationErrors(tenantId));
        try {
            return objectMapper.writeValueAsString(ctx);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private SupportCaseResponse toResponse(
        SupportCase row,
        boolean platformAdminActor,
        TenantOnboarding tenant
    ) {
        boolean urgent = row.getPriority() == SupportCasePriority.URGENT;
        SubscriptionTier tier = tenant != null ? tenant.getSubscriptionTier() : SubscriptionTier.STANDARD;

        SupportCaseResponse r = new SupportCaseResponse();
        r.setCaseUid(row.getCaseUid());
        r.setTenantId(row.getTenantId());
        r.setCreatedByEmail(row.getCreatedByEmail());
        r.setCategory(row.getCategory());
        r.setPriority(row.getPriority());
        r.setStatus(row.getStatus());
        r.setSubject(row.getSubject());
        r.setDescription(row.getDescription());
        r.setCorrelationId(row.getCorrelationId());
        r.setProgrammeUid(row.getProgrammeUid());
        r.setPageUrl(row.getPageUrl());
        r.setCreatedAt(row.getCreatedAt());
        r.setUpdatedAt(row.getUpdatedAt());
        r.setResolvedAt(row.getResolvedAt());
        r.setStatusUpdatedAt(row.getStatusUpdatedAt());
        r.setStatusUpdatedBy(row.getStatusUpdatedBy());
        r.setStatusUpdatedByType(row.getStatusUpdatedByType());
        r.setSlaResponseHint(SupportSlaSupport.responseHint(tier, urgent));
        r.setCompanyName(tenant != null ? tenant.getCompanyName() : null);
        r.setSubscriptionTier(tier.name());
        r.setAllowedNextStatuses(
            new ArrayList<>(SupportStatusTransitionSupport.allowedTargets(row.getStatus(), platformAdminActor))
        );
        return r;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}

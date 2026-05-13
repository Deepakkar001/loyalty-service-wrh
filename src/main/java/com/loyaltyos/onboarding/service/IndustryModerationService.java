package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.RefBusinessCategory;
import com.loyaltyos.onboarding.domain.enums.BusinessCategoryStatus;
import com.loyaltyos.onboarding.dto.response.AdminBusinessCategoryItem;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.RefBusinessCategoryRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Admin-side moderation for tenant-suggested industries (a.k.a. business categories).
 *
 * <p>Approvals make a row visible in the public onboarding dropdown. Rejections leave the row in
 * the database (as audit trail / dedupe key) but keep it hidden from public metadata.</p>
 *
 * <p><b>State machine</b> (any transition is reversible by re-approving, except rejection of
 * system-seeded rows which is forbidden — use {@link #deactivate} for those instead):</p>
 *
 * <pre>
 *   PENDING_REVIEW ──approve──→ APPROVED+active
 *   PENDING_REVIEW ──reject───→ REJECTED
 *   REJECTED       ──approve──→ APPROVED+active   (re-approve)
 *   APPROVED+x     ──reject───→ REJECTED          (revoke; blocked for seeded rows)
 *   APPROVED+act   ──deact────→ APPROVED+inactive (hide from public)
 *   APPROVED+inact ──reactiv──→ APPROVED+active   (unhide)
 *   APPROVED+x     ──approve──→ APPROVED+active   (label/sortOrder edit)
 * </pre>
 */
@Service
public class IndustryModerationService {

    private static final Logger log = LoggerFactory.getLogger(IndustryModerationService.class);

    private final RefBusinessCategoryRepository categoryRepository;
    private final TenantOnboardingRepository tenantRepository;
    private final OnboardingAuditLogRepository auditLogRepository;

    public IndustryModerationService(
        RefBusinessCategoryRepository categoryRepository,
        TenantOnboardingRepository tenantRepository,
        OnboardingAuditLogRepository auditLogRepository
    ) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
    }

    @Transactional(readOnly = true)
    public List<AdminBusinessCategoryItem> list(BusinessCategoryStatus status) {
        List<RefBusinessCategory> rows = (status == null)
                ? categoryRepository.findAllByOrderByStatusAscSortOrderAscLabelAsc()
                : categoryRepository.findByStatusOrderBySortOrderAscLabelAsc(status);

        return rows.stream().map(c -> {
            String submittedByCompany = null;
            String submittedByEmail = null;
            if (c.getSubmittedByTenantId() != null) {
                var tenant = tenantRepository.findByTenantId(c.getSubmittedByTenantId()).orElse(null);
                if (tenant != null) {
                    submittedByCompany = tenant.getCompanyName();
                    submittedByEmail = tenant.getEmail();
                }
            }
            return toItem(c, submittedByCompany, submittedByEmail);
        }).toList();
    }

    /**
     * Approve / re-approve / reactivate / edit. Allowed from any state.
     *
     * <ul>
     *   <li>From PENDING/REJECTED → APPROVED+active, decisionReason cleared.</li>
     *   <li>From APPROVED+inactive → APPROVED+active, decisionReason cleared.</li>
     *   <li>From APPROVED+active → label/sortOrder edit only (no-op if neither provided).</li>
     * </ul>
     */
    @Transactional
    public AdminBusinessCategoryItem approve(String code, String overrideLabel, Integer overrideSortOrder,
                                             String adminId, String adminRole) {
        RefBusinessCategory cat = categoryRepository.findById(Objects.requireNonNull(code, "code"))
                .orElseThrow(() -> new IllegalArgumentException("Unknown business category: " + code));

        BusinessCategoryStatus previousStatus = cat.getStatus();
        boolean wasActive = Boolean.TRUE.equals(cat.getActive());
        String previousLabel = cat.getLabel();
        Integer previousSortOrder = cat.getSortOrder();

        boolean labelChanged = false;
        if (overrideLabel != null && !overrideLabel.isBlank()) {
            String trimmed = overrideLabel.trim();
            if (!Objects.equals(trimmed, previousLabel)) {
                cat.setLabel(trimmed);
                labelChanged = true;
            }
        }
        boolean sortOrderChanged = false;
        if (overrideSortOrder != null && !Objects.equals(overrideSortOrder, previousSortOrder)) {
            cat.setSortOrder(overrideSortOrder);
            sortOrderChanged = true;
        }

        boolean statusChanged = previousStatus != BusinessCategoryStatus.APPROVED;
        boolean reactivated = previousStatus == BusinessCategoryStatus.APPROVED && !wasActive;

        cat.setStatus(BusinessCategoryStatus.APPROVED);
        cat.setActive(true);
        cat.setDecisionReason(null);

        boolean anythingChanged = statusChanged || reactivated || labelChanged || sortOrderChanged;
        if (anythingChanged) {
            cat.setDecidedByAdminId(adminId);
            cat.setDecidedAt(Instant.now());
        }
        categoryRepository.save(cat);

        if (!anythingChanged) {
            // Pure idempotent: nothing to audit.
            return toItem(cat);
        }

        String action;
        if (statusChanged) {
            action = previousStatus == BusinessCategoryStatus.REJECTED
                    ? "INDUSTRY_SUGGESTION_REINSTATED"
                    : "INDUSTRY_SUGGESTION_APPROVED";
        } else if (reactivated) {
            action = "INDUSTRY_SUGGESTION_REACTIVATED";
        } else {
            action = "INDUSTRY_SUGGESTION_EDITED";
        }

        Map<String, Object> after = new HashMap<>();
        after.put("code", code);
        after.put("label", cat.getLabel());
        after.put("status", cat.getStatus().name());
        after.put("active", cat.getActive());
        if (statusChanged) after.put("previousStatus", previousStatus.name());
        if (labelChanged) after.put("previousLabel", previousLabel);
        if (sortOrderChanged) after.put("previousSortOrder", previousSortOrder);
        OnboardingAuditLog audit = OnboardingAuditLog.builder()
                .tenantId(cat.getSubmittedByTenantId() == null ? "PLATFORM" : cat.getSubmittedByTenantId())
                .action(action)
                .actorId(adminId)
                .actorRole(adminRole != null ? adminRole : "ADMIN")
                .afterState(after)
                .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        log.info("Industry {}: code={}, by adminId={}", action, code, adminId);
        return toItem(cat);
    }

    /**
     * Reject / revoke. Allowed from any state for tenant-submitted rows. Forbidden for
     * system-seeded rows ({@code submittedByTenantId == null}) — those must be hidden via
     * {@link #deactivate} instead, so that tenants who previously selected the seeded category
     * don't suddenly see a "rejected" banner on Step 1.
     */
    @Transactional
    public AdminBusinessCategoryItem reject(String code, String reason, String adminId, String adminRole) {
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("Rejection reason is required (min 5 chars).");
        }
        RefBusinessCategory cat = categoryRepository.findById(Objects.requireNonNull(code, "code"))
                .orElseThrow(() -> new IllegalArgumentException("Unknown business category: " + code));

        if (cat.getSubmittedByTenantId() == null) {
            throw new IllegalStateException(
                    "System-seeded categories cannot be rejected. Use deactivate to hide it instead.");
        }

        BusinessCategoryStatus previousStatus = cat.getStatus();
        boolean wasApproved = previousStatus == BusinessCategoryStatus.APPROVED;

        if (previousStatus == BusinessCategoryStatus.REJECTED
                && Objects.equals(reason.trim(), cat.getDecisionReason())) {
            // Idempotent.
            return toItem(cat);
        }

        cat.setStatus(BusinessCategoryStatus.REJECTED);
        // Hidden from public dropdown by the metadata filter (status=APPROVED AND active=true);
        // also flip active=false to make the inactive state explicit for any future query.
        cat.setActive(false);
        cat.setDecisionReason(reason.trim());
        cat.setDecidedByAdminId(adminId);
        cat.setDecidedAt(Instant.now());
        categoryRepository.save(cat);

        Map<String, Object> after = new HashMap<>();
        after.put("code", code);
        after.put("status", cat.getStatus().name());
        after.put("reason", cat.getDecisionReason());
        after.put("previousStatus", previousStatus.name());

        String action = wasApproved ? "INDUSTRY_SUGGESTION_REVOKED" : "INDUSTRY_SUGGESTION_REJECTED";
        OnboardingAuditLog audit = OnboardingAuditLog.builder()
                .tenantId(cat.getSubmittedByTenantId() == null ? "PLATFORM" : cat.getSubmittedByTenantId())
                .action(action)
                .actorId(adminId)
                .actorRole(adminRole != null ? adminRole : "ADMIN")
                .afterState(after)
                .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        log.info("Industry {}: code={}, by adminId={}", action, code, adminId);
        return toItem(cat);
    }

    /**
     * Hide an APPROVED row from the public dropdown without rejecting it. Tenants who already
     * selected this category keep their selection (and don't see any "rejected" banner) but
     * new tenants no longer see it as an option.
     */
    @Transactional
    public AdminBusinessCategoryItem deactivate(String code, String reason, String adminId, String adminRole) {
        RefBusinessCategory cat = categoryRepository.findById(Objects.requireNonNull(code, "code"))
                .orElseThrow(() -> new IllegalArgumentException("Unknown business category: " + code));

        if (cat.getStatus() != BusinessCategoryStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED categories can be deactivated. Current status: " + cat.getStatus());
        }
        if (Boolean.FALSE.equals(cat.getActive())) {
            // Idempotent.
            return toItem(cat);
        }

        cat.setActive(false);
        if (reason != null && !reason.isBlank()) {
            cat.setDecisionReason(reason.trim());
        }
        cat.setDecidedByAdminId(adminId);
        cat.setDecidedAt(Instant.now());
        categoryRepository.save(cat);

        Map<String, Object> after = new HashMap<>();
        after.put("code", code);
        after.put("status", cat.getStatus().name());
        after.put("active", cat.getActive());
        if (cat.getDecisionReason() != null) after.put("reason", cat.getDecisionReason());
        OnboardingAuditLog audit = OnboardingAuditLog.builder()
                .tenantId(cat.getSubmittedByTenantId() == null ? "PLATFORM" : cat.getSubmittedByTenantId())
                .action("INDUSTRY_SUGGESTION_DEACTIVATED")
                .actorId(adminId)
                .actorRole(adminRole != null ? adminRole : "ADMIN")
                .afterState(after)
                .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        log.info("Industry DEACTIVATED: code={}, by adminId={}", code, adminId);
        return toItem(cat);
    }

    /**
     * Re-show a previously deactivated APPROVED row in the public dropdown. To bring back a
     * REJECTED row, use {@link #approve} instead.
     */
    @Transactional
    public AdminBusinessCategoryItem reactivate(String code, String adminId, String adminRole) {
        RefBusinessCategory cat = categoryRepository.findById(Objects.requireNonNull(code, "code"))
                .orElseThrow(() -> new IllegalArgumentException("Unknown business category: " + code));

        if (cat.getStatus() != BusinessCategoryStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED categories can be reactivated. To bring back a rejected entry, approve it.");
        }
        if (Boolean.TRUE.equals(cat.getActive())) {
            // Idempotent.
            return toItem(cat);
        }

        cat.setActive(true);
        cat.setDecisionReason(null);
        cat.setDecidedByAdminId(adminId);
        cat.setDecidedAt(Instant.now());
        categoryRepository.save(cat);

        Map<String, Object> after = new HashMap<>();
        after.put("code", code);
        after.put("status", cat.getStatus().name());
        after.put("active", cat.getActive());
        OnboardingAuditLog audit = OnboardingAuditLog.builder()
                .tenantId(cat.getSubmittedByTenantId() == null ? "PLATFORM" : cat.getSubmittedByTenantId())
                .action("INDUSTRY_SUGGESTION_REACTIVATED")
                .actorId(adminId)
                .actorRole(adminRole != null ? adminRole : "ADMIN")
                .afterState(after)
                .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        log.info("Industry REACTIVATED: code={}, by adminId={}", code, adminId);
        return toItem(cat);
    }

    private AdminBusinessCategoryItem toItem(RefBusinessCategory c) {
        return toItem(c, null, null);
    }

    private AdminBusinessCategoryItem toItem(RefBusinessCategory c,
                                             String submittedByCompany,
                                             String submittedByEmail) {
        return AdminBusinessCategoryItem.builder()
                .code(c.getCode())
                .label(c.getLabel())
                .submittedLabel(c.getSubmittedLabel())
                .sortOrder(c.getSortOrder())
                .active(Boolean.TRUE.equals(c.getActive()))
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .submittedByTenantId(c.getSubmittedByTenantId())
                .submittedByCompanyName(submittedByCompany)
                .submittedByEmail(submittedByEmail)
                .decisionReason(c.getDecisionReason())
                .decidedByAdminId(c.getDecidedByAdminId())
                .decidedAt(c.getDecidedAt())
                .build();
    }
}

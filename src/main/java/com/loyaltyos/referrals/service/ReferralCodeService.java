package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.entity.ReferralAuditLog;
import com.loyaltyos.referrals.entity.ReferralCode;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.enums.ReferralCodeStatus;
import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.repository.ReferralCodeRepository;
import com.loyaltyos.referrals.support.ReferralCodeGenerator;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralCodeService {

    private static final int CODE_LENGTH = 10;
    private static final int MAX_GENERATION_ATTEMPTS = 8;

    private final ReferralCodeRepository codeRepository;
    private final ReferralProgrammeService programmeService;
    private final ReferralEligibilityService eligibilityService;
    private final ReferralAuditService auditService;

    public ReferralCodeService(
        ReferralCodeRepository codeRepository,
        ReferralProgrammeService programmeService,
        ReferralEligibilityService eligibilityService,
        ReferralAuditService auditService
    ) {
        this.codeRepository = Objects.requireNonNull(codeRepository, "codeRepository");
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.eligibilityService = Objects.requireNonNull(eligibilityService, "eligibilityService");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
    }

    @Transactional
    public ReferralCode getOrGenerateCode(String tenantId, String customerId, String programmeUid) {
        String programme = normalizeProgramme(programmeUid);
        ReferralProgramme active = programmeService.getActiveOrNull(tenantId, programme);
        if (active != null) {
            eligibilityService.assertReferrerEligible(
                tenantId, programme, customerId, programmeService.readConfig(active)
            );
        }
        return codeRepository
            .findByTenantIdAndProgrammeUidAndCustomerIdAndStatus(
                tenantId, programme, customerId, ReferralCodeStatus.ACTIVE
            )
            .orElseGet(() -> createNewCode(tenantId, customerId, programme));
    }

    @Transactional(readOnly = true)
    public ReferralCode lookupActiveCode(String tenantId, String code) {
        return codeRepository.findByTenantIdAndCodeAndStatus(tenantId, code.trim().toUpperCase(), ReferralCodeStatus.ACTIVE)
            .orElseThrow(() -> new ReferralException("REFERRAL_CODE_NOT_FOUND", "Referral code not found or inactive"));
    }

    private ReferralCode createNewCode(String tenantId, String customerId, String programmeUid) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = ReferralCodeGenerator.generate(CODE_LENGTH).toUpperCase();
            if (codeRepository.existsByTenantIdAndCode(tenantId, candidate)) {
                continue;
            }
            ReferralCode row = new ReferralCode();
            row.setTenantId(tenantId);
            row.setProgrammeUid(programmeUid);
            row.setCustomerId(customerId);
            row.setCode(candidate);
            row.setStatus(ReferralCodeStatus.ACTIVE);
            row.setCreatedAt(Instant.now());
            try {
                ReferralCode saved = codeRepository.save(row);
                auditService.log(
                    tenantId,
                    programmeUid,
                    null,
                    ReferralAuditLog.Action.CODE_CREATED,
                    ReferralAuditLog.ActorType.SYSTEM,
                    customerId,
                    Map.of("code", candidate)
                );
                return saved;
            } catch (DataIntegrityViolationException ex) {
                // concurrent create for same customer or code collision — retry fetch
                return codeRepository
                    .findByTenantIdAndProgrammeUidAndCustomerIdAndStatus(
                        tenantId, programmeUid, customerId, ReferralCodeStatus.ACTIVE
                    )
                    .orElseThrow(() -> ex);
            }
        }
        throw new ReferralException("CODE_GENERATION_FAILED", "Unable to generate unique referral code");
    }

    private static String normalizeProgramme(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }
}

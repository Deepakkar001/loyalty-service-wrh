package com.loyaltyos.referrals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.dto.ReferralProgrammeUpsertRequest;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.enums.ReferralProgrammeStatus;
import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.repository.ReferralProgrammeRepository;
import com.loyaltyos.referrals.support.ReferralConfigNormalizer;
import com.loyaltyos.referrals.support.ReferralConfigSupport;
import com.loyaltyos.referrals.support.ReferralConfigValidator;
import com.loyaltyos.referrals.support.ReferralProgrammeEventSchemaCatalog;
import com.loyaltyos.referrals.support.ReferralProgrammeSchemaLoader;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralProgrammeService {

    private final ReferralProgrammeRepository programmeRepository;
    private final ObjectMapper objectMapper;
    private final ReferralProgrammeSchemaLoader programmeSchemaLoader;

    public ReferralProgrammeService(
        ReferralProgrammeRepository programmeRepository,
        ObjectMapper objectMapper,
        ReferralProgrammeSchemaLoader programmeSchemaLoader
    ) {
        this.programmeRepository = Objects.requireNonNull(programmeRepository, "programmeRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.programmeSchemaLoader = Objects.requireNonNull(programmeSchemaLoader, "programmeSchemaLoader");
    }

    @Transactional(readOnly = true)
    public Optional<ReferralProgramme> find(String tenantId, String programmeUid) {
        String p = normalizeProgramme(programmeUid);
        return programmeRepository.findByTenantIdAndProgrammeUid(tenantId, p);
    }

    @Transactional(readOnly = true)
    public ReferralProgramme getRequired(String tenantId, String programmeUid) {
        return find(tenantId, programmeUid)
            .orElseThrow(() -> new ReferralException("REFERRAL_PROGRAMME_NOT_FOUND", "Referral programme not configured"));
    }

    @Transactional(readOnly = true)
    public ReferralProgramme getActiveOrNull(String tenantId, String programmeUid) {
        String p = normalizeProgramme(programmeUid);
        return programmeRepository.findByTenantIdAndProgrammeUid(tenantId, p)
            .filter(this::isActiveNow)
            .orElse(null);
    }

    @Transactional
    public ReferralProgramme upsert(String tenantId, ReferralProgrammeUpsertRequest request) {
        Objects.requireNonNull(request, "request");
        String programmeUid = normalizeProgramme(request.getProgrammeUid());
        ReferralProgrammeConfig config = ReferralConfigNormalizer.normalize(request.getConfig());
        ReferralProgrammeEventSchemaCatalog programmeSchema = programmeSchemaLoader.loadForProgramme(tenantId, programmeUid);
        ReferralConfigValidator.validate(config, programmeSchema);

        ReferralProgramme row = programmeRepository.findByTenantIdAndProgrammeUid(tenantId, programmeUid)
            .orElseGet(ReferralProgramme::new);

        Instant now = Instant.now();
        if (row.getId() == null) {
            row.setTenantId(tenantId);
            row.setProgrammeUid(programmeUid);
            row.setCreatedAt(now);
        }
        row.setName(request.getName() != null ? request.getName().trim() : "Referral programme");
        row.setDescription(request.getDescription());
        row.setStatus(request.getStatus() != null ? request.getStatus() : ReferralProgrammeStatus.ACTIVE);
        row.setValidFrom(request.getValidFrom());
        row.setValidUntil(request.getValidUntil());
        row.setConfigJson(ReferralConfigSupport.toJson(objectMapper, config));
        row.setMaxReferralsPerCustomer(
            request.getMaxReferralsPerCustomer() != null ? request.getMaxReferralsPerCustomer() : 50
        );
        row.setUpdatedAt(now);
        return programmeRepository.save(row);
    }

    public ReferralProgrammeConfig readConfig(ReferralProgramme programme) {
        return ReferralConfigNormalizer.normalize(
            ReferralConfigSupport.parseConfig(objectMapper, programme.getConfigJson())
        );
    }

    public boolean isActiveNow(ReferralProgramme programme) {
        if (programme.getStatus() != ReferralProgrammeStatus.ACTIVE) {
            return false;
        }
        Instant now = Instant.now();
        if (programme.getValidFrom() != null && now.isBefore(programme.getValidFrom())) {
            return false;
        }
        return programme.getValidUntil() == null || !now.isAfter(programme.getValidUntil());
    }

    private static String normalizeProgramme(String programmeUid) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return "default";
        }
        return programmeUid.trim();
    }
}

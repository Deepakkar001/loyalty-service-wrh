package com.loyaltyos.referrals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.entity.ReferralAuditLog;
import com.loyaltyos.referrals.repository.ReferralAuditLogRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralAuditService {

    private final ReferralAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public ReferralAuditService(ReferralAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
        String tenantId,
        String programmeUid,
        String referralUid,
        ReferralAuditLog.Action action,
        ReferralAuditLog.ActorType actorType,
        String actorId,
        Map<String, Object> metadata
    ) {
        ReferralAuditLog row = new ReferralAuditLog();
        row.setTenantId(tenantId);
        row.setProgrammeUid(programmeUid != null ? programmeUid : "default");
        row.setReferralUid(referralUid);
        row.setAction(action);
        row.setActorType(actorType != null ? actorType : ReferralAuditLog.ActorType.SYSTEM);
        row.setActorId(actorId);
        if (metadata != null && !metadata.isEmpty()) {
            try {
                row.setMetadataJson(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                row.setMetadataJson("{\"error\":\"metadata serialization failed\"}");
            }
        }
        row.setCreatedAt(Instant.now());
        auditLogRepository.save(row);
    }
}

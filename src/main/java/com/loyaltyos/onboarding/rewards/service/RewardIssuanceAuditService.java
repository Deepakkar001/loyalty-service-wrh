package com.loyaltyos.onboarding.rewards.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.rewards.config.RewardEngineProperties;
import com.loyaltyos.onboarding.rewards.entity.RewardIssuanceAudit;
import com.loyaltyos.onboarding.rewards.exception.RewardIssuanceAuditWriteException;
import com.loyaltyos.onboarding.rewards.repository.RewardIssuanceAuditRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardIssuanceAuditService {

    private static final Logger log = LoggerFactory.getLogger(RewardIssuanceAuditService.class);

    private final RewardIssuanceAuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    private final RewardEngineProperties rewardEngineProperties;

    public RewardIssuanceAuditService(
        RewardIssuanceAuditRepository auditRepository,
        ObjectMapper objectMapper,
        RewardEngineProperties rewardEngineProperties
    ) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.rewardEngineProperties = Objects.requireNonNull(rewardEngineProperties, "rewardEngineProperties");
    }

    /**
     * Joins the caller's transaction — rolls back with issuance on failure.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSuccess(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        BigDecimal totalPoints,
        int ruleCount,
        List<Long> ledgerIds,
        int durationMs
    ) {
        if (!rewardEngineProperties.isIssuanceAuditEnabled()) {
            return;
        }
        try {
            RewardIssuanceAudit row = new RewardIssuanceAudit();
            row.setTenantId(tenantId);
            row.setProgrammeUid(programmeUid);
            row.setCustomerId(customerId);
            row.setEventId(eventId);
            row.setTotalPointsAwarded(totalPoints);
            row.setRuleCount(ruleCount);
            row.setStatus(RewardIssuanceAudit.STATUS_SUCCESS);
            row.setErrorMessage(null);
            row.setLedgerIdsJson(objectMapper.writeValueAsString(ledgerIds));
            row.setDurationMs(durationMs);
            auditRepository.save(row);
        } catch (JsonProcessingException e) {
            log.error("reward_issuance_audit JSON encode failed: {}", e.getMessage(), e);
            if (rewardEngineProperties.isIssuanceAuditStrict()) {
                throw new RewardIssuanceAuditWriteException("Unable to encode SUCCESS issuance audit ledger_ids", e);
            }
        } catch (Exception e) {
            log.error("reward_issuance_audit save failed: {}", e.getMessage(), e);
            if (rewardEngineProperties.isIssuanceAuditStrict()) {
                throw new RewardIssuanceAuditWriteException("Unable to persist SUCCESS issuance audit row", e);
            }
        }
    }

    /**
     * Commits independently so a rollback of issuance still leaves an audit breadcrumb.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
        String tenantId,
        String programmeUid,
        String customerId,
        String eventId,
        String message,
        int durationMs
    ) {
        if (!rewardEngineProperties.isFailureAuditEnabled()) {
            return;
        }
        try {
            RewardIssuanceAudit row = new RewardIssuanceAudit();
            row.setTenantId(tenantId);
            row.setProgrammeUid(programmeUid);
            row.setCustomerId(customerId);
            row.setEventId(eventId);
            row.setTotalPointsAwarded(BigDecimal.ZERO);
            row.setRuleCount(0);
            row.setStatus(RewardIssuanceAudit.STATUS_FAILED);
            row.setErrorMessage(message == null ? "" : message.substring(0, Math.min(message.length(), 65000)));
            row.setLedgerIdsJson("[]");
            row.setDurationMs(durationMs);
            auditRepository.save(row);
        } catch (Exception e) {
            log.warn("reward_issuance_audit failure row save failed: {}", e.getMessage());
        }
    }
}

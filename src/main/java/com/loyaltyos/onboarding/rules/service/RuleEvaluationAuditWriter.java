package com.loyaltyos.onboarding.rules.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.rules.entity.RuleEvaluationAudit;
import com.loyaltyos.onboarding.rules.repository.RuleEvaluationAuditRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleEvaluationAuditWriter {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluationAuditWriter.class);

    private final RuleEvaluationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public RuleEvaluationAuditWriter(RuleEvaluationAuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(String tenantId, String programmeUid, String customerId, String eventId, boolean success, JsonNode trace) {
        try {
            String json = objectMapper.writeValueAsString(trace);
            RuleEvaluationAudit audit = RuleEvaluationAudit.builder()
                .tenantId(tenantId)
                .programmeUid(programmeUid)
                .customerId(customerId)
                .eventId(eventId)
                .success(success)
                .traceJson(json)
                .build();
            auditRepository.save(Objects.requireNonNull(audit, "audit"));
        } catch (Exception e) {
            log.warn("Failed to persist rule evaluation audit: {}", e.getMessage());
        }
    }
}

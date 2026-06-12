package com.loyaltyos.merchants.service.statemachine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantOnboardingAudit;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.exception.InvalidMerchantStateTransitionException;
import com.loyaltyos.merchants.repository.MerchantOnboardingAuditRepository;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MerchantOnboardingStateMachine {

    private static final Logger log = LoggerFactory.getLogger(MerchantOnboardingStateMachine.class);

    private final MerchantOnboardingAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public MerchantOnboardingStateMachine(
        MerchantOnboardingAuditRepository auditRepository,
        ObjectMapper objectMapper
    ) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public void transition(
        Merchant merchant,
        MerchantOnboardingStage next,
        String actorEmail,
        String action,
        String reason,
        Map<String, Object> metadata
    ) {
        MerchantOnboardingStage current = merchant.getOnboardingStage();
        if (!current.canTransitionTo(next)) {
            throw new InvalidMerchantStateTransitionException(current, next);
        }

        log.info("Merchant [{}] onboarding: {} → {}", merchant.getMerchantUid(), current, next);

        MerchantOnboardingAudit audit = new MerchantOnboardingAudit();
        audit.setAuditUid(UUID.randomUUID().toString());
        audit.setTenantId(merchant.getTenantId());
        audit.setMerchantUid(merchant.getMerchantUid());
        audit.setFromStage(current.name());
        audit.setToStage(next.name());
        audit.setActorEmail(actorEmail != null ? actorEmail : "system");
        audit.setAction(action);
        audit.setReason(reason);
        if (metadata != null && !metadata.isEmpty()) {
            try {
                audit.setMetadataJson(objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                audit.setMetadataJson(null);
            }
        }
        auditRepository.save(audit);

        merchant.setOnboardingStage(next);
    }
}

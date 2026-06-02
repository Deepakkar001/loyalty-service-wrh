package com.loyaltyos.voucher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.voucher.entity.VoucherRedemptionAudit;
import com.loyaltyos.voucher.enums.VoucherAuditEventType;
import com.loyaltyos.voucher.repository.VoucherRedemptionAuditRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class VoucherAuditService {

    private static final Logger log = LoggerFactory.getLogger(VoucherAuditService.class);

    private final VoucherRedemptionAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public VoucherAuditService(
        VoucherRedemptionAuditRepository auditRepository,
        ObjectMapper objectMapper
    ) {
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public void logEvent(
        String tenantId,
        VoucherAuditEventType eventType,
        String inventoryUid,
        String customerId,
        String redemptionId,
        Long ledgerId,
        String reason,
        Map<String, Object> metadata
    ) {
        try {
            VoucherRedemptionAudit audit = new VoucherRedemptionAudit();
            audit.setAuditUid(UUID.randomUUID().toString());
            audit.setTenantId(tenantId);
            audit.setInventoryUid(inventoryUid);
            audit.setRedemptionId(redemptionId != null ? redemptionId : "n/a");
            audit.setLedgerId(ledgerId);
            audit.setCustomerId(customerId != null ? customerId : "n/a");
            audit.setEventType(eventType);
            audit.setActor(currentActor());
            audit.setReason(reason);
            if (metadata != null && !metadata.isEmpty()) {
                audit.setMetadataJson(objectMapper.writeValueAsString(metadata));
            }
            audit.setCreatedAt(Instant.now());
            auditRepository.save(audit);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize voucher audit metadata", e);
        } catch (Exception e) {
            log.error("Failed to log voucher audit event", e);
        }
    }

    private static String currentActor() {
        var context = SecurityContextHolder.getContext();
        if (context != null && context.getAuthentication() != null) {
            return context.getAuthentication().getName();
        }
        return "SYSTEM";
    }
}

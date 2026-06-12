package com.loyaltyos.access.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.access.entity.AccessControlAuditLog;
import com.loyaltyos.access.repository.AccessControlAuditLogRepository;
import com.loyaltyos.onboarding.security.TenantJwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class AccessControlAuditService {

    private final AccessControlAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AccessControlAuditService(
        AccessControlAuditLogRepository auditLogRepository,
        ObjectMapper objectMapper
    ) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public void log(String tenantId, String actorType, String actorId, String action, Map<String, ?> payload) {
        AccessControlAuditLog entry = new AccessControlAuditLog();
        entry.setTenantId(tenantId);
        entry.setActorType(actorType);
        entry.setActorId(actorId);
        entry.setAction(action);
        if (payload != null && !payload.isEmpty()) {
            try {
                entry.setPayloadJson(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException ignored) {
                entry.setPayloadJson("{}");
            }
        }
        auditLogRepository.save(entry);
    }

    public void logFromSecurityContext(String tenantId, String action, Map<String, ?> payload) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = TenantJwt.resolve(authentication);
        if (jwt != null) {
            String tenantUserId = TenantJwt.tenantUserId(jwt);
            String adminUid = TenantJwt.adminUid(jwt);
            if (tenantUserId != null && !tenantUserId.isBlank()) {
                log(tenantId, "TENANT_USER", tenantUserId, action, payload);
                return;
            }
            if (adminUid != null && !adminUid.isBlank()) {
                log(tenantId, "PLATFORM_ADMIN", adminUid, action, payload);
                return;
            }
        }
        log(tenantId, "SYSTEM", tenantId, action, payload);
    }
}

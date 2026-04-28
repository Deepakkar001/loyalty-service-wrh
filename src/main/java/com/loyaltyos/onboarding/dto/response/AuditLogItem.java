package com.loyaltyos.onboarding.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class AuditLogItem {
    private Long id;
    private String tenantId;
    private String action;
    private String actorId;
    private String actorRole;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private Instant createdAt;
}

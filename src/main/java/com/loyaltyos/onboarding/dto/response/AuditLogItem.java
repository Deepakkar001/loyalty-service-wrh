package com.loyaltyos.onboarding.dto.response;

import java.time.Instant;
import java.util.Map;

public class AuditLogItem {
    private Long id;
    private String tenantId;
    private String action;
    private String actorId;
    private String actorRole;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private Instant createdAt;

    public AuditLogItem() {}

    public AuditLogItem(
        Long id,
        String tenantId,
        String action,
        String actorId,
        String actorRole,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.action = action;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String action;
        private String actorId;
        private String actorRole;
        private Map<String, Object> beforeState;
        private Map<String, Object> afterState;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder actorId(String actorId) { this.actorId = actorId; return this; }
        public Builder actorRole(String actorRole) { this.actorRole = actorRole; return this; }
        public Builder beforeState(Map<String, Object> beforeState) { this.beforeState = beforeState; return this; }
        public Builder afterState(Map<String, Object> afterState) { this.afterState = afterState; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public AuditLogItem build() {
            return new AuditLogItem(id, tenantId, action, actorId, actorRole, beforeState, afterState, createdAt);
        }
    }

    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getAction() { return action; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public Map<String, Object> getBeforeState() { return beforeState; }
    public Map<String, Object> getAfterState() { return afterState; }
    public Instant getCreatedAt() { return createdAt; }
}

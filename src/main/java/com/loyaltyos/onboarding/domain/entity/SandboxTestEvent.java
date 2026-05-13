package com.loyaltyos.onboarding.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "sandbox_test_events")
public class SandboxTestEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "programme_uid", nullable = false, length = 64)
    private String programmeUid;

    @Column(name = "transaction_id", nullable = false, length = 128)
    private String transactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload_json", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> requestPayloadJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> responseJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public SandboxTestEvent() {}

    public SandboxTestEvent(
        Long id,
        String tenantId,
        String programmeUid,
        String transactionId,
        Map<String, Object> requestPayloadJson,
        Map<String, Object> responseJson,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.programmeUid = programmeUid;
        this.transactionId = transactionId;
        this.requestPayloadJson = requestPayloadJson;
        this.responseJson = responseJson;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String programmeUid;
        private String transactionId;
        private Map<String, Object> requestPayloadJson;
        private Map<String, Object> responseJson;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder programmeUid(String programmeUid) {
            this.programmeUid = programmeUid;
            return this;
        }

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder requestPayloadJson(Map<String, Object> requestPayloadJson) {
            this.requestPayloadJson = requestPayloadJson;
            return this;
        }

        public Builder responseJson(Map<String, Object> responseJson) {
            this.responseJson = responseJson;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SandboxTestEvent build() {
            return new SandboxTestEvent(
                id,
                tenantId,
                programmeUid,
                transactionId,
                requestPayloadJson,
                responseJson,
                createdAt
            );
        }
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Map<String, Object> getRequestPayloadJson() {
        return requestPayloadJson;
    }

    public Map<String, Object> getResponseJson() {
        return responseJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

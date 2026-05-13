package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.WebhookVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "endpoint_url", nullable = false, length = 2048)
    private String endpointUrl;

    @Column(name = "secret_vault_ref", nullable = false, length = 255)
    private String secretVaultRef;

    @Column(name = "events_subscribed", nullable = false, columnDefinition = "JSON")
    private String eventsSubscribed = "[]";

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private WebhookVerificationStatus verificationStatus = WebhookVerificationStatus.PENDING;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** JPA requires a no-arg constructor. */
    public WebhookSubscription() {}

    public WebhookSubscription(
        Long id,
        String tenantId,
        String endpointUrl,
        String secretVaultRef,
        String eventsSubscribed,
        Boolean active,
        WebhookVerificationStatus verificationStatus,
        Instant lastVerifiedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.endpointUrl = endpointUrl;
        this.secretVaultRef = secretVaultRef;
        this.eventsSubscribed = eventsSubscribed != null ? eventsSubscribed : "[]";
        this.active = active != null ? active : true;
        this.verificationStatus = verificationStatus != null ? verificationStatus : WebhookVerificationStatus.PENDING;
        this.lastVerifiedAt = lastVerifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String endpointUrl;
        private String secretVaultRef;
        private String eventsSubscribed = "[]";
        private Boolean active = true;
        private WebhookVerificationStatus verificationStatus = WebhookVerificationStatus.PENDING;
        private Instant lastVerifiedAt;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder endpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; return this; }
        public Builder secretVaultRef(String secretVaultRef) { this.secretVaultRef = secretVaultRef; return this; }
        public Builder eventsSubscribed(String eventsSubscribed) { this.eventsSubscribed = eventsSubscribed; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }
        public Builder verificationStatus(WebhookVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public Builder lastVerifiedAt(Instant lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public WebhookSubscription build() {
            return new WebhookSubscription(
                id,
                tenantId,
                endpointUrl,
                secretVaultRef,
                eventsSubscribed,
                active,
                verificationStatus,
                lastVerifiedAt,
                createdAt,
                updatedAt
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getSecretVaultRef() { return secretVaultRef; }
    public void setSecretVaultRef(String secretVaultRef) { this.secretVaultRef = secretVaultRef; }
    public String getEventsSubscribed() { return eventsSubscribed; }
    public void setEventsSubscribed(String eventsSubscribed) { this.eventsSubscribed = eventsSubscribed; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public WebhookVerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(WebhookVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(Instant lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}


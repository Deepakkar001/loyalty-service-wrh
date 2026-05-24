package com.loyaltyos.onboarding.entity;

import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tenant_api_keys")
public class TenantApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "key_uid", nullable = false, unique = true, length = 128)
    private String keyUid;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "signing_secret_hash", nullable = false, length = 64)
    private String signingSecretHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 50)
    private ApiKeyEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "signing_secret_encrypted", columnDefinition = "TEXT")
    private String signingSecretEncrypted;

    @Column(name = "rate_limit_requests")
    private Integer rateLimitRequests = 1000;

    @Column(name = "rate_limit_window_minutes")
    private Integer rateLimitWindowMinutes = 60;

    @Column(name = "ip_whitelist", columnDefinition = "JSON")
    private String ipWhitelist;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "last_secret_revealed_at")
    private Instant lastSecretRevealedAt;

    /** JPA requires a no-arg constructor. */
    public TenantApiKey() {}

    public TenantApiKey(
        Long id,
        String tenantId,
        String keyUid,
        String keyPrefix,
        String keyHash,
        String signingSecretHash,
        ApiKeyEnvironment environment,
        ApiKeyStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.keyUid = keyUid;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.signingSecretHash = signingSecretHash;
        this.environment = environment;
        this.status = status != null ? status : ApiKeyStatus.ACTIVE;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.lastUsedAt = lastUsedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String keyUid;
        private String keyPrefix;
        private String keyHash;
        private String signingSecretHash;
        private ApiKeyEnvironment environment;
        private ApiKeyStatus status = ApiKeyStatus.ACTIVE;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant revokedAt;
        private Instant lastUsedAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder keyUid(String keyUid) { this.keyUid = keyUid; return this; }
        public Builder keyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; return this; }
        public Builder keyHash(String keyHash) { this.keyHash = keyHash; return this; }
        public Builder signingSecretHash(String signingSecretHash) { this.signingSecretHash = signingSecretHash; return this; }
        public Builder environment(ApiKeyEnvironment environment) { this.environment = environment; return this; }
        public Builder status(ApiKeyStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder revokedAt(Instant revokedAt) { this.revokedAt = revokedAt; return this; }
        public Builder lastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; return this; }
        private String signingSecretEncrypted;
        private Integer rateLimitRequests = 1000;
        private Integer rateLimitWindowMinutes = 60;
        private String ipWhitelist;
        private String name;
        private String description;
        private Instant lastSecretRevealedAt;

        public Builder signingSecretEncrypted(String signingSecretEncrypted) {
            this.signingSecretEncrypted = signingSecretEncrypted;
            return this;
        }
        public Builder rateLimitRequests(Integer rateLimitRequests) {
            this.rateLimitRequests = rateLimitRequests;
            return this;
        }
        public Builder rateLimitWindowMinutes(Integer rateLimitWindowMinutes) {
            this.rateLimitWindowMinutes = rateLimitWindowMinutes;
            return this;
        }
        public Builder ipWhitelist(String ipWhitelist) { this.ipWhitelist = ipWhitelist; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder lastSecretRevealedAt(Instant lastSecretRevealedAt) {
            this.lastSecretRevealedAt = lastSecretRevealedAt;
            return this;
        }

        public TenantApiKey build() {
            TenantApiKey key = new TenantApiKey(
                id,
                tenantId,
                keyUid,
                keyPrefix,
                keyHash,
                signingSecretHash,
                environment,
                status,
                createdAt,
                expiresAt,
                revokedAt,
                lastUsedAt
            );
            key.signingSecretEncrypted = signingSecretEncrypted;
            key.rateLimitRequests = rateLimitRequests;
            key.rateLimitWindowMinutes = rateLimitWindowMinutes;
            key.ipWhitelist = ipWhitelist;
            key.name = name;
            key.description = description;
            key.lastSecretRevealedAt = lastSecretRevealedAt;
            return key;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getKeyUid() { return keyUid; }
    public void setKeyUid(String keyUid) { this.keyUid = keyUid; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public String getSigningSecretHash() { return signingSecretHash; }
    public void setSigningSecretHash(String signingSecretHash) { this.signingSecretHash = signingSecretHash; }
    public ApiKeyEnvironment getEnvironment() { return environment; }
    public void setEnvironment(ApiKeyEnvironment environment) { this.environment = environment; }
    public ApiKeyStatus getStatus() { return status; }
    public void setStatus(ApiKeyStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public String getSigningSecretEncrypted() { return signingSecretEncrypted; }
    public void setSigningSecretEncrypted(String signingSecretEncrypted) {
        this.signingSecretEncrypted = signingSecretEncrypted;
    }
    public Integer getRateLimitRequests() {
        return rateLimitRequests != null ? rateLimitRequests : 1000;
    }
    public void setRateLimitRequests(Integer rateLimitRequests) { this.rateLimitRequests = rateLimitRequests; }
    public Integer getRateLimitWindowMinutes() {
        return rateLimitWindowMinutes != null ? rateLimitWindowMinutes : 60;
    }
    public void setRateLimitWindowMinutes(Integer rateLimitWindowMinutes) {
        this.rateLimitWindowMinutes = rateLimitWindowMinutes;
    }
    public String getIpWhitelist() { return ipWhitelist; }
    public void setIpWhitelist(String ipWhitelist) { this.ipWhitelist = ipWhitelist; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getLastSecretRevealedAt() { return lastSecretRevealedAt; }
    public void setLastSecretRevealedAt(Instant lastSecretRevealedAt) {
        this.lastSecretRevealedAt = lastSecretRevealedAt;
    }

    public boolean hasRetrievableSecret() {
        return signingSecretEncrypted != null && !signingSecretEncrypted.isBlank();
    }
}


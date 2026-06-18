package com.loyaltyos.merchants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "merchant_credentials",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_credential_uid", columnNames = "credential_uid"),
        @UniqueConstraint(name = "uk_username_tenant", columnNames = {"username", "tenant_id"})
    },
    indexes = @Index(name = "idx_merchant", columnList = "tenant_id, merchant_uid")
)
public class MerchantCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credential_uid", nullable = false, length = 128)
    private String credentialUid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "merchant_uid", nullable = false, length = 128)
    private String merchantUid;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "invite_token_hash", length = 128)
    private String inviteTokenHash;

    @Column(name = "invite_token_expires_at")
    private Instant inviteTokenExpiresAt;

    @Column(name = "invite_accepted_at")
    private Instant inviteAcceptedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "login_attempt_count", nullable = false)
    private int loginAttemptCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MerchantCredentials() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCredentialUid() { return credentialUid; }
    public void setCredentialUid(String credentialUid) { this.credentialUid = credentialUid; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getMerchantUid() { return merchantUid; }
    public void setMerchantUid(String merchantUid) { this.merchantUid = merchantUid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public String getInviteTokenHash() { return inviteTokenHash; }
    public void setInviteTokenHash(String inviteTokenHash) { this.inviteTokenHash = inviteTokenHash; }

    public Instant getInviteTokenExpiresAt() { return inviteTokenExpiresAt; }
    public void setInviteTokenExpiresAt(Instant inviteTokenExpiresAt) { this.inviteTokenExpiresAt = inviteTokenExpiresAt; }

    public Instant getInviteAcceptedAt() { return inviteAcceptedAt; }
    public void setInviteAcceptedAt(Instant inviteAcceptedAt) { this.inviteAcceptedAt = inviteAcceptedAt; }

    public boolean isInvitePending() {
        return inviteTokenHash != null && inviteAcceptedAt == null;
    }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public int getLoginAttemptCount() { return loginAttemptCount; }
    public void setLoginAttemptCount(int loginAttemptCount) { this.loginAttemptCount = loginAttemptCount; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }
}

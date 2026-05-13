package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.ContactRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tenant_contacts")
public class TenantContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "contact_uid", nullable = false, unique = true, length = 128)
    private String contactUid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "designation", length = 255)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ContactRole role;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    public TenantContact() {}

    public TenantContact(
        Long id,
        String tenantId,
        String contactUid,
        String name,
        String email,
        String phone,
        String designation,
        ContactRole role,
        Boolean isPrimary,
        Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.contactUid = contactUid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.designation = designation;
        this.role = role;
        this.isPrimary = isPrimary != null ? isPrimary : false;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String contactUid;
        private String name;
        private String email;
        private String phone;
        private String designation;
        private ContactRole role;
        private Boolean isPrimary = false;
        private Instant createdAt;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder contactUid(String contactUid) { this.contactUid = contactUid; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder designation(String designation) { this.designation = designation; return this; }
        public Builder role(ContactRole role) { this.role = role; return this; }
        public Builder isPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public TenantContact build() {
            return new TenantContact(
                id,
                tenantId,
                contactUid,
                name,
                email,
                phone,
                designation,
                role,
                isPrimary,
                createdAt
            );
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getContactUid() { return contactUid; }
    public void setContactUid(String contactUid) { this.contactUid = contactUid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public ContactRole getRole() { return role; }
    public void setRole(ContactRole role) { this.role = role; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean primary) { isPrimary = primary; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}


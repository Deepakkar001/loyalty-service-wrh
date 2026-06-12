package com.loyaltyos.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_role_template")
public class TenantRoleTemplate {

    @Id
    @Column(name = "template_key", length = 64)
    private String templateKey;

    @Column(name = "role_name", nullable = false, length = 128)
    private String roleName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "permission_keys", columnDefinition = "JSON")
    private String permissionKeys;

    public TenantRoleTemplate() {}

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPermissionKeys() { return permissionKeys; }
    public void setPermissionKeys(String permissionKeys) { this.permissionKeys = permissionKeys; }
}

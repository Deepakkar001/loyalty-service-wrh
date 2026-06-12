package com.loyaltyos.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "access_module_action",
    uniqueConstraints = @UniqueConstraint(columnNames = {"module_key", "action_key"})
)
public class AccessModuleAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_key", nullable = false, length = 64)
    private String moduleKey;

    @Column(name = "action_key", nullable = false, length = 32)
    private String actionKey;

    @Column(name = "permission_key", nullable = false, unique = true, length = 128)
    private String permissionKey;

    @Column(name = "is_assignable", nullable = false)
    private boolean assignable = true;

    public AccessModuleAction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
    public boolean isAssignable() { return assignable; }
    public void setAssignable(boolean assignable) { this.assignable = assignable; }
}

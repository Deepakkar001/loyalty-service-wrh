package com.loyaltyos.access.dto;

import java.util.List;

public class AssignPrivilegesRequest {
    private String roleId;
    private String userId;
    private List<String> permissionKeys;

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<String> getPermissionKeys() { return permissionKeys; }
    public void setPermissionKeys(List<String> permissionKeys) { this.permissionKeys = permissionKeys; }
}

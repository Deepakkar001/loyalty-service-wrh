package com.loyaltyos.access.dto;

import jakarta.validation.constraints.NotBlank;

public class ReassignUserRoleRequest {

    @NotBlank
    private String roleId;

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
}

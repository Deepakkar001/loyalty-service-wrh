package com.loyaltyos.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateRoleRequest {

    @NotBlank
    @Size(max = 128)
    private String roleName;

    @Size(max = 512)
    private String description;

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

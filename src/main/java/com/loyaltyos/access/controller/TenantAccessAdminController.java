package com.loyaltyos.access.controller;

import com.loyaltyos.access.dto.AssignPrivilegesRequest;
import com.loyaltyos.access.dto.CreateRoleRequest;
import com.loyaltyos.access.dto.InviteUserRequest;
import com.loyaltyos.access.dto.LoadPrivilegesRequest;
import com.loyaltyos.access.dto.PrivilegeMatrixResponse;
import com.loyaltyos.access.dto.RoleResponse;
import com.loyaltyos.access.dto.RoleTemplateResponse;
import com.loyaltyos.access.dto.TenantUserResponse;
import com.loyaltyos.access.service.PrivilegeAssignmentService;
import com.loyaltyos.access.service.TenantRoleService;
import com.loyaltyos.access.service.TenantUserAdminService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.loyaltyos.access.dto.ReassignUserRoleRequest;
import com.loyaltyos.access.dto.UpdateRoleRequest;
import com.loyaltyos.access.dto.UpdateUserRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/access")
@Tag(name = "Tenant Access Admin")
@SecurityRequirement(name = "bearerAuth")
public class TenantAccessAdminController {

    private final TenantRoleService roleService;
    private final TenantUserAdminService userAdminService;
    private final PrivilegeAssignmentService privilegeService;

    public TenantAccessAdminController(
        TenantRoleService roleService,
        TenantUserAdminService userAdminService,
        PrivilegeAssignmentService privilegeService
    ) {
        this.roleService = roleService;
        this.userAdminService = userAdminService;
        this.privilegeService = privilegeService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasPermission(null, 'team_admin.view')")
    @Operation(summary = "List tenant roles")
    public List<RoleResponse> listRoles(@AuthenticationPrincipal Jwt jwt) {
        return roleService.listRoles(TenantJwt.requireTenantId(jwt));
    }

    @GetMapping("/role-templates")
    @PreAuthorize("hasPermission(null, 'team_admin.view')")
    @Operation(summary = "List cloneable role templates")
    public List<RoleTemplateResponse> listTemplates() {
        return roleService.listTemplates();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasPermission(null, 'team_admin.create')")
    @Operation(summary = "Create a custom or template-cloned role")
    public RoleResponse createRole(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateRoleRequest request) {
        return roleService.createRole(TenantJwt.requireTenantId(jwt), request);
    }

    @PatchMapping("/roles/{roleId}")
    @PreAuthorize("hasPermission(null, 'team_admin.edit')")
    @Operation(summary = "Update a non-system role")
    public RoleResponse updateRole(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String roleId,
        @Valid @RequestBody UpdateRoleRequest request
    ) {
        return roleService.updateRole(TenantJwt.requireTenantId(jwt), roleId, request);
    }

    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasPermission(null, 'team_admin.delete')")
    @Operation(summary = "Delete a non-system role")
    public void deleteRole(@AuthenticationPrincipal Jwt jwt, @PathVariable String roleId) {
        roleService.deleteRole(TenantJwt.requireTenantId(jwt), roleId);
    }

    @GetMapping("/users")
    @PreAuthorize("hasPermission(null, 'team_admin.view')")
    @Operation(summary = "List tenant users")
    public List<TenantUserResponse> listUsers(@AuthenticationPrincipal Jwt jwt) {
        return userAdminService.listUsers(TenantJwt.requireTenantId(jwt));
    }

    @PostMapping("/users/invite")
    @PreAuthorize("hasPermission(null, 'team_admin.create')")
    @Operation(summary = "Invite a tenant user")
    public TenantUserResponse inviteUser(@AuthenticationPrincipal Jwt jwt, @RequestBody InviteUserRequest request) {
        return userAdminService.inviteUser(TenantJwt.requireTenantId(jwt), request);
    }

    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasPermission(null, 'team_admin.edit')")
    @Operation(summary = "Reassign a tenant user to a different role")
    public TenantUserResponse reassignUserRole(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String userId,
        @Valid @RequestBody ReassignUserRoleRequest request
    ) {
        return userAdminService.reassignUserRole(TenantJwt.requireTenantId(jwt), userId, request);
    }

    @PatchMapping("/users/{userId}")
    @PreAuthorize("hasPermission(null, 'team_admin.edit')")
    @Operation(summary = "Update a tenant user's profile and role")
    public TenantUserResponse updateUser(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return userAdminService.updateUser(TenantJwt.requireTenantId(jwt), userId, request);
    }

    @PostMapping("/users/{userId}/disable")
    @PreAuthorize("hasPermission(null, 'team_admin.delete')")
    @Operation(summary = "Disable a tenant user")
    public void disableUser(@AuthenticationPrincipal Jwt jwt, @PathVariable String userId) {
        userAdminService.disableUser(TenantJwt.requireTenantId(jwt), userId);
    }

    @PostMapping("/privileges/load")
    @PreAuthorize("hasPermission(null, 'team_admin.view')")
    @Operation(summary = "Load privilege matrix for role and optional user")
    public PrivilegeMatrixResponse loadPrivileges(@AuthenticationPrincipal Jwt jwt, @RequestBody LoadPrivilegesRequest request) {
        return privilegeService.loadMatrix(TenantJwt.requireTenantId(jwt), request.getRoleId(), request.getUserId());
    }

    @PostMapping("/privileges/assign")
    @PreAuthorize("hasPermission(null, 'team_admin.edit')")
    @Operation(summary = "Assign privileges to role or user overrides")
    public PrivilegeMatrixResponse assignPrivileges(@AuthenticationPrincipal Jwt jwt, @RequestBody AssignPrivilegesRequest request) {
        return privilegeService.assign(TenantJwt.requireTenantId(jwt), request);
    }
}

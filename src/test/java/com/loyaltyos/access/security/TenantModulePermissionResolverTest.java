package com.loyaltyos.access.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TenantModulePermissionResolverTest {

    @Test
    void postPrivilegesLoad_requiresViewNotCreate() {
        assertEquals(
            "team_admin.view",
            TenantModulePermissionResolver.resolve(
                "team_admin",
                "POST",
                "/api/v1/me/access/privileges/load"
            )
        );
    }

    @Test
    void postPrivilegesAssign_requiresEdit() {
        assertEquals(
            "team_admin.edit",
            TenantModulePermissionResolver.resolve(
                "team_admin",
                "POST",
                "/api/v1/me/access/privileges/assign"
            )
        );
    }

    @Test
    void postInviteUser_stillRequiresCreate() {
        assertEquals(
            "team_admin.create",
            TenantModulePermissionResolver.resolve(
                "team_admin",
                "POST",
                "/api/v1/me/access/users/invite"
            )
        );
    }

    @Test
    void postDisableUser_requiresDelete() {
        assertEquals(
            "team_admin.delete",
            TenantModulePermissionResolver.resolve(
                "team_admin",
                "POST",
                "/api/v1/me/access/users/usr-1/disable"
            )
        );
    }
}

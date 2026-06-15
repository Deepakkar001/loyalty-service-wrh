package com.loyaltyos.access.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.access.config.AccessProperties;
import com.loyaltyos.access.exception.ModuleNotEntitledException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TenantModuleAccessFilterTest {

    private ModuleEntitlementGuard guard;
    private AccessProperties accessProperties;
    private TenantModuleAccessFilter filter;

    @BeforeEach
    void setUp() {
        guard = mock(ModuleEntitlementGuard.class);
        accessProperties = new AccessProperties();
        filter = new TenantModuleAccessFilter(guard, accessProperties, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsEnforcementWhenPermissionsNotEnforced() throws Exception {
        accessProperties.getPermissions().setEnforce(false);
        authenticateTenant("tenant-1");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/merchants");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(guard, never()).requireModuleAccess(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void returnsForbiddenWhenModuleNotEntitled() throws Exception {
        accessProperties.getPermissions().setEnforce(true);
        authenticateTenant("tenant-1");
        doThrow(new ModuleNotEntitledException("merchants"))
            .when(guard).requireModuleAccess(
                org.mockito.ArgumentMatchers.any(),
                eq("merchants"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
            );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/merchants");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsEntitledModuleRequests() throws Exception {
        accessProperties.getPermissions().setEnforce(true);
        authenticateTenant("tenant-1");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/config");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(guard).requireModuleAccess(
            org.mockito.ArgumentMatchers.any(),
            eq("programme_config"),
            eq("GET"),
            eq("/api/v1/me/config")
        );
        verify(chain).doFilter(request, response);
    }

    private void authenticateTenant(String tenantId) {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("tenantId", tenantId)
            .claim("role", "TENANT_ADMIN")
            .claim("sessionVersion", 1)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, Map.of(), "tenant"));
    }
}

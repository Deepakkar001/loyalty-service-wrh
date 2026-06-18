package com.loyaltyos.merchants.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.merchants.repository.MerchantCredentialsRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MerchantOnboardingEmailGuardTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantCredentialsRepository credentialsRepository;

    @Mock
    private TenantOnboardingRepository tenantRepository;

    @Mock
    private TenantUserRepository tenantUserRepository;

    private MerchantOnboardingEmailGuard guard;

    @BeforeEach
    void setUp() {
        guard = new MerchantOnboardingEmailGuard(
            merchantRepository,
            credentialsRepository,
            tenantRepository,
            tenantUserRepository
        );
    }

    @Test
    void checkPortalEmailAvailability_rejectsTenantPrimaryAdminEmail() {
        TenantOnboarding tenant = new TenantOnboarding();
        tenant.setTenantId("tenant-1");
        tenant.setEmail("admin@tenant.com");
        when(tenantRepository.findByTenantId("tenant-1")).thenReturn(Optional.of(tenant));

        var result = guard.checkPortalEmailAvailability("tenant-1", "admin@tenant.com", null);

        assertFalse(result.isAvailable());
        assertTrue(result.getMessage().contains("tenant administrator"));
    }

    @Test
    void checkPortalEmailAvailability_rejectsExistingTenantUser() {
        when(tenantRepository.findByTenantId("tenant-1")).thenReturn(Optional.empty());
        TenantUser user = new TenantUser();
        user.setEmail("ops@tenant.com");
        when(tenantUserRepository.findByTenantIdAndEmailIgnoreCase("tenant-1", "ops@tenant.com"))
            .thenReturn(Optional.of(user));

        var result = guard.checkPortalEmailAvailability("tenant-1", "ops@tenant.com", null);

        assertFalse(result.isAvailable());
        assertTrue(result.getMessage().contains("tenant user"));
    }

    @Test
    void checkPortalEmailAvailability_rejectsDuplicateMerchantContactEmail() {
        when(tenantRepository.findByTenantId("tenant-1")).thenReturn(Optional.empty());
        when(tenantUserRepository.findByTenantIdAndEmailIgnoreCase("tenant-1", "partner@acme.com"))
            .thenReturn(Optional.empty());
        when(merchantRepository.existsByTenantIdAndContactEmailIgnoreCase("tenant-1", "partner@acme.com"))
            .thenReturn(true);

        var result = guard.checkPortalEmailAvailability("tenant-1", "partner@acme.com", null);

        assertFalse(result.isAvailable());
        assertTrue(result.getMessage().contains("another merchant"));
    }

    @Test
    void checkPortalEmailAvailability_allowsSameMerchantEmailWhenExcluded() {
        when(tenantRepository.findByTenantId("tenant-1")).thenReturn(Optional.empty());
        when(tenantUserRepository.findByTenantIdAndEmailIgnoreCase("tenant-1", "partner@acme.com"))
            .thenReturn(Optional.empty());
        when(merchantRepository.existsByTenantIdAndContactEmailIgnoreCaseAndMerchantUidNot(
            "tenant-1", "partner@acme.com", "m-1"))
            .thenReturn(false);
        when(credentialsRepository.existsByTenantIdAndUsernameIgnoreCaseAndMerchantUidNot(
            "tenant-1", "partner@acme.com", "m-1"))
            .thenReturn(false);

        var result = guard.checkPortalEmailAvailability("tenant-1", "partner@acme.com", "m-1");

        assertTrue(result.isAvailable());
    }

    @Test
    void assertPortalEmailAvailable_throwsConflictWhenUnavailable() {
        when(tenantRepository.findByTenantId("tenant-1")).thenReturn(Optional.empty());
        when(tenantUserRepository.findByTenantIdAndEmailIgnoreCase("tenant-1", "partner@acme.com"))
            .thenReturn(Optional.empty());
        when(merchantRepository.existsByTenantIdAndContactEmailIgnoreCase("tenant-1", "partner@acme.com"))
            .thenReturn(true);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> guard.assertPortalEmailAvailable("tenant-1", "partner@acme.com", null)
        );
        assertEquals(409, ex.getStatusCode().value());
    }
}

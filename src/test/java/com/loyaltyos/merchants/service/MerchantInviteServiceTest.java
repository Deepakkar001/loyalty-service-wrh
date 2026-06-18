package com.loyaltyos.merchants.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantCredentials;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.repository.MerchantCredentialsRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MerchantInviteServiceTest {

    @Mock
    private MerchantCredentialsRepository credentialsRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantInviteMailer inviteMailer;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MerchantProperties merchantProperties;
    private MerchantInviteService inviteService;

    @BeforeEach
    void setUp() {
        merchantProperties = new MerchantProperties();
        merchantProperties.setInviteTokenTtlDays(7);
        inviteService = new MerchantInviteService(
            credentialsRepository,
            merchantRepository,
            inviteMailer,
            merchantProperties,
            passwordEncoder
        );
    }

    @Test
    void validateToken_returnsInvalidForBlankToken() {
        var response = inviteService.validateToken("  ");
        assertFalse(response.isValid());
    }

    @Test
    void resendPortalInvite_refreshesTokenWhenInvitePending() {
        Merchant merchant = activeMerchant();
        MerchantCredentials creds = pendingCreds(merchant);

        when(credentialsRepository.findByTenantIdAndMerchantUid("tenant-1", "m-1"))
            .thenReturn(Optional.of(creds));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(inviteMailer.sendInviteEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(inviteMailer.buildAcceptUrl(anyString())).thenReturn("http://localhost:3000/merchant/onboarding/token");
        when(credentialsRepository.save(any(MerchantCredentials.class))).thenAnswer(i -> i.getArgument(0));

        var result = inviteService.resendPortalInvite(merchant);

        assertTrue(result.emailSent());
        assertEquals("partner@example.com", result.credentials().getUsername());
        assertEquals("http://localhost:3000/merchant/onboarding/token", result.inviteUrl());
        verify(credentialsRepository).save(creds);
    }

    @Test
    void resendPortalInvite_rejectsWhenAlreadyAccepted() {
        Merchant merchant = activeMerchant();
        MerchantCredentials creds = pendingCreds(merchant);
        creds.setInviteAcceptedAt(Instant.now());

        when(credentialsRepository.findByTenantIdAndMerchantUid("tenant-1", "m-1"))
            .thenReturn(Optional.of(creds));

        assertThrows(ResponseStatusException.class, () -> inviteService.resendPortalInvite(merchant));
        verify(inviteMailer, never()).sendInviteEmail(anyString(), anyString(), anyString());
    }

    @Test
    void hashToken_isDeterministic() {
        String a = MerchantInviteService.hashToken("abc");
        String b = MerchantInviteService.hashToken("abc");
        assertEquals(a, b);
    }

    private static Merchant activeMerchant() {
        Merchant merchant = new Merchant();
        merchant.setTenantId("tenant-1");
        merchant.setMerchantUid("m-1");
        merchant.setLegalName("Acme");
        merchant.setContactEmail("partner@example.com");
        merchant.setOnboardingStage(MerchantOnboardingStage.ACTIVE);
        return merchant;
    }

    private static MerchantCredentials pendingCreds(Merchant merchant) {
        MerchantCredentials creds = new MerchantCredentials();
        creds.setTenantId(merchant.getTenantId());
        creds.setMerchantUid(merchant.getMerchantUid());
        creds.setUsername(merchant.getContactEmail());
        creds.setActive(true);
        creds.setInviteTokenHash("old");
        creds.setInviteTokenExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        return creds;
    }
}

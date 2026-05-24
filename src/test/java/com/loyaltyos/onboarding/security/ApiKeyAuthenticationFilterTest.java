package com.loyaltyos.onboarding.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loyaltyos.integration.security.IntegrationHmacVerifier;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationCredentialCryptoService;
import com.loyaltyos.integration.service.IntegrationRateLimitService;
import com.loyaltyos.onboarding.entity.TenantApiKey;
import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticationFilterTest {

  @Test
  void skipsNonIntegrationPaths() throws Exception {
    ApiKeyAuthenticationFilter filter = buildFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/integration/credentials");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
  }

  @Test
  void rejectsMissingAuthorization() throws Exception {
    ApiKeyAuthenticationFilter filter = buildFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/integration/t1/events/process");
    request.setContent("{}".getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);
    assertEquals(401, response.getStatus());
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void acceptsValidKeyAndSignature() throws Exception {
    String apiKey = "los_sandbox_" + "a".repeat(32);
    String secret = "test-secret-value";
    String body = "{\"eventId\":\"evt_1\"}";
    String sig = "sha256=" + IntegrationHmacVerifier.hmacSha256Hex(body, secret);

    TenantApiKey entity = TenantApiKey.builder()
        .tenantId("t1")
        .keyUid("uid1")
        .keyPrefix("los_sandbox_aaa")
        .keyHash(IntegrationHmacVerifier.sha256Hex(apiKey))
        .signingSecretHash(IntegrationHmacVerifier.sha256Hex(secret))
        .signingSecretEncrypted("enc")
        .environment(ApiKeyEnvironment.SANDBOX)
        .status(ApiKeyStatus.ACTIVE)
        .build();

    TenantApiKeyRepository keyRepo = mock(TenantApiKeyRepository.class);
    when(keyRepo.findByKeyHash(anyString())).thenReturn(Optional.of(entity));
    when(keyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    IntegrationCredentialCryptoService crypto = mock(IntegrationCredentialCryptoService.class);
    when(crypto.decrypt("enc")).thenReturn(secret);

    IntegrationRateLimitService rateLimit = mock(IntegrationRateLimitService.class);
    when(rateLimit.check(entity)).thenReturn(
        IntegrationRateLimitService.RateLimitResult.allowed(1000, 999));

    ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
        keyRepo, crypto, rateLimit, mock(IntegrationAuditService.class), objectMapper());

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/integration/t1/events/process");
    request.addHeader("Authorization", "Bearer " + apiKey);
    request.addHeader("X-LoyaltyOS-Signature", sig);
    request.setContent(body.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);
    verify(chain).doFilter(any(), eq(response));
  }

  @Test
  void acceptsGetWithEmptyBodySignature() throws Exception {
    String apiKey = "los_sandbox_" + "c".repeat(32);
    String secret = "test-secret-value";
    String sig = "sha256=" + IntegrationHmacVerifier.hmacSha256Hex("", secret);

    TenantApiKey entity = TenantApiKey.builder()
        .tenantId("t1")
        .keyUid("uid1")
        .keyPrefix("los_sandbox_ccc")
        .keyHash(IntegrationHmacVerifier.sha256Hex(apiKey))
        .signingSecretHash(IntegrationHmacVerifier.sha256Hex(secret))
        .signingSecretEncrypted("enc")
        .environment(ApiKeyEnvironment.SANDBOX)
        .status(ApiKeyStatus.ACTIVE)
        .build();

    TenantApiKeyRepository keyRepo = mock(TenantApiKeyRepository.class);
    when(keyRepo.findByKeyHash(anyString())).thenReturn(Optional.of(entity));
    when(keyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    IntegrationCredentialCryptoService crypto = mock(IntegrationCredentialCryptoService.class);
    when(crypto.decrypt("enc")).thenReturn(secret);

    IntegrationRateLimitService rateLimit = mock(IntegrationRateLimitService.class);
    when(rateLimit.check(entity)).thenReturn(
        IntegrationRateLimitService.RateLimitResult.allowed(1000, 999));

    ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
        keyRepo, crypto, rateLimit, mock(IntegrationAuditService.class), objectMapper());

    MockHttpServletRequest request = new MockHttpServletRequest(
        "GET", "/api/v1/integration/t1/events/evt_postman_001");
    request.addHeader("Authorization", "Bearer " + apiKey);
    request.addHeader("X-LoyaltyOS-Signature", sig);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);
    verify(chain).doFilter(any(), eq(response));
  }

  @Test
  void passesReadableBodyToDownstreamFilter() throws Exception {
    String apiKey = "los_sandbox_" + "b".repeat(32);
    String secret = "test-secret-value";
    String body = "{\"eventId\":\"evt_1\",\"customerId\":\"c1\"}";
    String sig = "sha256=" + IntegrationHmacVerifier.hmacSha256Hex(body, secret);

    TenantApiKey entity = TenantApiKey.builder()
        .tenantId("t1")
        .keyUid("uid1")
        .keyPrefix("los_sandbox_bbb")
        .keyHash(IntegrationHmacVerifier.sha256Hex(apiKey))
        .signingSecretHash(IntegrationHmacVerifier.sha256Hex(secret))
        .signingSecretEncrypted("enc")
        .environment(ApiKeyEnvironment.SANDBOX)
        .status(ApiKeyStatus.ACTIVE)
        .build();

    TenantApiKeyRepository keyRepo = mock(TenantApiKeyRepository.class);
    when(keyRepo.findByKeyHash(anyString())).thenReturn(Optional.of(entity));
    when(keyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    IntegrationCredentialCryptoService crypto = mock(IntegrationCredentialCryptoService.class);
    when(crypto.decrypt("enc")).thenReturn(secret);

    IntegrationRateLimitService rateLimit = mock(IntegrationRateLimitService.class);
    when(rateLimit.check(entity)).thenReturn(
        IntegrationRateLimitService.RateLimitResult.allowed(1000, 999));

    ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
        keyRepo, crypto, rateLimit, mock(IntegrationAuditService.class), objectMapper());

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/integration/t1/events/validate");
    request.addHeader("Authorization", "Bearer " + apiKey);
    request.addHeader("X-LoyaltyOS-Signature", sig);
    request.setContent(body.getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();

    final String[] downstreamBody = new String[1];
    FilterChain chain = (req, res) ->
        downstreamBody[0] = new String(req.getInputStream().readAllBytes());

    filter.doFilter(request, response, chain);
    assertEquals(body, downstreamBody[0]);
  }

  private static ApiKeyAuthenticationFilter buildFilter() {
    return new ApiKeyAuthenticationFilter(
        mock(TenantApiKeyRepository.class),
        mock(IntegrationCredentialCryptoService.class),
        mock(IntegrationRateLimitService.class),
        mock(IntegrationAuditService.class),
        objectMapper()
    );
  }

  private static ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }
}

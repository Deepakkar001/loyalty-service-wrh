package com.loyaltyos.onboarding.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.integration.dto.EventProcessingErrorResponse;
import com.loyaltyos.integration.security.ApiKeyAuthentication;
import com.loyaltyos.integration.security.CachedBodyHttpServletRequest;
import com.loyaltyos.integration.security.IntegrationHmacVerifier;
import com.loyaltyos.integration.security.IntegrationIpWhitelistMatcher;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationCredentialCryptoService;
import com.loyaltyos.integration.service.IntegrationRateLimitService;
import com.loyaltyos.access.service.AccessResolutionService;
import com.loyaltyos.onboarding.entity.TenantApiKey;
import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import jakarta.servlet.FilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private static final Pattern INTEGRATION_PATH =
        Pattern.compile("^/api/v1/integration/([^/]+)(/.*)?$");

    private final TenantApiKeyRepository tenantApiKeyRepository;
    private final IntegrationCredentialCryptoService cryptoService;
    private final IntegrationRateLimitService rateLimitService;
    private final IntegrationAuditService auditService;
    private final ObjectMapper objectMapper;
    private final AccessResolutionService accessResolutionService;

    public ApiKeyAuthenticationFilter(
        TenantApiKeyRepository tenantApiKeyRepository,
        IntegrationCredentialCryptoService cryptoService,
        IntegrationRateLimitService rateLimitService,
        IntegrationAuditService auditService,
        ObjectMapper objectMapper,
        AccessResolutionService accessResolutionService
    ) {
        this.tenantApiKeyRepository = Objects.requireNonNull(tenantApiKeyRepository, "tenantApiKeyRepository");
        this.cryptoService = Objects.requireNonNull(cryptoService, "cryptoService");
        this.rateLimitService = Objects.requireNonNull(rateLimitService, "rateLimitService");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.accessResolutionService = Objects.requireNonNull(accessResolutionService, "accessResolutionService");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/v1/integration/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = headerOrNew(request, "X-Request-ID");
        byte[] bodyBytes = request.getInputStream().readAllBytes();
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, bodyBytes);
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        String pathTenantId = extractPathTenantId(wrapped.getRequestURI());

        try {
            String apiKey = extractBearerKey(wrapped);
            if (apiKey == null) {
                writeError(wrapped, response, 401, "UNAUTHORIZED", "Invalid or missing API key", false, start, requestId, null);
                return;
            }

            String keyHash = IntegrationHmacVerifier.sha256Hex(apiKey);
            Optional<TenantApiKey> keyOpt = tenantApiKeyRepository.findByKeyHash(keyHash);
            if (keyOpt.isEmpty() || keyOpt.get().getStatus() != ApiKeyStatus.ACTIVE) {
                writeError(wrapped, response, 401, "UNAUTHORIZED", "Invalid or missing API key", false, start, requestId, null);
                return;
            }

            TenantApiKey apiKeyEntity = keyOpt.get();
            if (pathTenantId != null && !pathTenantId.equals(apiKeyEntity.getTenantId())) {
                writeError(wrapped, response, 403, "FORBIDDEN", "API key does not match tenant", false, start, requestId, apiKeyEntity);
                return;
            }

            if (!accessResolutionService.isModuleEntitled(apiKeyEntity.getTenantId(), "integrations")) {
                writeError(wrapped, response, 403, "MODULE_NOT_ENTITLED",
                    "Integrations module is not enabled for this tenant", false, start, requestId, apiKeyEntity);
                return;
            }

            if (!environmentMatchesBearer(apiKey, apiKeyEntity.getEnvironment())) {
                writeError(wrapped, response, 403, "FORBIDDEN",
                    "API key environment does not match token prefix", false, start, requestId, apiKeyEntity);
                return;
            }

            String clientIp = clientIp(wrapped);
            if (!IntegrationIpWhitelistMatcher.isAllowed(apiKeyEntity.getIpWhitelist(), clientIp, objectMapper)) {
                writeError(wrapped, response, 403, "FORBIDDEN", "Client IP not allowed for this API key",
                    false, start, requestId, apiKeyEntity);
                return;
            }

            String requestedEnv = wrapped.getHeader("X-LoyaltyOS-Environment");
            if (requestedEnv != null && !requestedEnv.isBlank()
                && !requestedEnv.equalsIgnoreCase(apiKeyEntity.getEnvironment().name())) {
                writeError(wrapped, response, 403, "FORBIDDEN",
                    "X-LoyaltyOS-Environment does not match API key environment",
                    false, start, requestId, apiKeyEntity);
                return;
            }

            if (!apiKeyEntity.hasRetrievableSecret()) {
                writeError(wrapped, response, 401, "UNAUTHORIZED", "API key requires rotation before use", false, start, requestId, apiKeyEntity);
                return;
            }

            IntegrationRateLimitService.RateLimitResult rate = rateLimitService.check(apiKeyEntity);
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(rate.limit()));
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(rate.remaining()));
            if (rate.resetAt() != null) {
                response.setHeader("X-Rate-Limit-Reset", rate.resetAt().toString());
            }
            if (!rate.allowed()) {
                response.setHeader("Retry-After", "60");
                writeError(wrapped, response, 429, "RATE_LIMIT_EXCEEDED",
                    "API rate limit exceeded: " + rate.limit() + " requests per hour", true, start, requestId, apiKeyEntity);
                return;
            }

            String signature = wrapped.getHeader("X-LoyaltyOS-Signature");
            String secret = cryptoService.decrypt(apiKeyEntity.getSigningSecretEncrypted());
            if (!IntegrationHmacVerifier.verifySignature(body, signature, secret)) {
                writeError(wrapped, response, 401, "INVALID_SIGNATURE", "Request signature verification failed", false, start, requestId, apiKeyEntity);
                return;
            }

            apiKeyEntity.setLastUsedAt(Instant.now());
            tenantApiKeyRepository.save(apiKeyEntity);

            SecurityContextHolder.getContext().setAuthentication(
                new ApiKeyAuthentication(apiKeyEntity.getTenantId(), apiKeyEntity.getKeyUid(), apiKeyEntity.getEnvironment())
            );
            wrapped.setAttribute("integration.apiKeyUid", apiKeyEntity.getKeyUid());
            wrapped.setAttribute("integration.requestId", requestId);
            wrapped.setAttribute("integration.requestBody", body);

            filterChain.doFilter(wrapped, response);
        } catch (Exception e) {
            log.error("Integration API filter error: {}", e.getMessage(), e);
            if (!response.isCommitted()) {
                writeError(wrapped, response, 500, "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred processing your request", true, start, requestId, null);
            }
        }
    }

    private void writeError(
        HttpServletRequest request,
        HttpServletResponse response,
        int status,
        String code,
        String message,
        boolean retryable,
        long startMs,
        String requestId,
        TenantApiKey key
    ) throws IOException {
        int processingTime = (int) (System.currentTimeMillis() - startMs);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (retryable) {
            response.setHeader("Retry-After", "60");
        }

        EventProcessingErrorResponse body = new EventProcessingErrorResponse();
        body.setErrorCode(code);
        body.setErrorMessage(message);
        body.setRetryable(retryable);
        body.setTimestamp(Instant.now());
        if (status >= 500) {
            body.setErrorId("err_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            response.setHeader("X-Error-ID", body.getErrorId());
        }
        byte[] json = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        response.setContentLength(json.length);
        response.getOutputStream().write(json);

        String tenantId = key != null ? key.getTenantId() : extractPathTenantId(request.getRequestURI());
        if (tenantId != null) {
            auditService.logApiRequest(
                tenantId,
                key != null ? key.getKeyUid() : null,
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                null,
                null,
                status,
                processingTime,
                code,
                message,
                IntegrationHmacVerifier.sha256Hex(readBodySafe(request)),
                clientIp(request),
                request.getHeader("User-Agent")
            );
        }
    }

    private static String readBodySafe(HttpServletRequest request) {
        if (request instanceof CachedBodyHttpServletRequest cached) {
            return new String(cached.getCachedBody(), StandardCharsets.UTF_8);
        }
        return "";
    }

    private static String extractBearerKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        String token = auth.substring("Bearer ".length()).trim();
        if (!token.startsWith("los_")) {
            return null;
        }
        return token;
    }

    private static String extractPathTenantId(String uri) {
        if (uri == null) {
            return null;
        }
        Matcher m = INTEGRATION_PATH.matcher(uri);
        return m.matches() ? m.group(1) : null;
    }

    private static String headerOrNew(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null && !value.isBlank() ? value : UUID.randomUUID().toString();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean environmentMatchesBearer(String apiKey, ApiKeyEnvironment environment) {
        if (apiKey.startsWith("los_live_")) {
            return environment == ApiKeyEnvironment.PRODUCTION;
        }
        if (apiKey.startsWith("los_sandbox_")) {
            return environment == ApiKeyEnvironment.SANDBOX;
        }
        return false;
    }
}

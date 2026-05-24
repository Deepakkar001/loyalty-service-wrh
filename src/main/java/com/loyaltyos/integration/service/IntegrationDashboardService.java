package com.loyaltyos.integration.service;

import com.loyaltyos.integration.dto.CredentialSummaryDto;
import com.loyaltyos.integration.dto.DashboardOverviewResponse;
import com.loyaltyos.integration.dto.GenerateCredentialRequest;
import com.loyaltyos.integration.dto.RevealSecretResponse;
import com.loyaltyos.integration.dto.RotateCredentialResponse;
import com.loyaltyos.integration.dto.StatisticsResponse;
import com.loyaltyos.integration.enums.EventProcessingStatus;
import com.loyaltyos.integration.repository.IntegrationEventProcessingLogRepository;
import com.loyaltyos.integration.entity.ApiRequestAuditLog;
import com.loyaltyos.integration.enums.CredentialAccessType;
import com.loyaltyos.integration.repository.ApiRequestAuditLogRepository;
import com.loyaltyos.onboarding.dto.ApiKeyGeneratedResponse;
import com.loyaltyos.onboarding.entity.TenantApiKey;
import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.service.IntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class IntegrationDashboardService {

    private static final String SECRET_MASK = "████████████████████████████████";

    private final IntegrationService integrationService;
    private final TenantApiKeyRepository tenantApiKeyRepository;
    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final IntegrationCredentialCryptoService cryptoService;
    private final IntegrationAuditService auditService;
    private final ApiRequestAuditLogRepository apiRequestAuditLogRepository;
    private final IntegrationEventProcessingLogRepository eventProcessingLogRepository;

    public IntegrationDashboardService(
        IntegrationService integrationService,
        TenantApiKeyRepository tenantApiKeyRepository,
        TenantOnboardingRepository tenantOnboardingRepository,
        IntegrationCredentialCryptoService cryptoService,
        IntegrationAuditService auditService,
        ApiRequestAuditLogRepository apiRequestAuditLogRepository,
        IntegrationEventProcessingLogRepository eventProcessingLogRepository
    ) {
        this.integrationService = Objects.requireNonNull(integrationService, "integrationService");
        this.tenantApiKeyRepository = Objects.requireNonNull(tenantApiKeyRepository, "tenantApiKeyRepository");
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository, "tenantOnboardingRepository");
        this.cryptoService = Objects.requireNonNull(cryptoService, "cryptoService");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.apiRequestAuditLogRepository = Objects.requireNonNull(apiRequestAuditLogRepository, "apiRequestAuditLogRepository");
        this.eventProcessingLogRepository = Objects.requireNonNull(eventProcessingLogRepository, "eventProcessingLogRepository");
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(String tenantId) {
        ensureTenant(tenantId);
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        long total = apiRequestAuditLogRepository.countByTenantIdSince(tenantId, since);
        long success = apiRequestAuditLogRepository.countSuccessfulByTenantIdSince(tenantId, since);
        List<Integer> latencies = apiRequestAuditLogRepository.findLatenciesSince(tenantId, since);

        DashboardOverviewResponse overview = new DashboardOverviewResponse();
        overview.setTotalActiveKeys(countActiveKeys(tenantId));
        overview.setTotalRequestsLast24h((int) total);
        overview.setSuccessfulRequests((int) success);
        overview.setFailedRequests((int) (total - success));
        overview.setErrorRate(total == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(total - success).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        overview.setAvgResponseTime(average(latencies));
        overview.setP99ResponseTime(percentile(latencies, 99));
        overview.setLastRequestAt(apiRequestAuditLogRepository.findLastRequestAt(tenantId));
        return overview;
    }

    @Transactional(readOnly = true)
    public List<CredentialSummaryDto> listCredentials(String tenantId, String environmentFilter) {
        ensureTenant(tenantId);
        List<TenantApiKey> keys = tenantApiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        return keys.stream()
            .filter(k -> matchesEnvironmentFilter(k, environmentFilter))
            .map(k -> toSummary(k, tenantId, since))
            .collect(Collectors.toList());
    }

    @Transactional
    public ApiKeyGeneratedResponse generateCredentials(
        String tenantId,
        GenerateCredentialRequest request,
        String userId,
        HttpServletRequest httpRequest
    ) {
        ApiKeyGeneratedResponse response = integrationService.generateKeys(tenantId, request.getEnvironment());
        TenantApiKey key = tenantApiKeyRepository.findByKeyUidAndTenantId(response.getKeyUid(), tenantId)
            .orElseThrow(() -> new IllegalStateException("API key was created but could not be loaded"));
        if (request.getName() != null) {
            key.setName(request.getName());
        }
        if (request.getDescription() != null) {
            key.setDescription(request.getDescription());
        }
        tenantApiKeyRepository.save(key);
        auditService.logCredentialAccess(
            tenantId, key.getKeyUid(), CredentialAccessType.CREATE, userId, clientIp(httpRequest), null
        );
        return response;
    }

    @Transactional
    public RevealSecretResponse revealSecret(
        String tenantId,
        String keyId,
        String userId,
        HttpServletRequest httpRequest
    ) {
        TenantApiKey key = tenantApiKeyRepository.findByKeyUidAndTenantId(keyId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        if (!key.hasRetrievableSecret()) {
            throw new IllegalStateException("Signing secret not available — rotate credentials to enable HMAC.");
        }
        Instant previousReveal = key.getLastSecretRevealedAt();
        String secret = cryptoService.decrypt(key.getSigningSecretEncrypted());
        key.setLastSecretRevealedAt(Instant.now());
        tenantApiKeyRepository.save(key);
        auditService.logCredentialAccess(
            tenantId, keyId, CredentialAccessType.REVEAL_SECRET, userId, clientIp(httpRequest), null
        );

        RevealSecretResponse response = new RevealSecretResponse();
        response.setKeyId(keyId);
        response.setApiKey(key.getKeyPrefix() + "…");
        response.setSigningSecret(secret);
        response.setLastSecretRevealedAt(previousReveal);
        response.setWarning("This action has been logged.");
        return response;
    }

    @Transactional
    public RotateCredentialResponse rotateCredential(
        String tenantId,
        String keyId,
        String userId,
        HttpServletRequest httpRequest
    ) {
        TenantApiKey old = tenantApiKeyRepository.findByKeyUidAndTenantId(keyId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        old.setStatus(ApiKeyStatus.REVOKED);
        old.setRevokedAt(Instant.now());
        tenantApiKeyRepository.save(old);
        auditService.logCredentialAccess(
            tenantId, keyId, CredentialAccessType.ROTATE, userId, clientIp(httpRequest), null
        );
        GenerateCredentialRequest req = new GenerateCredentialRequest();
        req.setEnvironment(old.getEnvironment());
        req.setName(old.getName());
        req.setDescription(old.getDescription());
        ApiKeyGeneratedResponse generated = generateCredentials(tenantId, req, userId, httpRequest);
        RotateCredentialResponse response = new RotateCredentialResponse();
        response.setKeyId(generated.getKeyUid());
        response.setApiKey(generated.getApiKey());
        response.setSigningSecret(generated.getSigningSecret());
        response.setEnvironment(generated.getEnvironment());
        response.setKeyPrefix(generated.getKeyPrefix());
        response.setCreatedAt(Instant.now());
        response.setRevokedKeyId(keyId);
        response.setMessage("Save these credentials now — previous key has been revoked.");
        return response;
    }

    @Transactional
    public void revokeCredential(String tenantId, String keyId, String userId, HttpServletRequest httpRequest) {
        TenantApiKey key = tenantApiKeyRepository.findByKeyUidAndTenantId(keyId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        key.setStatus(ApiKeyStatus.REVOKED);
        key.setRevokedAt(Instant.now());
        tenantApiKeyRepository.save(key);
        auditService.logCredentialAccess(
            tenantId, keyId, CredentialAccessType.REVOKE, userId, clientIp(httpRequest), null
        );
    }

    @Transactional(readOnly = true)
    public Page<ApiRequestAuditLog> getAuditLogs(String tenantId, Pageable pageable) {
        return auditService.getAuditLogs(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(String tenantId, Instant from, Instant to) {
        Instant rangeFrom = from != null ? from : Instant.now().minus(24, ChronoUnit.HOURS);
        Instant rangeTo = to != null ? to : Instant.now();
        long total = apiRequestAuditLogRepository.countByTenantIdSince(tenantId, rangeFrom);
        long success = apiRequestAuditLogRepository.countSuccessfulByTenantIdSince(tenantId, rangeFrom);
        List<Integer> latencies = apiRequestAuditLogRepository.findLatenciesSince(tenantId, rangeFrom);

        StatisticsResponse stats = new StatisticsResponse();
        StatisticsResponse.TimeRange range = new StatisticsResponse.TimeRange();
        range.setFrom(rangeFrom);
        range.setTo(rangeTo);
        stats.setTimeRange(range);
        stats.setTotalRequests((int) total);
        stats.setSuccessful((int) success);
        stats.setFailed((int) (total - success));
        stats.setSuccessRate(total == 0 ? BigDecimal.valueOf(100)
            : BigDecimal.valueOf(success).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        StatisticsResponse.LatencyStats latency = new StatisticsResponse.LatencyStats();
        latency.setAvg(average(latencies));
        latency.setP50(percentile(latencies, 50));
        latency.setP95(percentile(latencies, 95));
        latency.setP99(percentile(latencies, 99));
        stats.setLatency(latency);

        Map<String, Integer> errorBreakdown = new LinkedHashMap<>();
        for (Object[] row : apiRequestAuditLogRepository.countByHttpStatusGrouped(tenantId, rangeFrom, rangeTo)) {
            errorBreakdown.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
        }
        stats.setErrorBreakdown(errorBreakdown);

        long eventsProcessed = eventProcessingLogRepository.countByTenantAndStatusSince(
            tenantId, rangeFrom, rangeTo, EventProcessingStatus.SUCCESS);
        stats.setEventsProcessed((int) eventsProcessed);
        stats.setPointsAwarded(eventProcessingLogRepository.sumPointsAwardedSince(
            tenantId, rangeFrom, rangeTo, EventProcessingStatus.SUCCESS));
        stats.setCustomersImpacted((int) eventProcessingLogRepository.countDistinctCustomersSince(
            tenantId, rangeFrom, rangeTo));
        return stats;
    }

    private CredentialSummaryDto toSummary(TenantApiKey k, String tenantId, Instant since) {
        CredentialSummaryDto dto = new CredentialSummaryDto();
        dto.setKeyId(k.getKeyUid());
        dto.setKeyPrefix(k.getKeyPrefix());
        dto.setSecretMasked(SECRET_MASK);
        dto.setEnvironment(k.getEnvironment());
        dto.setStatus(k.getStatus());
        dto.setCreatedAt(k.getCreatedAt());
        dto.setLastUsedAt(k.getLastUsedAt());
        dto.setLastSecretRevealedAt(k.getLastSecretRevealedAt());
        dto.setName(k.getName());
        dto.setDescription(k.getDescription());
        dto.setSecretRetrievable(k.hasRetrievableSecret());
        dto.setRequestCountLast24h((int) apiRequestAuditLogRepository.countByApiKeyUidSince(
            k.getKeyUid(), tenantId, since));
        return dto;
    }

    private int countActiveKeys(String tenantId) {
        return (int) tenantApiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .filter(k -> k.getStatus() == ApiKeyStatus.ACTIVE)
            .count();
    }

    private static boolean matchesEnvironmentFilter(TenantApiKey k, String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        return k.getEnvironment().name().equalsIgnoreCase(filter)
            || (filter.equalsIgnoreCase("sandbox") && k.getEnvironment() == ApiKeyEnvironment.SANDBOX)
            || (filter.equalsIgnoreCase("production") && k.getEnvironment() == ApiKeyEnvironment.PRODUCTION);
    }

    private void ensureTenant(String tenantId) {
        tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static int average(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        return (int) values.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static int percentile(List<Integer> values, int pct) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int index = Math.min(sorted.size() - 1, (int) Math.ceil(pct / 100.0 * sorted.size()) - 1);
        return sorted.get(Math.max(0, index));
    }
}

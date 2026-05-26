package com.loyaltyos.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.entity.ProgrammeConfig;
import com.loyaltyos.onboarding.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.entity.SandboxTestEvent;
import com.loyaltyos.onboarding.entity.TenantApiKey;
import com.loyaltyos.onboarding.entity.TenantOnboarding;
import com.loyaltyos.onboarding.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.enums.ApiKeyStatus;
import com.loyaltyos.onboarding.enums.OnboardingStatus;
import com.loyaltyos.onboarding.dto.SandboxValidateEventRequest;
import com.loyaltyos.rules.dto.RuleEvaluateRequest;
import com.loyaltyos.rules.dto.RuleEvaluationResponse;
import com.loyaltyos.rules.service.RuleEvaluationService;
import com.loyaltyos.onboarding.dto.ApiKeyGeneratedResponse;
import com.loyaltyos.onboarding.dto.ApiKeySummaryResponse;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.SandboxTestEventRepository;
import com.loyaltyos.onboarding.repository.TenantApiKeyRepository;
import com.loyaltyos.onboarding.repository.TenantConfigRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.integration.service.IntegrationCredentialCryptoService;
import com.loyaltyos.integration.service.IntegrationEventPayloadResolver;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    /** Statuses allowed to create or rotate integration API keys (dashboard + onboarding). */
    private static final Set<OnboardingStatus> KEY_GENERATION_ALLOWED = Set.of(
        OnboardingStatus.RULES_CONFIGURED,
        OnboardingStatus.SANDBOX_TESTING,
        OnboardingStatus.ACTIVE
    );

    private final TenantOnboardingRepository tenantOnboardingRepository;
    private final TenantApiKeyRepository tenantApiKeyRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final OnboardingStateMachine stateMachine;
    private final ObjectMapper objectMapper;
    private final RuleEvaluationService ruleEvaluationService;
    private final SandboxTestEventRepository sandboxTestEventRepository;
    private final ProgrammeService programmeService;
    private final IntegrationCredentialCryptoService credentialCryptoService;
    private final IntegrationEventPayloadResolver integrationEventPayloadResolver;

    private final SecureRandom secureRandom = new SecureRandom();

    public IntegrationService(
        TenantOnboardingRepository tenantOnboardingRepository,
        TenantApiKeyRepository tenantApiKeyRepository,
        TenantConfigRepository tenantConfigRepository,
        OnboardingAuditLogRepository auditLogRepository,
        OnboardingStateMachine stateMachine,
        ObjectMapper objectMapper,
        RuleEvaluationService ruleEvaluationService,
        SandboxTestEventRepository sandboxTestEventRepository,
        ProgrammeService programmeService,
        IntegrationCredentialCryptoService credentialCryptoService,
        IntegrationEventPayloadResolver integrationEventPayloadResolver
    ) {
        this.tenantOnboardingRepository = Objects.requireNonNull(tenantOnboardingRepository, "tenantOnboardingRepository");
        this.tenantApiKeyRepository = Objects.requireNonNull(tenantApiKeyRepository, "tenantApiKeyRepository");
        this.tenantConfigRepository = Objects.requireNonNull(tenantConfigRepository, "tenantConfigRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.ruleEvaluationService = Objects.requireNonNull(ruleEvaluationService, "ruleEvaluationService");
        this.sandboxTestEventRepository = Objects.requireNonNull(sandboxTestEventRepository, "sandboxTestEventRepository");
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.credentialCryptoService = Objects.requireNonNull(credentialCryptoService, "credentialCryptoService");
        this.integrationEventPayloadResolver = Objects.requireNonNull(
            integrationEventPayloadResolver, "integrationEventPayloadResolver");
    }

    public ApiKeyGeneratedResponse generateSandboxKeysLegacy(String tenantId) {
        // Legacy endpoint only creates SANDBOX key + secret.
        return generateKeys(tenantId, ApiKeyEnvironment.SANDBOX);
    }

    @Transactional
    public ApiKeyGeneratedResponse generateKeys(String tenantId, ApiKeyEnvironment env) {
        TenantOnboarding tenant = tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (!KEY_GENERATION_ALLOWED.contains(tenant.getOnboardingStatus())) {
            throw new com.loyaltyos.onboarding.exception.InvalidStateException(
                "Complete rules setup (or activate your programme) before generating API keys",
                tenant.getOnboardingStatus().name(),
                OnboardingStatus.RULES_CONFIGURED.name()
            );
        }

        // Revoke existing ACTIVE keys for this env (rotation-like behavior).
        List<TenantApiKey> active = tenantApiKeyRepository.findByTenantIdAndEnvironmentAndStatus(
            tenantId, env, ApiKeyStatus.ACTIVE);
        for (TenantApiKey k : active) {
            k.setStatus(ApiKeyStatus.REVOKED);
            k.setRevokedAt(Instant.now());
        }
        if (!active.isEmpty()) {
            tenantApiKeyRepository.saveAll(active);
        }

        String rawKey = generateRawKey(env);
        String rawSecret = randomHex(32); // 64 hex chars

        String keyHash = sha256Hex(rawKey);
        String secretHash = sha256Hex(rawSecret);
        String prefix = rawKey.substring(0, Math.min(16, rawKey.length()));

        TenantApiKey saved = tenantApiKeyRepository.save(TenantApiKey.builder()
            .tenantId(tenantId)
            .keyUid(UUID.randomUUID().toString())
            .keyPrefix(prefix)
            .keyHash(keyHash)
            .signingSecretHash(secretHash)
            .signingSecretEncrypted(credentialCryptoService.encrypt(rawSecret))
            .environment(env)
            .status(ApiKeyStatus.ACTIVE)
            .build());

        // Transition to SANDBOX_TESTING if we are coming from RULES_CONFIGURED
        if (tenant.getOnboardingStatus() == OnboardingStatus.RULES_CONFIGURED) {
            stateMachine.transition(tenant, OnboardingStatus.SANDBOX_TESTING, tenantId, "TENANT");
            tenantOnboardingRepository.save(tenant);
        }

        auditLogRepository.save(OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action(env == ApiKeyEnvironment.SANDBOX ? "SANDBOX_KEYS_GENERATED" : "PROD_KEYS_GENERATED")
            .actorId(tenantId)
            .actorRole("TENANT")
            .afterState(Map.of("environment", env.name(), "keyUid", saved.getKeyUid()))
            .build());

        return ApiKeyGeneratedResponse.builder()
            .keyUid(saved.getKeyUid())
            .apiKey(rawKey)
            .signingSecret(rawSecret)
            .environment(env)
            .keyPrefix(prefix)
            .message("Save these keys now — they will not be shown again.")
            .build();
    }

    @Transactional(readOnly = true)
    public List<ApiKeySummaryResponse> getKeySummaries(String tenantId) {
        tenantOnboardingRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        List<TenantApiKey> sandbox = tenantApiKeyRepository.findByTenantIdAndEnvironmentAndStatus(
            tenantId, ApiKeyEnvironment.SANDBOX, ApiKeyStatus.ACTIVE);
        List<TenantApiKey> prod = tenantApiKeyRepository.findByTenantIdAndEnvironmentAndStatus(
            tenantId, ApiKeyEnvironment.PRODUCTION, ApiKeyStatus.ACTIVE);

        return java.util.stream.Stream.concat(sandbox.stream(), prod.stream())
            .map(k -> ApiKeySummaryResponse.builder()
                .environment(k.getEnvironment())
                .keyUid(k.getKeyUid())
                .keyPrefix(k.getKeyPrefix())
                .createdAt(k.getCreatedAt())
                .lastUsedAt(k.getLastUsedAt())
                .build())
            .toList();
    }

    @Transactional(noRollbackFor = com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException.class)
    public Map<String, Object> validateSandboxEvent(String tenantId, SandboxValidateEventRequest request) {
        tenantConfigRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // Parse JSON, then validate required keys against programme_config (eventSchema) when present.
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(request.getPayloadJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException(
                "Invalid JSON payload", Map.of("payloadJson", "Must be valid JSON object"));
        }

        String programmeUid = resolveProgrammeUid(payload);
        Map<String, String> errors = new LinkedHashMap<>();
        boolean programmeSchemaPresent = false;
        try {
            ProgrammeConfig programmeCfg = programmeService.getActiveConfigOrNull(tenantId, programmeUid);
            if (programmeCfg != null && programmeCfg.getConfigJson() != null && !programmeCfg.getConfigJson().isBlank()) {
                programmeSchemaPresent = true;
                JsonNode root = objectMapper.readTree(programmeCfg.getConfigJson());
                errors.putAll(EventSchemaPayloadValidator.validatePayload(payload, root));
            } else {
                if (!payload.containsKey("eventType")) {
                    errors.put("eventType", "eventType is required");
                }
                if (!payload.containsKey("timestamp")) {
                    errors.put("timestamp", "timestamp is required");
                }
                if (!payload.containsKey("transactionId")) {
                    errors.put("transactionId", "transactionId is required");
                }
            }
        } catch (JsonProcessingException e) {
            errors.put("programmeConfig", "Unable to read programme configuration");
        }

        if (!errors.isEmpty()) {
            throw new com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException(
                "Event payload validation failed", errors);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "VALID");
        result.put("tenantId", tenantId);
        result.put("payload", payload);
        result.put("schemaPresent", programmeSchemaPresent);

        try {
            RuleEvaluateRequest ruleReq = integrationEventPayloadResolver.buildRuleEvaluateRequest(payload);
            RuleEvaluationResponse ruleRes = request.getRuleUid() != null && !request.getRuleUid().isBlank()
                ? ruleEvaluationService.evaluateSingleRule(tenantId, request.getRuleUid(), ruleReq)
                : ruleEvaluationService.evaluate(tenantId, ruleReq);
            result.put("ruleEvaluation", objectMapper.convertValue(ruleRes, new TypeReference<Map<String, Object>>() {}));
        } catch (IllegalArgumentException e) {
            log.debug("Sandbox rule evaluation skipped: {}", e.getMessage());
            result.put("ruleEvaluationError", e.getMessage());
        } catch (Exception e) {
            log.debug("Sandbox rule evaluation failed: {}", e.getMessage());
            result.put("ruleEvaluationError", e.getMessage());
        }

        persistSandboxTestEvent(tenantId, payload, result);
        return result;
    }

    private static String resolveProgrammeUid(Map<String, Object> payload) {
        Object p = payload.get("programmeUid");
        if (p != null && !String.valueOf(p).isBlank()) {
            return String.valueOf(p);
        }
        return "default";
    }

    private void persistSandboxTestEvent(
        String tenantId,
        Map<String, Object> payload,
        Map<String, Object> result
    ) {
        Map<String, Object> responseForDb = new LinkedHashMap<>();
        responseForDb.put("status", result.get("status"));
        responseForDb.put("tenantId", result.get("tenantId"));
        responseForDb.put("schemaPresent", result.get("schemaPresent"));
        if (result.containsKey("ruleEvaluation")) {
            responseForDb.put("ruleEvaluation", result.get("ruleEvaluation"));
        }
        if (result.containsKey("ruleEvaluationError")) {
            responseForDb.put("ruleEvaluationError", result.get("ruleEvaluationError"));
        }

        try {
            sandboxTestEventRepository.save(SandboxTestEvent.builder()
                .tenantId(tenantId)
                .programmeUid(resolveProgrammeUid(payload))
                .transactionId(String.valueOf(payload.get("transactionId")))
                .requestPayloadJson(new LinkedHashMap<>(payload))
                .responseJson(responseForDb)
                .build());
        } catch (Exception e) {
            log.warn("Failed to persist sandbox_test_events for tenant {}: {}", tenantId, e.getMessage());
        }
    }

    private String generateRawKey(ApiKeyEnvironment env) {
        String prefix = env == ApiKeyEnvironment.PRODUCTION ? "los_live_" : "los_sandbox_";
        return prefix + randomHex(16); // 32 hex chars
    }

    private String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        secureRandom.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @SuppressWarnings("unused")
    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }
}


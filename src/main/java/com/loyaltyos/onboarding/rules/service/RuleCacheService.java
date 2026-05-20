package com.loyaltyos.onboarding.rules.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.onboarding.rules.cache.CachedActionSnapshot;
import com.loyaltyos.onboarding.rules.cache.CachedRuleSnapshot;
import com.loyaltyos.onboarding.rules.config.RulesProperties;
import com.loyaltyos.onboarding.rules.entity.EarnRule;
import com.loyaltyos.onboarding.rules.entity.RuleAction;
import com.loyaltyos.onboarding.rules.entity.RuleCondition;
import com.loyaltyos.onboarding.rules.enums.ExecutionMode;
import com.loyaltyos.onboarding.rules.enums.RuleType;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RuleCacheService {

    private static final Logger log = LoggerFactory.getLogger(RuleCacheService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RulesProperties rulesProperties;

    public RuleCacheService(
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper,
        RulesProperties rulesProperties
    ) {
        this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate, "stringRedisTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.rulesProperties = Objects.requireNonNull(rulesProperties, "rulesProperties");
    }

    public Optional<List<CachedRuleSnapshot>> get(String tenantId, String programmeUid, String eventType) {
        if (!rulesProperties.isCacheEnabled()) {
            return Optional.empty();
        }
        String key = cacheKey(tenantId, programmeUid, eventType);
        try {
            String json = stringRedisTemplate.opsForValue().get(Objects.requireNonNull(key, "key"));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            List<CachedRuleSnapshot> list = objectMapper.readValue(json, new TypeReference<>() {});
            return Optional.ofNullable(list);
        } catch (Exception e) {
            log.warn("Rule cache read failed for {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String tenantId, String programmeUid, String eventType, List<EarnRule> rules) {
        if (!rulesProperties.isCacheEnabled()) {
            return;
        }
        String key = cacheKey(tenantId, programmeUid, eventType);
        try {
            List<CachedRuleSnapshot> snapshots = rules.stream().map(RuleCacheService::toSnapshot).toList();
            String json = objectMapper.writeValueAsString(snapshots);
            Duration ttl = Duration.ofSeconds(rulesProperties.getCacheTtlSeconds());
            stringRedisTemplate.opsForValue().set(
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(json, "json"),
                Objects.requireNonNull(ttl, "ttl")
            );
        } catch (Exception e) {
            log.warn("Rule cache write failed for {}: {}", key, e.getMessage());
        }
    }

    /**
     * Invalidates all cached rule lists for a tenant + programme (all event types).
     * Uses KEYS — acceptable for dev/small Redis; prefer SCAN in high-volume production.
     */
    public void invalidateProgramme(String tenantId, String programmeUid) {
        String pattern = "rules:v1:" + tenantId + ":" + programmeUid + ":*";
        try {
            var keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.info("Invalidated {} rule cache keys for tenant={} programme={}", keys.size(), tenantId, programmeUid);
            }
        } catch (Exception e) {
            log.warn("Rule cache invalidation failed: {}", e.getMessage());
        }
    }

    private static String cacheKey(String tenantId, String programmeUid, String eventType) {
        return "rules:v1:" + tenantId + ":" + programmeUid + ":" + eventType;
    }

    private static CachedRuleSnapshot toSnapshot(EarnRule r) {
        RuleCondition c = r.getCondition();
        List<CachedActionSnapshot> acts = new ArrayList<>();
        if (r.getActions() != null) {
            for (RuleAction a : r.getActions()) {
                acts.add(CachedActionSnapshot.builder()
                    .actionUid(a.getActionUid())
                    .actionType(a.getActionType() != null ? a.getActionType().name() : null)
                    .formula(a.getFormula())
                    .build());
            }
        }
        return CachedRuleSnapshot.builder()
            .ruleDbId(r.getId() != null ? r.getId() : 0L)
            .ruleUid(r.getRuleUid())
            .name(r.getName())
            .priority(r.getPriority() != null ? r.getPriority() : 0)
            .executionMode(
                r.getExecutionMode() != null ? r.getExecutionMode().name() : ExecutionMode.ALL_MATCHING.name())
            .ruleType(r.getRuleType() != null ? r.getRuleType().name() : RuleType.PROGRAMME.name())
            .campaignUid(r.getCampaignUid())
            .triggerEventType(r.getTriggerEventType())
            .conditionTree(c != null ? c.getConditionTree() : null)
            .actions(acts)
            .build();
    }

    /** Visible for RuleEvaluationService when bypassing entity graph after cache hit. */
    public static List<CachedRuleSnapshot> snapshotsFromEntities(List<EarnRule> rules) {
        return rules.stream().map(RuleCacheService::toSnapshot).toList();
    }
}

package com.loyaltyos.onboarding.rules.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loyalty.rules")
public class RulesProperties {

    /**
     * When false, rule lists are always loaded from the database.
     */
    private boolean cacheEnabled = true;

    private int cacheTtlSeconds = 300;
    private int maxConditionDepth = 32;
    private int evaluationTimeoutMs = 5000;

    /**
     * When false, daily/monthly cap keys are not read from Redis (caps are informational only).
     */
    private boolean capsRedisEnabled = false;

    public RulesProperties() {}

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public int getMaxConditionDepth() {
        return maxConditionDepth;
    }

    public void setMaxConditionDepth(int maxConditionDepth) {
        this.maxConditionDepth = maxConditionDepth;
    }

    public int getEvaluationTimeoutMs() {
        return evaluationTimeoutMs;
    }

    public void setEvaluationTimeoutMs(int evaluationTimeoutMs) {
        this.evaluationTimeoutMs = evaluationTimeoutMs;
    }

    public boolean isCapsRedisEnabled() {
        return capsRedisEnabled;
    }

    public void setCapsRedisEnabled(boolean capsRedisEnabled) {
        this.capsRedisEnabled = capsRedisEnabled;
    }
}

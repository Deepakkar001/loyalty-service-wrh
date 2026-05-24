package com.loyaltyos.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loyaltyos.integration")
public class IntegrationProperties {

    private String encryptionKey = "";
    private RateLimit rateLimit = new RateLimit();
    private Idempotency idempotency = new Idempotency();

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public void setIdempotency(Idempotency idempotency) {
        this.idempotency = idempotency;
    }

    public static class RateLimit {
        private boolean redisEnabled = true;
        private int defaultRequestsPerHour = 1000;

        public boolean isRedisEnabled() {
            return redisEnabled;
        }

        public void setRedisEnabled(boolean redisEnabled) {
            this.redisEnabled = redisEnabled;
        }

        public int getDefaultRequestsPerHour() {
            return defaultRequestsPerHour;
        }

        public void setDefaultRequestsPerHour(int defaultRequestsPerHour) {
            this.defaultRequestsPerHour = defaultRequestsPerHour;
        }
    }

    public static class Idempotency {
        private int cacheTtlHours = 24;

        public int getCacheTtlHours() {
            return cacheTtlHours;
        }

        public void setCacheTtlHours(int cacheTtlHours) {
            this.cacheTtlHours = cacheTtlHours;
        }
    }
}

package com.loyaltyos.onboarding.exception;

public class KafkaProvisioningException extends RuntimeException {
    public KafkaProvisioningException(String tenantId, Throwable cause) {
        super("Failed to provision Kafka topics for tenant: " + tenantId, cause);
    }
}


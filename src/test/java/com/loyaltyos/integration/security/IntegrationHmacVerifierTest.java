package com.loyaltyos.integration.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationHmacVerifierTest {

    @Test
    void verifySignature_validHexWithPrefix() {
        String body = "{\"eventId\":\"evt_1\"}";
        String secret = "test-secret";
        String sig = "sha256=" + IntegrationHmacVerifier.hmacSha256Hex(body, secret);
        assertTrue(IntegrationHmacVerifier.verifySignature(body, sig, secret));
    }

    @Test
    void verifySignature_invalidSecret() {
        String body = "{}";
        String sig = "sha256=" + IntegrationHmacVerifier.hmacSha256Hex(body, "a");
        assertFalse(IntegrationHmacVerifier.verifySignature(body, sig, "b"));
    }

    @Test
    void sha256Hex_deterministic() {
        assertEquals(
            IntegrationHmacVerifier.sha256Hex("los_test"),
            IntegrationHmacVerifier.sha256Hex("los_test")
        );
    }
}

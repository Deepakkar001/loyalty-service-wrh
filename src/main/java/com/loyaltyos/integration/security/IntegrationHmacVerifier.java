package com.loyaltyos.integration.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class IntegrationHmacVerifier {

    private IntegrationHmacVerifier() {}

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    public static boolean verifySignature(String payload, String providedHeader, String secret) {
        if (providedHeader == null || providedHeader.isBlank()) {
            return false;
        }
        String expected = hmacSha256Hex(payload, secret);
        String provided = providedHeader.trim();
        if (provided.startsWith("sha256=")) {
            provided = provided.substring("sha256=".length());
        }
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            provided.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }
}

package com.loyaltyos.voucher.support;

import com.loyaltyos.integration.security.IntegrationHmacVerifier;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

public final class VoucherCodeNormalizer {

    private VoucherCodeNormalizer() {}

    public static String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public static String sha256Hex(String value) {
        return IntegrationHmacVerifier.sha256Hex(value);
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

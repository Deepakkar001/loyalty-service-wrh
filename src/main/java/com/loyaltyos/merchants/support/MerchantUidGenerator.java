package com.loyaltyos.merchants.support;

import java.util.UUID;

public final class MerchantUidGenerator {
    private MerchantUidGenerator() {}

    public static String generate() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MCH_" + suffix;
    }
}

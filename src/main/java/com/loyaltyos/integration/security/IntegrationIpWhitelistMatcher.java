package com.loyaltyos.integration.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.util.List;

public final class IntegrationIpWhitelistMatcher {

    private IntegrationIpWhitelistMatcher() {}

    public static boolean isAllowed(String ipWhitelistJson, String clientIp, ObjectMapper objectMapper) {
        if (ipWhitelistJson == null || ipWhitelistJson.isBlank()) {
            return true;
        }
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        try {
            List<String> entries = objectMapper.readValue(ipWhitelistJson, new TypeReference<List<String>>() {});
            if (entries == null || entries.isEmpty()) {
                return true;
            }
            for (String entry : entries) {
                if (entry != null && matchesEntry(clientIp.trim(), entry.trim())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchesEntry(String clientIp, String entry) {
        if (entry.equals(clientIp)) {
            return true;
        }
        if (entry.contains("/")) {
            return matchesCidr(clientIp, entry);
        }
        return false;
    }

    private static boolean matchesCidr(String clientIp, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            InetAddress target = InetAddress.getByName(clientIp);
            InetAddress network = InetAddress.getByName(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            byte[] targetBytes = target.getAddress();
            byte[] networkBytes = network.getAddress();
            if (targetBytes.length != networkBytes.length) {
                return false;
            }
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (targetBytes[i] != networkBytes[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (targetBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        } catch (Exception e) {
            return false;
        }
    }
}

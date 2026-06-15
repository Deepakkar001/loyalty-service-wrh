package com.loyaltyos.access.security;

import java.util.regex.Pattern;

public final class TenantPasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private TenantPasswordPolicy() {}

    public static void validateNewPassword(String newPassword, String userEmail, String currentPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        if (newPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (!UPPER.matcher(newPassword).find()) {
            throw new IllegalArgumentException("Password must include an uppercase letter");
        }
        if (!LOWER.matcher(newPassword).find()) {
            throw new IllegalArgumentException("Password must include a lowercase letter");
        }
        if (!DIGIT.matcher(newPassword).find()) {
            throw new IllegalArgumentException("Password must include a number");
        }
        if (!SPECIAL.matcher(newPassword).find()) {
            throw new IllegalArgumentException("Password must include a special character");
        }
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from your current password");
        }
        if (userEmail != null && !userEmail.isBlank()) {
            String local = userEmail.toLowerCase().split("@")[0];
            if (local.length() >= 3 && newPassword.toLowerCase().contains(local)) {
                throw new IllegalArgumentException("Password must not contain your email address");
            }
        }
    }
}

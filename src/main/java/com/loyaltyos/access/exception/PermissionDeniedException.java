package com.loyaltyos.access.exception;

public class PermissionDeniedException extends RuntimeException {

    private final String permissionKey;

    public PermissionDeniedException(String permissionKey) {
        super("Permission denied: " + permissionKey);
        this.permissionKey = permissionKey;
    }

    public String getPermissionKey() {
        return permissionKey;
    }
}

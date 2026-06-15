package com.loyaltyos.access.security;

/**
 * Maps tenant API requests to the minimum permission key required for the action.
 */
public final class TenantModulePermissionResolver {

    private TenantModulePermissionResolver() {}

    public static String resolve(String moduleKey, String httpMethod, String path) {
        String method = httpMethod != null ? httpMethod.toUpperCase() : "GET";
        return switch (method) {
            case "GET", "HEAD" -> resolveReadPermission(moduleKey, path);
            case "POST" -> resolvePostPermission(moduleKey, path);
            case "PUT", "PATCH" -> moduleKey + ".edit";
            case "DELETE" -> moduleKey + ".delete";
            default -> moduleKey + ".view";
        };
    }

    /**
     * POST is usually "create", but some tenant APIs use POST for read/update semantics.
     */
    private static String resolvePostPermission(String moduleKey, String path) {
        if (path != null) {
            String normalized = path.toLowerCase();
            if (normalized.endsWith("/privileges/load")) {
                return "team_admin.view";
            }
            if (normalized.endsWith("/privileges/assign")) {
                return "team_admin.edit";
            }
            if (normalized.contains("/users/") && normalized.endsWith("/disable")) {
                return "team_admin.delete";
            }
        }
        return moduleKey + ".create";
    }

    private static String resolveReadPermission(String moduleKey, String path) {
        if (path != null) {
            String normalized = path.toLowerCase();
            if (normalized.contains("/export") || normalized.endsWith("/export")) {
                return moduleKey + ".export";
            }
            if (normalized.contains("/approve") || normalized.contains("/fraud")) {
                return moduleKey + ".approve";
            }
            if (normalized.contains("/publish")) {
                return moduleKey + ".publish";
            }
        }
        return moduleKey + ".view";
    }
}

package com.loyaltyos.access.dto;

public class RouteGuardDto {

    private String path;
    private String permissionKey;

    public RouteGuardDto() {}

    public RouteGuardDto(String path, String permissionKey) {
        this.path = path;
        this.permissionKey = permissionKey;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
}

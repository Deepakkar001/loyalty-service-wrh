package com.loyaltyos.access.dto;

import java.util.List;

public class MeAccessResponse {

    private String tenantId;
    private String tenantUserId;
    private int sessionVersion;
    private List<String> permissions;
    private List<String> entitledModules;
    private List<NavGroupDto> navGroups;
    private List<RouteGuardDto> routeGuards;
    private boolean modulesConfigured;
    private boolean dynamicNavEnabled;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantUserId() { return tenantUserId; }
    public void setTenantUserId(String tenantUserId) { this.tenantUserId = tenantUserId; }
    public int getSessionVersion() { return sessionVersion; }
    public void setSessionVersion(int sessionVersion) { this.sessionVersion = sessionVersion; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public List<String> getEntitledModules() { return entitledModules; }
    public void setEntitledModules(List<String> entitledModules) { this.entitledModules = entitledModules; }
    public List<NavGroupDto> getNavGroups() { return navGroups; }
    public void setNavGroups(List<NavGroupDto> navGroups) { this.navGroups = navGroups; }
    public List<RouteGuardDto> getRouteGuards() { return routeGuards; }
    public void setRouteGuards(List<RouteGuardDto> routeGuards) { this.routeGuards = routeGuards; }
    public boolean isModulesConfigured() { return modulesConfigured; }
    public void setModulesConfigured(boolean modulesConfigured) { this.modulesConfigured = modulesConfigured; }
    public boolean isDynamicNavEnabled() { return dynamicNavEnabled; }
    public void setDynamicNavEnabled(boolean dynamicNavEnabled) { this.dynamicNavEnabled = dynamicNavEnabled; }
}

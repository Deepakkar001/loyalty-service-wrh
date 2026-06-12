package com.loyaltyos.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_nav_item")
public class AccessNavItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_key", nullable = false, length = 64)
    private String moduleKey;

    @Column(name = "route_path", nullable = false, length = 256)
    private String routePath;

    @Column(name = "label_key", nullable = false, length = 128)
    private String labelKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "required_permission_key", nullable = false, length = 128)
    private String requiredPermissionKey;

    @Column(name = "requires_onboarding_complete", nullable = false)
    private boolean requiresOnboardingComplete = true;

    @Column(name = "icon_key", length = 64)
    private String iconKey;

    public AccessNavItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getRoutePath() { return routePath; }
    public void setRoutePath(String routePath) { this.routePath = routePath; }
    public String getLabelKey() { return labelKey; }
    public void setLabelKey(String labelKey) { this.labelKey = labelKey; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getRequiredPermissionKey() { return requiredPermissionKey; }
    public void setRequiredPermissionKey(String requiredPermissionKey) { this.requiredPermissionKey = requiredPermissionKey; }
    public boolean isRequiresOnboardingComplete() { return requiresOnboardingComplete; }
    public void setRequiresOnboardingComplete(boolean requiresOnboardingComplete) { this.requiresOnboardingComplete = requiresOnboardingComplete; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
}

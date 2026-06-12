package com.loyaltyos.access.entity;

import com.loyaltyos.onboarding.enums.SubscriptionTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_module")
public class AccessModule {

    @Id
    @Column(name = "module_key", length = 64)
    private String moduleKey;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "nav_section", length = 64)
    private String navSection;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "icon_key", length = 64)
    private String iconKey;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_subscription_tier", length = 32)
    private SubscriptionTier minSubscriptionTier;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public AccessModule() {}

    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getNavSection() { return navSection; }
    public void setNavSection(String navSection) { this.navSection = navSection; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public SubscriptionTier getMinSubscriptionTier() { return minSubscriptionTier; }
    public void setMinSubscriptionTier(SubscriptionTier minSubscriptionTier) { this.minSubscriptionTier = minSubscriptionTier; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

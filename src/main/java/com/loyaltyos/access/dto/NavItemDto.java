package com.loyaltyos.access.dto;

public class NavItemDto {

    private String href;
    private String label;
    private String iconKey;
    private String moduleKey;
    private boolean requiresOnboardingComplete;

    public NavItemDto() {}

    public NavItemDto(String href, String label, String iconKey, String moduleKey, boolean requiresOnboardingComplete) {
        this.href = href;
        this.label = label;
        this.iconKey = iconKey;
        this.moduleKey = moduleKey;
        this.requiresOnboardingComplete = requiresOnboardingComplete;
    }

    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public boolean isRequiresOnboardingComplete() { return requiresOnboardingComplete; }
    public void setRequiresOnboardingComplete(boolean requiresOnboardingComplete) { this.requiresOnboardingComplete = requiresOnboardingComplete; }
}

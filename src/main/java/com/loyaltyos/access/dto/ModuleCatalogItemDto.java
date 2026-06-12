package com.loyaltyos.access.dto;

public class ModuleCatalogItemDto {

    private String moduleKey;
    private String displayName;
    private String description;
    private boolean required;
    private boolean inTierBaseline;
    private boolean preSelected;
    private boolean locked;
    private Boolean enabled;

    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isInTierBaseline() { return inTierBaseline; }
    public void setInTierBaseline(boolean inTierBaseline) { this.inTierBaseline = inTierBaseline; }
    public boolean isPreSelected() { return preSelected; }
    public void setPreSelected(boolean preSelected) { this.preSelected = preSelected; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

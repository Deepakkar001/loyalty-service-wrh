package com.loyaltyos.access.dto;

import java.util.List;

public class ModuleCatalogResponse {

    private String tier;
    private List<String> required;
    private List<String> tierBaseline;
    private List<ModuleCatalogItemDto> modules;
    private boolean modulesConfigured;

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public List<String> getRequired() { return required; }
    public void setRequired(List<String> required) { this.required = required; }
    public List<String> getTierBaseline() { return tierBaseline; }
    public void setTierBaseline(List<String> tierBaseline) { this.tierBaseline = tierBaseline; }
    public List<ModuleCatalogItemDto> getModules() { return modules; }
    public void setModules(List<ModuleCatalogItemDto> modules) { this.modules = modules; }
    public boolean isModulesConfigured() { return modulesConfigured; }
    public void setModulesConfigured(boolean modulesConfigured) { this.modulesConfigured = modulesConfigured; }
}

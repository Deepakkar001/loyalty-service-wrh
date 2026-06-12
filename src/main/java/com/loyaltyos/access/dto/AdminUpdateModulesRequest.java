package com.loyaltyos.access.dto;

import java.util.List;

public class AdminUpdateModulesRequest {

    private List<String> enabledModuleKeys;

    public List<String> getEnabledModuleKeys() { return enabledModuleKeys; }
    public void setEnabledModuleKeys(List<String> enabledModuleKeys) { this.enabledModuleKeys = enabledModuleKeys; }
}

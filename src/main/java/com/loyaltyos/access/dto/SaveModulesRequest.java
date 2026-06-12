package com.loyaltyos.access.dto;

import java.util.List;

public class SaveModulesRequest {

    private List<String> selectedModuleKeys;

    public List<String> getSelectedModuleKeys() { return selectedModuleKeys; }
    public void setSelectedModuleKeys(List<String> selectedModuleKeys) { this.selectedModuleKeys = selectedModuleKeys; }
}

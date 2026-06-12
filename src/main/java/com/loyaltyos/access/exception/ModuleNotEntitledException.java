package com.loyaltyos.access.exception;

public class ModuleNotEntitledException extends RuntimeException {

    private final String moduleKey;

    public ModuleNotEntitledException(String moduleKey) {
        super("Module not entitled: " + moduleKey);
        this.moduleKey = moduleKey;
    }

    public String getModuleKey() {
        return moduleKey;
    }
}

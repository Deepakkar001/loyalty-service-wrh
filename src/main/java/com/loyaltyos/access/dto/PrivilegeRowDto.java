package com.loyaltyos.access.dto;

public class PrivilegeRowDto {
    private String moduleKey;
    private String moduleName;
    private String navSection;
    private String actionKey;
    private String permissionKey;
    private boolean assignable;
    private boolean selected;
    private boolean inheritedFromRole;
    private boolean denied;

    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getNavSection() { return navSection; }
    public void setNavSection(String navSection) { this.navSection = navSection; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
    public boolean isAssignable() { return assignable; }
    public void setAssignable(boolean assignable) { this.assignable = assignable; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isInheritedFromRole() { return inheritedFromRole; }
    public void setInheritedFromRole(boolean inheritedFromRole) { this.inheritedFromRole = inheritedFromRole; }
    public boolean isDenied() { return denied; }
    public void setDenied(boolean denied) { this.denied = denied; }
}

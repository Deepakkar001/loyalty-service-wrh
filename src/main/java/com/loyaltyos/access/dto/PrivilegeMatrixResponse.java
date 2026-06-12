package com.loyaltyos.access.dto;

import java.util.List;

public class PrivilegeMatrixResponse {
    private String roleId;
    private String userId;
    private List<String> actionKeys;
    private List<PrivilegeRowDto> rows;

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<String> getActionKeys() { return actionKeys; }
    public void setActionKeys(List<String> actionKeys) { this.actionKeys = actionKeys; }
    public List<PrivilegeRowDto> getRows() { return rows; }
    public void setRows(List<PrivilegeRowDto> rows) { this.rows = rows; }
}

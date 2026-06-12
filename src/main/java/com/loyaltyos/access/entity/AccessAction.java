package com.loyaltyos.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_action")
public class AccessAction {

    @Id
    @Column(name = "action_key", length = 32)
    private String actionKey;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public AccessAction() {}

    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}

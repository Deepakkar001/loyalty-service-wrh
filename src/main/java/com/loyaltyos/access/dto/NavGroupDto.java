package com.loyaltyos.access.dto;

import java.util.List;

public class NavGroupDto {

    private String label;
    private List<NavItemDto> items;

    public NavGroupDto() {}

    public NavGroupDto(String label, List<NavItemDto> items) {
        this.label = label;
        this.items = items;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public List<NavItemDto> getItems() { return items; }
    public void setItems(List<NavItemDto> items) { this.items = items; }
}

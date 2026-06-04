package com.loyaltyos.campaigns.dto;

public class CampaignTargetUploadColumnSpec {

    private String name;
    private boolean required;
    private String dataType;
    private String description;
    private String example;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }
}

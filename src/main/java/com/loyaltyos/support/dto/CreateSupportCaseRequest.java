package com.loyaltyos.support.dto;

import com.loyaltyos.support.enums.SupportCaseCategory;
import com.loyaltyos.support.enums.SupportCasePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSupportCaseRequest {

    @NotNull
    private SupportCaseCategory category;

    @NotNull
    private SupportCasePriority priority = SupportCasePriority.NORMAL;

    @NotBlank
    @Size(max = 200)
    private String subject;

    @NotBlank
    @Size(max = 5000)
    private String description;

    @Size(max = 128)
    private String correlationId;

    @Size(max = 64)
    private String programmeUid;

    @Size(max = 500)
    private String pageUrl;

    public SupportCaseCategory getCategory() {
        return category;
    }

    public void setCategory(SupportCaseCategory category) {
        this.category = category;
    }

    public SupportCasePriority getPriority() {
        return priority;
    }

    public void setPriority(SupportCasePriority priority) {
        this.priority = priority;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getProgrammeUid() {
        return programmeUid;
    }

    public void setProgrammeUid(String programmeUid) {
        this.programmeUid = programmeUid;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }
}

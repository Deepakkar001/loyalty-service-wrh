package com.loyaltyos.support.dto;

import java.util.ArrayList;
import java.util.List;

public class SupportContextResponse {

    private String tenantId;
    private String companyName;
    private String subscriptionTier;
    private String userEmail;
    private String slaResponseHint;
    private List<IntegrationErrorSnippet> recentIntegrationErrors = new ArrayList<>();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getSlaResponseHint() {
        return slaResponseHint;
    }

    public void setSlaResponseHint(String slaResponseHint) {
        this.slaResponseHint = slaResponseHint;
    }

    public List<IntegrationErrorSnippet> getRecentIntegrationErrors() {
        return recentIntegrationErrors;
    }

    public void setRecentIntegrationErrors(List<IntegrationErrorSnippet> recentIntegrationErrors) {
        this.recentIntegrationErrors = recentIntegrationErrors;
    }

    public static class IntegrationErrorSnippet {
        private String requestId;
        private String httpMethod;
        private String requestPath;
        private int httpStatus;
        private String errorCode;
        private String errorMessage;
        private String createdAt;

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getRequestPath() {
            return requestPath;
        }

        public void setRequestPath(String requestPath) {
            this.requestPath = requestPath;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public void setHttpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}

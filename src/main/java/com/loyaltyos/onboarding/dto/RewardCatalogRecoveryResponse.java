package com.loyaltyos.onboarding.dto;

public class RewardCatalogRecoveryResponse {

    private boolean recoverable;
    private Integer sourceConfigVersion;
    private int itemCount;
    private int voucherItemCount;
    private String message;

    public RewardCatalogRecoveryResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean recoverable;
        private Integer sourceConfigVersion;
        private int itemCount;
        private int voucherItemCount;
        private String message;

        private Builder() {}

        public Builder recoverable(boolean recoverable) {
            this.recoverable = recoverable;
            return this;
        }

        public Builder sourceConfigVersion(Integer sourceConfigVersion) {
            this.sourceConfigVersion = sourceConfigVersion;
            return this;
        }

        public Builder itemCount(int itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public Builder voucherItemCount(int voucherItemCount) {
            this.voucherItemCount = voucherItemCount;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public RewardCatalogRecoveryResponse build() {
            RewardCatalogRecoveryResponse r = new RewardCatalogRecoveryResponse();
            r.recoverable = recoverable;
            r.sourceConfigVersion = sourceConfigVersion;
            r.itemCount = itemCount;
            r.voucherItemCount = voucherItemCount;
            r.message = message;
            return r;
        }
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public Integer getSourceConfigVersion() {
        return sourceConfigVersion;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getVoucherItemCount() {
        return voucherItemCount;
    }

    public String getMessage() {
        return message;
    }
}

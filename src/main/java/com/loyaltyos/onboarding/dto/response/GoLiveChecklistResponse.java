package com.loyaltyos.onboarding.dto.response;

import java.util.List;

public class GoLiveChecklistResponse {
    private boolean canGoLive;
    private List<Item> items;

    public static class Item {
        private String item;
        private String status; // COMPLETE | PENDING | MISSING | FAILED
        private boolean required;
        private String details;

        public Item() {}

        public Item(String item, String status, boolean required, String details) {
            this.item = item;
            this.status = status;
            this.required = required;
            this.details = details;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String item;
            private String status;
            private boolean required;
            private String details;

            private Builder() {}

            public Builder item(String item) { this.item = item; return this; }
            public Builder status(String status) { this.status = status; return this; }
            public Builder required(boolean required) { this.required = required; return this; }
            public Builder details(String details) { this.details = details; return this; }

            public Item build() { return new Item(item, status, required, details); }
        }

        public String getItem() { return item; }
        public String getStatus() { return status; }
        public boolean isRequired() { return required; }
        public String getDetails() { return details; }
    }

    public GoLiveChecklistResponse() {}

    public GoLiveChecklistResponse(boolean canGoLive, List<Item> items) {
        this.canGoLive = canGoLive;
        this.items = items;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private boolean canGoLive;
        private List<Item> items;

        private Builder() {}

        public Builder canGoLive(boolean canGoLive) { this.canGoLive = canGoLive; return this; }
        public Builder items(List<Item> items) { this.items = items; return this; }

        public GoLiveChecklistResponse build() { return new GoLiveChecklistResponse(canGoLive, items); }
    }

    public boolean isCanGoLive() { return canGoLive; }
    public List<Item> getItems() { return items; }
}


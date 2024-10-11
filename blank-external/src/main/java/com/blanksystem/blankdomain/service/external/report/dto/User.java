package com.blanksystem.blankdomain.service.external.report.dto;

public record User(String userId) {


    private User(Builder builder) {
        this(builder.userId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String userId;

        private Builder() {
        }

        public Builder withUserId(String val) {
            userId = val;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}

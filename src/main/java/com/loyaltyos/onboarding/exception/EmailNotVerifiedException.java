package com.loyaltyos.onboarding.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Email is not verified. Please verify your email to continue.");
    }
}


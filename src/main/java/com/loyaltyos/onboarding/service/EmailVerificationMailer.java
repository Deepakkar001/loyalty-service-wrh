package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailer {

    private final PlatformEmailService emailService;
    private final AppUrlConfig appUrlConfig;

    public EmailVerificationMailer(PlatformEmailService emailService, AppUrlConfig appUrlConfig) {
        this.emailService = Objects.requireNonNull(emailService, "emailService");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
    }

    public void sendVerificationCodeEmail(String toEmail, String code) {
        String helpUrl = appUrlConfig.getPortalUrl() + "/onboarding";
        String body = """
Welcome to LoyaltyOS!

Your verification code is:

%s

This code expires in 10 minutes.

Go back to the onboarding screen to enter the code:
%s

If you didn't request this, you can ignore this email.
""".formatted(code, helpUrl);

        emailService.sendPlainText(
            toEmail,
            "Your LoyaltyOS verification code",
            body,
            "Verification code: " + code
        );
    }
}

package com.loyaltyos.merchants.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import com.loyaltyos.onboarding.service.PlatformEmailService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MerchantInviteMailer {

    private final PlatformEmailService emailService;
    private final AppUrlConfig appUrlConfig;

    public MerchantInviteMailer(PlatformEmailService emailService, AppUrlConfig appUrlConfig) {
        this.emailService = Objects.requireNonNull(emailService, "emailService");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
    }

    public boolean sendInviteEmail(String toEmail, String merchantName, String inviteToken) {
        String acceptUrl = buildAcceptUrl(inviteToken);
        String displayName = merchantName != null && !merchantName.isBlank() ? merchantName : "your organisation";
        String body = """
You have been invited to the %s merchant portal on LoyaltyOS.

Open the link below to set your password and activate your account:

%s

This link expires in 7 days.

If you were not expecting this invitation, you can ignore this email.
""".formatted(displayName, acceptUrl);

        return emailService.sendPlainText(
            toEmail,
            "Your LoyaltyOS merchant portal invite",
            body,
            "Merchant invite URL: " + acceptUrl
        );
    }

    public String buildAcceptUrl(String inviteToken) {
        return appUrlConfig.getPortalUrl() + "/merchant/onboarding/" + inviteToken;
    }
}

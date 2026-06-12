package com.loyaltyos.access.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class TenantInviteMailer {

    private static final Logger log = LoggerFactory.getLogger(TenantInviteMailer.class);

    private final JavaMailSender mailSender;
    private final AppUrlConfig appUrlConfig;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.email.log-code-on-send-failure:false}")
    private boolean logOnSendFailure;

    public TenantInviteMailer(JavaMailSender mailSender, AppUrlConfig appUrlConfig) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
    }

    public void sendInviteEmail(String toEmail, String inviteToken) {
        String acceptUrl = appUrlConfig.getPortalUrl()
            + "/accept-invite?email="
            + URLEncoder.encode(toEmail, StandardCharsets.UTF_8)
            + "&token="
            + URLEncoder.encode(inviteToken, StandardCharsets.UTF_8);

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("spring.mail.username not set; skipping invite email. Accept URL: {}", acceptUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromAddress);
        message.setSubject("You have been invited to LoyaltyOS");
        message.setText("""
You have been invited to join your organisation on LoyaltyOS.

Open the link below to set your password and activate your account:

%s

This link expires in 7 days.

If you were not expecting this invitation, you can ignore this email.
""".formatted(acceptUrl));

        try {
            mailSender.send(message);
            log.info("Team invite email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send team invite email to {}.", toEmail, ex);
            if (logOnSendFailure) {
                log.warn("Invite accept URL for {}: {}", toEmail, acceptUrl);
                return;
            }
            throw ex;
        }
    }
}

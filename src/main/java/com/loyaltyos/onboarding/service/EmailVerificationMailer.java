package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationMailer.class);

    private final JavaMailSender mailSender;
    private final AppUrlConfig appUrlConfig;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * When true (recommended for local profile only), SMTP failures do not propagate:
     * the code is logged so onboarding can continue. Production should keep this false
     * so misconfigured mail surfaces as an error.
     */
    @Value("${app.email.log-code-on-send-failure:false}")
    private boolean logCodeOnSendFailure;

    public EmailVerificationMailer(JavaMailSender mailSender, AppUrlConfig appUrlConfig) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
    }

    public void sendVerificationCodeEmail(String toEmail, String code) {
        String helpUrl = appUrlConfig.getBaseUrl() + "/onboarding";

        // If mail isn't configured, don't blow up local dev — just log the link.
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("spring.mail.username not set; skipping email send. Verification code: {}", code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromAddress);
        message.setSubject("Your LoyaltyOS verification code");
        message.setText("""
Welcome to LoyaltyOS!

Your verification code is:

%s

This code expires in 10 minutes.

Go back to the onboarding screen to enter the code:
%s

If you didn't request this, you can ignore this email.
""".formatted(code, helpUrl));

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send verification email to {}.", toEmail, ex);
            if (logCodeOnSendFailure) {
                log.warn(
                    "app.email.log-code-on-send-failure=true — continuing without email. "
                        + "Verification code for {}: {}",
                    toEmail,
                    code);
                return;
            }
            throw ex;
        }
    }
}


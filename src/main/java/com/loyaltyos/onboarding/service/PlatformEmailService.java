package com.loyaltyos.onboarding.service;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Single outbound email path for onboarding OTP, agreement notifications, and team invites.
 * Mirrors the behaviour that works for {@link EmailVerificationMailer}.
 */
@Service
public class PlatformEmailService {

    private static final Logger log = LoggerFactory.getLogger(PlatformEmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Value("${app.email.log-code-on-send-failure:false}")
    private boolean lenientOnFailure;

    public PlatformEmailService(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.mailProperties = Objects.requireNonNull(mailProperties, "mailProperties");
    }

    public boolean isSmtpConfigured() {
        String username = mailProperties.getUsername();
        String password = mailProperties.getPassword();
        return username != null && !username.isBlank()
            && password != null && !password.isBlank();
    }

    /**
     * @param devFallbackDetail logged when SMTP is missing or send fails with lenient mode (e.g. OTP, invite URL)
     * @return true when SMTP accepted the message
     */
    public boolean sendPlainText(String toEmail, String subject, String body, String devFallbackDetail) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Email not sent — missing recipient (subject={})", subject);
            return false;
        }

        if (!isSmtpConfigured()) {
            log.warn(
                "Email NOT sent — SMTP username/password not configured (subject={}, to={}). {}",
                subject,
                toEmail,
                devFallbackDetail != null ? devFallbackDetail : ""
            );
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail.trim());
        message.setFrom(mailProperties.getUsername().trim());
        message.setSubject(subject);
        message.setText(body);

        try {
            log.info(
                "Sending email (subject={}, to={}) via {}:{}",
                subject,
                toEmail,
                mailProperties.getHost(),
                mailProperties.getPort()
            );
            mailSender.send(message);
            log.info("Email accepted by SMTP (subject={}, to={})", subject, toEmail);
            return true;
        } catch (MailException ex) {
            log.error("Failed to send email (subject={}, to={})", subject, toEmail, ex);
            if (lenientOnFailure) {
                if (devFallbackDetail != null && !devFallbackDetail.isBlank()) {
                    log.warn(
                        "app.email.log-code-on-send-failure=true — continuing without email. Detail for {}: {}",
                        toEmail,
                        devFallbackDetail
                    );
                }
                return false;
            }
            throw ex;
        }
    }
}

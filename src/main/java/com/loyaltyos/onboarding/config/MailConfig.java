package com.loyaltyos.onboarding.config;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    public JavaMailSender javaMailSender(MailProperties props) {
        // Explicit bean so mail is available regardless of IDE/auto-config quirks.
        // Fail-fast with an actionable message when required properties are missing.
        if (props.getHost() == null || props.getHost().isBlank()) {
            throw new IllegalStateException("Email is mandatory but spring.mail.host is not set.");
        }
        boolean hasCreds =
            props.getUsername() != null && !props.getUsername().isBlank()
                && props.getPassword() != null && !props.getPassword().isBlank();

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getHost());
        sender.setPort(props.getPort());
        if (hasCreds) {
            sender.setUsername(props.getUsername());
            sender.setPassword(props.getPassword());
        }
        if (props.getProtocol() != null && !props.getProtocol().isBlank()) {
            sender.setProtocol(props.getProtocol());
        }

        var encoding = props.getDefaultEncoding() != null ? props.getDefaultEncoding() : StandardCharsets.UTF_8;
        sender.setDefaultEncoding(encoding.name());

        Properties javaMailProps = new Properties();
        if (props.getProperties() != null) {
            javaMailProps.putAll(props.getProperties());
        }
        if (!hasCreds) {
            // Local dev: allow app to boot even without SMTP creds.
            // Callers should handle failures (we already log OTP in backend logs when enabled).
            javaMailProps.put("mail.smtp.auth", "false");
            javaMailProps.put("mail.smtp.starttls.enable", "false");
            javaMailProps.put("mail.smtp.starttls.required", "false");
        }
        sender.setJavaMailProperties(javaMailProps);

        if (hasCreds) return sender;

        return new JavaMailSenderImpl() {
            {
                setHost(sender.getHost());
                setPort(sender.getPort());
                setProtocol(sender.getProtocol());
                setDefaultEncoding(sender.getDefaultEncoding());
                setJavaMailProperties(sender.getJavaMailProperties());
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) throws MailException {
                log.warn("SMTP not configured; ignoring email send (subject={})", simpleMessage.getSubject());
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) throws MailException {
                log.warn("SMTP not configured; ignoring {} email(s)", simpleMessages == null ? 0 : simpleMessages.length);
            }

            @Override
            public void send(jakarta.mail.internet.MimeMessage mimeMessage) throws MailException {
                log.warn("SMTP not configured; ignoring MimeMessage email send");
            }

            @Override
            public void send(jakarta.mail.internet.MimeMessage... mimeMessages) throws MailException {
                log.warn("SMTP not configured; ignoring {} mime email(s)", mimeMessages == null ? 0 : mimeMessages.length);
            }

            @Override
            public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
                log.warn("SMTP not configured; ignoring MimeMessagePreparator email send");
            }

            @Override
            public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
                log.warn("SMTP not configured; ignoring {} preparator email(s)", mimeMessagePreparators == null ? 0 : mimeMessagePreparators.length);
            }
        };
    }
}


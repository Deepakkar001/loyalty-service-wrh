package com.loyaltyos.onboarding.config;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(MailProperties props) {
        // Explicit bean so mail is available regardless of IDE/auto-config quirks.
        // Fail-fast with an actionable message when required properties are missing.
        if (props.getHost() == null || props.getHost().isBlank()) {
            throw new IllegalStateException("Email is mandatory but spring.mail.host is not set.");
        }
        if (props.getUsername() == null || props.getUsername().isBlank()) {
            throw new IllegalStateException("Email is mandatory but spring.mail.username is not set.");
        }
        if (props.getPassword() == null || props.getPassword().isBlank()) {
            throw new IllegalStateException("Email is mandatory but spring.mail.password is not set.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getHost());
        sender.setPort(props.getPort());
        sender.setUsername(props.getUsername());
        sender.setPassword(props.getPassword());
        if (props.getProtocol() != null && !props.getProtocol().isBlank()) {
            sender.setProtocol(props.getProtocol());
        }

        var encoding = props.getDefaultEncoding() != null ? props.getDefaultEncoding() : StandardCharsets.UTF_8;
        sender.setDefaultEncoding(encoding.name());

        Properties javaMailProps = new Properties();
        if (props.getProperties() != null) {
            javaMailProps.putAll(props.getProperties());
        }
        sender.setJavaMailProperties(javaMailProps);
        return sender;
    }
}


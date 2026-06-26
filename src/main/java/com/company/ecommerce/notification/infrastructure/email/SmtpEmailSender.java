package com.company.ecommerce.notification.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * {@link EmailSender} backed by Spring's {@link JavaMailSender}. Delivers to the configured SMTP server
 * (Mailpit in dev/docker). Translates transport failures into {@link EmailDeliveryException} so the
 * caller can record a {@code FAILED} notification without leaking mail-library types.
 */
@Slf4j
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSender(
            JavaMailSender mailSender,
            @Value("${app.notification.from-address}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.debug("Email dispatched to {} (subject='{}').", to, subject);
        } catch (MailException ex) {
            throw new EmailDeliveryException("Failed to send email to " + to, ex);
        }
    }
}

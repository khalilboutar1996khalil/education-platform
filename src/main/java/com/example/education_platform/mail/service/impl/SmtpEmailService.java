package com.example.education_platform.mail.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends through an SMTP server once spring.mail.host is set and no Brevo key is.
 *
 * <p>Not usable on Render's free plan, which blocks outbound SMTP ports: use Brevo there.
 */
@Service("smtpEmailService")
@ConditionalOnExpression("!'${spring.mail.host:}'.isBlank() and '${BREVO_API_KEY:}'.isBlank()")
@RequiredArgsConstructor
public class SmtpEmailService extends TextEmailService {

    private final JavaMailSender mailSender;

    @Override
    protected void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

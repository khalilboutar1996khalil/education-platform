package com.example.education_platform.mail.service.impl;

import com.example.education_platform.mail.service.EmailService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * What runs when no SMTP host is configured: development, and tests.
 *
 * <p>It logs that a mail would have gone out and to whom, but never the temporary password or the
 * reset token — a log file is not a place to put either.
 */
@Service
@ConditionalOnMissingBean(name = "smtpEmailService")
public class LoggingEmailService implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendInvitation(String to, String fullName, String temporaryPassword) {
        LOG.info("[no SMTP configured] invitation for {} would be sent to {}", fullName, to);
    }

    @Override
    public void sendPasswordReset(String to, String fullName, String resetToken) {
        LOG.info("[no SMTP configured] password reset for {} would be sent to {}", fullName, to);
    }

    @Override
    public void sendAccessRejected(String to, String fullName, String note) {
        LOG.info("[no SMTP configured] access refusal for {} would be sent to {}", fullName, to);
    }

    @Override
    public void sendTemporaryPassword(String to, String fullName, String temporaryPassword) {
        LOG.info("[no SMTP configured] temporary password for {} would be sent to {}", fullName, to);
    }

    @Override
    public void sendDeadlineReminder(String to, String fullName, String title, Instant deadline, String path) {
        LOG.info("[no SMTP configured] reminder for \"{}\" would be sent to {}", title, to);
    }
}

package com.example.education_platform.mail.service;

import java.time.Instant;

public interface EmailService {

    void sendInvitation(String to, String fullName, String temporaryPassword);

    void sendPasswordReset(String to, String fullName, String resetToken);

    /** The admin turned down an access request; {@code note} is their reason, possibly null. */
    void sendAccessRejected(String to, String fullName, String note);

    /** The admin reset a student's password; the student signs in with this one and changes it. */
    void sendTemporaryPassword(String to, String fullName, String temporaryPassword);

    /** Work due soon that the student has not handed in yet. */
    void sendDeadlineReminder(String to, String fullName, String title, Instant deadline, String path);
}

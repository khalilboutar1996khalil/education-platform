package com.example.education_platform.mail.service;

public interface EmailService {

    void sendInvitation(String to, String fullName, String temporaryPassword);

    void sendPasswordReset(String to, String fullName, String resetToken);
}

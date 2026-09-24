package com.example.education_platform.mail.service.impl;

import com.example.education_platform.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Active only once spring.mail.host is set, which in practice means production. */
@Service("smtpEmailService")
@ConditionalOnProperty("spring.mail.host")
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@eduflow.dz}")
    private String from;

    @Value("${app.mail.base-url:http://localhost:5173}")
    private String baseUrl;

    @Override
    public void sendInvitation(String to, String fullName, String temporaryPassword) {
        send(to, "Votre accès à EduFlow", """
                Bonjour %s,

                Un compte vous a été créé sur EduFlow.

                Identifiant : %s
                Mot de passe provisoire : %s

                Connectez-vous sur %s et changez ce mot de passe dès votre première connexion.
                """.formatted(fullName, to, temporaryPassword, baseUrl));
    }

    @Override
    public void sendPasswordReset(String to, String fullName, String resetToken) {
        send(to, "Réinitialiser votre mot de passe", """
                Bonjour %s,

                Pour choisir un nouveau mot de passe, suivez ce lien :
                %s/reset-password?token=%s

                Ce lien expire dans une heure. Si vous n'êtes pas à l'origine de cette demande,
                ignorez ce message : rien n'a changé.
                """.formatted(fullName, baseUrl, resetToken));
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

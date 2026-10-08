package com.example.education_platform.mail.service.impl;

import com.example.education_platform.mail.service.EmailService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;

/**
 * The wording of every e-mail, written once. Subclasses only decide how a finished message
 * leaves the server: SMTP ({@link SmtpEmailService}) or an HTTPS API ({@link BrevoEmailService}).
 */
public abstract class TextEmailService implements EmailService {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM 'à' HH'h'mm", Locale.FRENCH);

    @Value("${app.mail.from:no-reply@eduflow.dz}")
    protected String from;

    @Value("${app.mail.base-url:http://localhost:5173}")
    private String baseUrl;

    /** Deadlines are stored in UTC; a student reads them in local time. */
    @Value("${app.mail.time-zone:Africa/Algiers}")
    private ZoneId timeZone;

    /** Sends one plain-text message. */
    protected abstract void send(String to, String subject, String body);

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

    @Override
    public void sendAccessRejected(String to, String fullName, String note) {
        String reason = note == null || note.isBlank() ? "" : "\nMotif : " + note.trim() + "\n";
        send(to, "Votre demande d'accès à EduFlow", """
                Bonjour %s,

                Votre demande d'accès à EduFlow n'a pas été acceptée.
                %s
                Si vous pensez qu'il s'agit d'une erreur, adressez-vous à votre professeur.
                """.formatted(fullName, reason));
    }

    @Override
    public void sendTemporaryPassword(String to, String fullName, String temporaryPassword) {
        send(to, "Votre nouveau mot de passe EduFlow", """
                Bonjour %s,

                Votre professeur a réinitialisé votre mot de passe.

                Identifiant : %s
                Mot de passe provisoire : %s

                Connectez-vous sur %s et changez ce mot de passe dès votre première connexion.
                """.formatted(fullName, to, temporaryPassword, baseUrl));
    }

    @Override
    public void sendDeadlineReminder(String to, String fullName, String title, Instant deadline, String path) {
        send(to, "À rendre demain : " + title, """
                Bonjour %s,

                Le travail « %s » est à rendre le %s, et vous ne l'avez pas encore remis.

                Pour le déposer : %s%s
                """.formatted(fullName, title, DEADLINE_FORMAT.format(deadline.atZone(timeZone)), baseUrl, path));
    }
}

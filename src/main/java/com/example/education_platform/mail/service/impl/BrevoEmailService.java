package com.example.education_platform.mail.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sends through Brevo's HTTPS API (port 443) instead of SMTP. Hosts such as Render's free plan
 * block the SMTP ports, so this is the sender to use there. Active as soon as BREVO_API_KEY is set,
 * and it takes priority over SMTP.
 *
 * <p>The sender address (APP_MAIL_FROM) must be a sender verified in the Brevo account.
 */
@Service("brevoEmailService")
@ConditionalOnExpression("!'${BREVO_API_KEY:}'.isBlank()")
public class BrevoEmailService extends TextEmailService {

    private static final String API_URL = "https://api.brevo.com/v3";

    private final RestClient client;

    @Value("${app.mail.from-name:EduFlow}")
    private String fromName;

    public BrevoEmailService(@Value("${BREVO_API_KEY}") String apiKey) {
        this.client = RestClient.builder()
                .baseUrl(API_URL)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    @Override
    protected void send(String to, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", from),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", body);
        client.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}

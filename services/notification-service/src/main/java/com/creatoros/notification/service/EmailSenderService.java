package com.creatoros.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.exceptions.MailerSendException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailSenderService {

    private final MailerSend mailerSend;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String emailProvider;

    private final String brevoApiKey;
    private final String brevoBaseUrl;
    private final String brevoFromEmail;
    private final String brevoFromName;

    private final String mailerSendToken;
    private final String fromEmail;
    private final String fromName;

    public EmailSenderService(
            MailerSend mailerSend,
            ObjectMapper objectMapper,
            @Value("${creatoros.email.provider:brevo}") String emailProvider,
            @Value("${creatoros.brevo.api-key:}") String brevoApiKey,
            @Value("${creatoros.brevo.base-url:https://api.brevo.com}") String brevoBaseUrl,
            @Value("${creatoros.brevo.from.email:}") String brevoFromEmail,
            @Value("${creatoros.brevo.from.name:CreatorOS}") String brevoFromName,
            @Value("${creatoros.mailersend.token:}") String mailerSendToken,
            @Value("${creatoros.mailersend.from.email:}") String fromEmail,
            @Value("${creatoros.mailersend.from.name:CreatorOS}") String fromName
    ) {
        this.mailerSend = mailerSend;
        this.objectMapper = objectMapper;
        this.emailProvider = emailProvider;
        this.brevoApiKey = brevoApiKey;
        this.brevoBaseUrl = brevoBaseUrl;
        this.brevoFromEmail = brevoFromEmail;
        this.brevoFromName = brevoFromName;
        this.mailerSendToken = mailerSendToken;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public String sendEmail(String toEmail, String subject, String plainText, String html) throws MailerSendException {
        if (toEmail == null || toEmail.isBlank()) {
            throw new MailerSendException("Missing recipient email");
        }

        String provider = emailProvider == null ? "brevo" : emailProvider.trim().toLowerCase(Locale.ROOT);
        if ("brevo".equals(provider)) {
            return sendViaBrevo(toEmail, subject, plainText, html);
        }
        if ("mailersend".equals(provider)) {
            return sendViaMailerSend(toEmail, subject, plainText, html);
        }
        if ("auto".equals(provider)) {
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                return sendViaBrevo(toEmail, subject, plainText, html);
            }
            return sendViaMailerSend(toEmail, subject, plainText, html);
        }

        throw new MailerSendException("Unsupported email provider: " + emailProvider + " (expected brevo, mailersend, or auto)");
    }

    private String sendViaMailerSend(String toEmail, String subject, String plainText, String html) throws MailerSendException {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new MailerSendException("Missing configured from email (creatoros.mailersend.from.email)");
        }
        if (mailerSendToken == null || mailerSendToken.isBlank()) {
            throw new MailerSendException("Missing MailerSend token (creatoros.mailersend.token / MAILERSEND_TOKEN)");
        }

        Email email = new Email();
        email.setFrom(fromName, fromEmail);
        email.addRecipient("", toEmail);
        email.setSubject(subject == null ? "" : subject);

        if (plainText != null) {
            email.setPlain(plainText);
        }
        if (html != null && !html.isBlank()) {
            email.setHtml(html);
        }

        MailerSendResponse response = mailerSend.emails().send(email);
        return response.messageId;
    }

    private String sendViaBrevo(String toEmail, String subject, String plainText, String html) throws MailerSendException {
        if (brevoFromEmail == null || brevoFromEmail.isBlank()) {
            throw new MailerSendException("Missing configured from email (creatoros.brevo.from.email)");
        }
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new MailerSendException("Missing Brevo API key (creatoros.brevo.api-key / BREVO_API_KEY / BREVO_SMTP_KEY)");
        }

        String baseUrl = (brevoBaseUrl == null || brevoBaseUrl.isBlank()) ? "https://api.brevo.com" : brevoBaseUrl;
        String endpoint = baseUrl + "/v3/smtp/email";

        Map<String, Object> sender = new HashMap<>();
        sender.put("email", brevoFromEmail);
        sender.put("name", brevoFromName);

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", toEmail);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(recipient));
        payload.put("subject", subject == null ? "" : subject);
        if (html != null && !html.isBlank()) {
            payload.put("htmlContent", html);
        } else {
            payload.put("textContent", plainText == null ? "" : plainText);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", brevoApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MailerSendException("Brevo send failed with status " + response.getStatusCode().value());
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return "";
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode messageId = root.get("messageId");
            return messageId == null || messageId.isNull() ? "" : messageId.asText("");
        } catch (RestClientException ex) {
            throw new MailerSendException("Brevo request failed: " + ex.getMessage());
        } catch (Exception ex) {
            throw new MailerSendException("Brevo response parsing failed: " + ex.getMessage());
        }
    }
}

package com.creatoros.notification.service;

import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.exceptions.MailerSendException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private final MailerSend mailerSend;
    private final String mailerSendToken;
    private final String fromEmail;
    private final String fromName;

    public EmailSenderService(
            MailerSend mailerSend,
            @Value("${creatoros.mailersend.token:}") String mailerSendToken,
            @Value("${creatoros.mailersend.from.email:}") String fromEmail,
            @Value("${creatoros.mailersend.from.name:CreatorOS}") String fromName
    ) {
        this.mailerSend = mailerSend;
        this.mailerSendToken = mailerSendToken;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public String sendEmail(String toEmail, String subject, String plainText, String html) throws MailerSendException {
        if (toEmail == null || toEmail.isBlank()) {
            throw new MailerSendException("Missing recipient email");
        }
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
}

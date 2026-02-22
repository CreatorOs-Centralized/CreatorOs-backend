package com.creatoros.notification.config;

import com.mailersend.sdk.MailerSend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailerSendConfig {

    @Bean
    MailerSend mailerSend(@Value("${creatoros.mailersend.token:}") String token) {
        MailerSend mailerSend = new MailerSend();
        if (token != null && !token.isBlank()) {
            mailerSend.setToken(token);
        }
        return mailerSend;
    }
}

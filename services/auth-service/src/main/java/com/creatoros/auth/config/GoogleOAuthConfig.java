package com.creatoros.auth.config;

import java.util.Set;

import com.creatoros.auth.oauth.google.AudienceValidator;
import com.creatoros.auth.oauth.google.GoogleIssuerValidator;
import com.creatoros.auth.oauth.google.GoogleOAuthTokenClient;
import com.creatoros.auth.oauth.google.RestClientGoogleOAuthTokenClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleOAuthConfig {

    @Bean
    RestClient googleOAuthRestClient() {
        return RestClient.create();
    }

    @Bean
    GoogleOAuthTokenClient googleOAuthTokenClient(GoogleOAuthProperties properties, RestClient googleOAuthRestClient) {
        return new RestClientGoogleOAuthTokenClient(properties, googleOAuthRestClient);
    }

    @Bean
    JwtDecoder googleJwtDecoder(GoogleOAuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwksUri()).build();

        OAuth2TokenValidator<Jwt> timestamp = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> issuer = new GoogleIssuerValidator(Set.of("https://accounts.google.com", "accounts.google.com"));
        OAuth2TokenValidator<Jwt> audience = new AudienceValidator(properties.getClientId());

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, issuer, audience));
        return decoder;
    }
}

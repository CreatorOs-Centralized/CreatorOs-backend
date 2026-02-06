package com.creatoros.auth.config;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * JWT validation hardening for a Keycloak-backed resource server.
 *
 * <p>Spring already validates signature and issuer when issuer-uri is configured.
 * Audience (aud) validation is not enforced by default, so we support an optional
 * allow-list to match Keycloak deployments that set aud via protocol mappers.
 */
@Configuration
@EnableConfigurationProperties(JwtValidationConfig.JwtSecurityProperties.class)
public class JwtValidationConfig {

    @Bean
    @SuppressWarnings("unused")
    JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties, JwtSecurityProperties jwtSecurityProperties) {
        String issuerUri = properties.getJwt().getIssuerUri();
        String jwkSetUri = properties.getJwt().getJwkSetUri();
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalStateException("spring.security.oauth2.resourceserver.jwt.issuer-uri must be configured");
        }

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jwtSecurityProperties.audiences());

        // Important: do not eagerly resolve issuer configuration during app startup.
        // NimbusJwtDecoder.withIssuerLocation(...) fetches OIDC metadata from the issuer, which makes
        // the entire service fail-fast if Keycloak is down. We keep strict validation, but defer the
        // remote lookup until the first authenticated request.
        //
        // Additionally, in Docker we may need issuer validation against a host URL (token `iss`),
        // while fetching keys from a container-reachable URL (jwk-set-uri).
        AtomicReference<NimbusJwtDecoder> delegateRef = new AtomicReference<>();

        return token -> {
            try {
                NimbusJwtDecoder delegate = delegateRef.get();
                if (delegate == null) {
                    NimbusJwtDecoder created = (jwkSetUri != null && !jwkSetUri.isBlank())
                            ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                            : NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
                    created.setJwtValidator(jwt -> {
                        OAuth2TokenValidatorResult issuerResult = issuerValidator.validate(jwt);
                        OAuth2TokenValidatorResult audResult = audienceValidator.validate(jwt);
                        if (!issuerResult.hasErrors() && !audResult.hasErrors()) {
                            return OAuth2TokenValidatorResult.success();
                        }
                        Set<OAuth2Error> errors = new LinkedHashSet<>();
                        errors.addAll(issuerResult.getErrors());
                        errors.addAll(audResult.getErrors());
                        return OAuth2TokenValidatorResult.failure(errors.toArray(OAuth2Error[]::new));
                    });
                    if (!delegateRef.compareAndSet(null, created)) {
                        delegate = delegateRef.get();
                    } else {
                        delegate = created;
                    }
                }
                return delegate.decode(token);
            } catch (JwtException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                // Ensure Spring Security treats decoder failures as authentication failures (401), not 500s.
                throw new JwtException("Failed to decode JWT", ex);
            }
        };
    }

    @ConfigurationProperties(prefix = "creatoros.security.jwt")
    public record JwtSecurityProperties(Set<String> audiences) {
        public JwtSecurityProperties {
            if (audiences == null || audiences.isEmpty()) {
                audiences = Set.of();
            } else {
                LinkedHashSet<String> normalized = new LinkedHashSet<>();
                for (String aud : audiences) {
                    if (aud == null) {
                        continue;
                    }
                    String trimmed = aud.trim();
                    if (!trimmed.isEmpty()) {
                        normalized.add(trimmed);
                    }
                }
                audiences = Set.copyOf(normalized);
            }
        }
    }

    private static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

        private final Set<String> allowedAudiences;

        private AudienceValidator(Set<String> allowedAudiences) {
            this.allowedAudiences = allowedAudiences == null ? Set.of() : allowedAudiences;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (allowedAudiences.isEmpty()) {
                // No audience requirement configured; skip validation.
                return OAuth2TokenValidatorResult.success();
            }
            for (String audience : token.getAudience()) {
                if (allowedAudiences.contains(audience)) {
                    return OAuth2TokenValidatorResult.success();
                }
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "JWT audience (aud) is not allowed for this resource", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}

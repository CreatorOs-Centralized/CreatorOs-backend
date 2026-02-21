package com.creatoros.gateway.utils;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public static final String CLAIM_USER_ID = "user_id";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";

    private final JwtDecoder jwtDecoder;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be configured (env JWT_SECRET)");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
        }

        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Validate exp/nbf; issuer/audience validation is handled by auth-service contract.
        decoder.setJwtValidator(JwtValidators.createDefault());
        this.jwtDecoder = decoder;
    }

    public String validateAndExtractUserId(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject;
        }
        return jwt.getClaimAsString(CLAIM_USER_ID);
    }

    public String validateAndExtractEmail(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return jwt.getClaimAsString(CLAIM_EMAIL);
    }
}

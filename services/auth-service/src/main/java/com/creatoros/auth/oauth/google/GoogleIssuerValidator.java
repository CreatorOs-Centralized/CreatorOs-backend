package com.creatoros.auth.oauth.google;

import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class GoogleIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> allowedIssuers;

    public GoogleIssuerValidator(Set<String> allowedIssuers) {
        this.allowedIssuers = allowedIssuers;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String iss = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        if (iss != null && allowedIssuers != null && allowedIssuers.contains(iss)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "issuer mismatch", null));
    }
}

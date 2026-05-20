package com.creatoros.auth.oauth.google;

import com.creatoros.auth.config.GoogleOAuthProperties;
import com.creatoros.auth.exception.UnauthorizedException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class RestClientGoogleOAuthTokenClient implements GoogleOAuthTokenClient {

    private final GoogleOAuthProperties properties;
    private final RestClient restClient;

    public RestClientGoogleOAuthTokenClient(GoogleOAuthProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public GoogleOAuthTokenResponse exchangeAuthorizationCode(String code, String redirectUri, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        if (codeVerifier != null && !codeVerifier.isBlank()) {
            form.add("code_verifier", codeVerifier);
        }

        try {
            return restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(GoogleOAuthTokenResponse.class);
        } catch (RestClientResponseException ex) {
            throw new UnauthorizedException("google_token_exchange_failed");
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("google_token_exchange_failed");
        }
    }
}

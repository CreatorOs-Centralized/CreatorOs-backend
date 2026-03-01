package com.creatoros.auth.oauth.google;

public interface GoogleOAuthTokenClient {
    GoogleOAuthTokenResponse exchangeAuthorizationCode(String code, String redirectUri, String codeVerifier);
}

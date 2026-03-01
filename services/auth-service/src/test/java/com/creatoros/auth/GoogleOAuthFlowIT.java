package com.creatoros.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.creatoros.auth.oauth.google.GoogleOAuthTokenClient;
import com.creatoros.auth.oauth.google.GoogleOAuthTokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "creatoros.auth.debug-token-response=true",
        "spring.kafka.bootstrap-servers=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",

        "creatoros.oauth.google.enabled=true",
        "creatoros.oauth.google.client-id=test-google-client",
        "creatoros.oauth.google.client-secret=test-google-secret"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class GoogleOAuthFlowIT {

    static {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String envDockerHost = System.getenv("DOCKER_HOST");
            String sysDockerHost = System.getProperty("docker.host");

            boolean envMissing = envDockerHost == null || envDockerHost.isBlank();
            boolean sysMissing = sysDockerHost == null || sysDockerHost.isBlank();

            boolean envLooksLikeCliPipe = envDockerHost != null && envDockerHost.toLowerCase(Locale.ROOT).contains("docker_cli");
            boolean sysLooksLikeCliPipe = sysDockerHost != null && sysDockerHost.toLowerCase(Locale.ROOT).contains("docker_cli");

            if (sysLooksLikeCliPipe || envLooksLikeCliPipe || (sysMissing && envMissing)) {
                System.setProperty("docker.host", "npipe:////./pipe/docker_engine");
            }
        }
    }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_db")
            .withUsername("auth")
            .withPassword("auth");

    private static HttpServer jwksServer;
    private static int jwksPort;
    private static RSAKey rsaJwk;

        static void startJwksServerIfNeeded() {
                if (jwksServer != null) {
                        return;
                }

                try {
                                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                                kpg.initialize(2048);
                                KeyPair kp = kpg.generateKeyPair();
                                RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
                                RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

                                rsaJwk = new RSAKey.Builder(publicKey)
                                                .privateKey(privateKey)
                                                .keyID("test-kid")
                                                .build();

                    String jwksJson = new JWKSet(rsaJwk.toPublicJWK()).toString();

                        jwksServer = HttpServer.create(new InetSocketAddress(0), 0);
                        jwksServer.createContext("/certs", exchange -> {
                                byte[] bytes = jwksJson.getBytes(StandardCharsets.UTF_8);
                                exchange.getResponseHeaders().add("Content-Type", "application/json");
                                exchange.sendResponseHeaders(200, bytes.length);
                                try (OutputStream os = exchange.getResponseBody()) {
                                        os.write(bytes);
                                }
                        });
                        jwksServer.start();
                        jwksPort = jwksServer.getAddress().getPort();
                } catch (Exception ex) {
                        throw new RuntimeException(ex);
                }
    }

    @AfterAll
    static void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
                startJwksServerIfNeeded();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("creatoros.security.jwt.secret", () -> "01234567890123456789012345678901");
        registry.add("creatoros.security.jwt.access-token-expiration", () -> "900");
        registry.add("creatoros.security.jwt.refresh-token-expiration", () -> "604800");

        registry.add("creatoros.oauth.google.jwks-uri", () -> "http://localhost:" + jwksPort + "/certs");
    }

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    GoogleOAuthTokenClient googleOAuthTokenClient;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void googleOauth_createsOrLogsInUser_andIssuesPlatformTokens() throws Exception {
        String idToken = buildIdToken("bob@example.com", true, "Bob Creator");

        when(googleOAuthTokenClient.exchangeAuthorizationCode(any(), any(), any()))
                .thenReturn(new GoogleOAuthTokenResponse(
                        "google-access",
                        3600L,
                        "google-refresh",
                        "openid email profile",
                        "Bearer",
                        idToken
                ));

        MvcResult oauthResult = mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "test-code",
                                "redirectUri", "http://localhost/callback",
                                "codeVerifier", "verifier"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode oauthBody = objectMapper.readTree(oauthResult.getResponse().getContentAsString());
        String accessToken = oauthBody.get("accessToken").asText();
        String refreshToken = oauthBody.get("refreshToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).startsWith("rt_");

        MvcResult meResult = mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode meBody = objectMapper.readTree(meResult.getResponse().getContentAsString());
        assertThat(meBody.get("email").asText()).isEqualTo("bob@example.com");
        assertThat(meBody.get("id").asText()).isNotBlank();
    }

    @Test
    void googleOauth_requiresVerifiedEmail() throws Exception {
        String idToken = buildIdToken("eve@example.com", false, "Eve");

        when(googleOAuthTokenClient.exchangeAuthorizationCode(any(), any(), any()))
                .thenReturn(new GoogleOAuthTokenResponse(
                        "google-access",
                        3600L,
                        "google-refresh",
                        "openid email profile",
                        "Bearer",
                        idToken
                ));

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "test-code",
                                "redirectUri", "http://localhost/callback"
                        ))))
                .andExpect(status().isForbidden());
    }

    private static String buildIdToken(String email, boolean emailVerified, String name) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience("test-google-client")
                .subject("google-sub-123")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("name", name)
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(rsaJwk.getKeyID())
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(rsaJwk.toPrivateKey()));
        return jwt.serialize();
    }
}

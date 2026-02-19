package com.creatoros.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIT {

        static {
                // On some Windows + Docker Desktop setups, Docker may advertise the control pipe (docker_cli)
                // which Testcontainers can't use as a Docker Engine endpoint. Prefer docker_engine.
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

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // 32+ bytes
        registry.add("creatoros.security.jwt.secret", () -> "01234567890123456789012345678901");
        registry.add("creatoros.security.jwt.access-token-expiration", () -> "900");
        registry.add("creatoros.security.jwt.refresh-token-expiration", () -> "604800");
    }

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fullAuthFlow_worksAndEnforcesSecurity() throws Exception {
        String email = "alice@example.com";
        String password = "Password123!";

        // register
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerBody = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String userId = registerBody.get("userId").asText();
        String verificationToken = registerBody.get("emailVerificationToken").asText();
        assertThat(userId).isNotBlank();
        assertThat(verificationToken).startsWith("evt_");

        // login should be forbidden until email verified
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isForbidden());

        // verify email
        mockMvc.perform(post("/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationToken))))
                .andExpect(status().isNoContent());

        // login
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();
        String refreshToken = loginBody.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).startsWith("rt_");

        // protected endpoint requires auth
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        // /auth/me works with access token
        MvcResult meResult = mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode meBody = objectMapper.readTree(meResult.getResponse().getContentAsString());
        assertThat(meBody.get("id").asText()).isEqualTo(userId);

        // USER should not access ADMIN endpoint
        mockMvc.perform(get("/auth/admin/ping")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        // refresh rotates token
        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = refreshBody.get("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotBlank();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        // old refresh token cannot be reused
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());

        // logout revokes token
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
                .andExpect(status().isUnauthorized());

        // password reset request returns debug token
        MvcResult prtResult = mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode prtBody = objectMapper.readTree(prtResult.getResponse().getContentAsString());
        String resetToken = prtBody.get("resetToken").asText();
        assertThat(resetToken).startsWith("prt_");

        // confirm reset
        String newPassword = "NewPassword123!";
        mockMvc.perform(post("/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", resetToken,
                                "newPassword", newPassword
                        ))))
                .andExpect(status().isNoContent());

        // old password invalid
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isUnauthorized());

        // new password works
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", newPassword
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = "dup@example.com";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "u1",
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "u2",
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isConflict());
    }
}

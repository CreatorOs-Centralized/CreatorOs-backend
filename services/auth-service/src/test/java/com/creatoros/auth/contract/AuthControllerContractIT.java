package com.creatoros.auth.contract;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import com.creatoros.auth.security.JwtAuthConverter;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_service_tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/test"
})
@AutoConfigureMockMvc
class AuthControllerContractIT {

    @Autowired
    MockMvc mockMvc;

        private final JwtAuthConverter jwtAuthConverter = new JwtAuthConverter();

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void me_contractReturnsIdentityAndAllowedRoles() throws Exception {
        Jwt jwt = jwtWithRoles(
                "user-123",
                "alice",
                "alice@example.com",
                List.of("admin"),
                List.of("svc"),
                List.of("leak")
        );

        mockMvc.perform(get("/auth/me")
                        .with(authentication(asAuth(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.roles", hasItem("admin")))
                .andExpect(jsonPath("$.roles", hasItem("svc")))
                .andExpect(jsonPath("$.roles", not(hasItem("leak"))));
    }

    @Test
    void sync_contractIsIdempotentAndKafkaFailureDoesNotFailRequest() throws Exception {
        doThrow(new RuntimeException("kafka down"))
                .when(kafkaTemplate).send(anyString(), anyString(), any());

        Jwt jwt = jwtWithRoles(
                "user-456",
                "bob",
                "bob@example.com",
                List.of("user"),
                List.of("creator"),
                List.of()
        );

        mockMvc.perform(post("/auth/users/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit")
                        .with(authentication(asAuth(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-456"))
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andExpect(jsonPath("$.roles", hasItem("user")))
                .andExpect(jsonPath("$.roles", hasItem("creator")));

        // Second call should still succeed (idempotent upsert) even if Kafka remains unavailable.
        mockMvc.perform(post("/auth/users/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit")
                        .with(authentication(asAuth(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-456"));

        mockMvc.perform(get("/auth/users/user-456/roles")
                        .with(authentication(asAuth(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("user")))
                .andExpect(jsonPath("$[*].name", hasItem("creator")));
    }

    private AbstractAuthenticationToken asAuth(Jwt jwt) {
        AbstractAuthenticationToken auth = jwtAuthConverter.convert(jwt);
        // Converter returns an Authentication token; ensure it is treated as authenticated in tests.
        auth.setDetails(jwt);
        return auth;
    }

    private static Jwt jwtWithRoles(
            String sub,
            String preferredUsername,
            String email,
            List<String> realmRoles,
            List<String> azpClientRoles,
            List<String> otherClientRoles
    ) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", sub)
                .claim("preferred_username", preferredUsername)
                .claim("email", email)
                .claim("sid", "kc-session-1")
                .claim("azp", "auth-service")
                .claim("realm_access", Map.of("roles", realmRoles))
                .claim("resource_access", Map.of(
                        "auth-service", Map.of("roles", azpClientRoles),
                        "other-client", Map.of("roles", otherClientRoles)
                ))
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}

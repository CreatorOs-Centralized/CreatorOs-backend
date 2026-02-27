package com.creatoros.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.creatoros.auth.config.GoogleOAuthProperties;
import com.creatoros.auth.config.JwtProperties;
import com.creatoros.auth.dto.auth.GoogleOAuthRequest;
import com.creatoros.auth.dto.auth.TokenResponse;
import com.creatoros.auth.event.UserCreatedEvent;
import com.creatoros.auth.event.UserRoleUpdatedEvent;
import com.creatoros.auth.exception.BadRequestException;
import com.creatoros.auth.exception.ForbiddenException;
import com.creatoros.auth.exception.UnauthorizedException;
import com.creatoros.auth.model.Role;
import com.creatoros.auth.model.User;
import com.creatoros.auth.oauth.google.GoogleOAuthTokenClient;
import com.creatoros.auth.oauth.google.GoogleOAuthTokenResponse;
import com.creatoros.auth.repository.UserRepository;
import com.creatoros.auth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    private final GoogleOAuthProperties googleOAuthProperties;
    private final GoogleOAuthTokenClient googleOAuthTokenClient;
    private final JwtDecoder googleJwtDecoder;

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;

    private final String topicUserCreated;
    private final String topicUserRoleUpdated;

    public GoogleOAuthService(
            GoogleOAuthProperties googleOAuthProperties,
            GoogleOAuthTokenClient googleOAuthTokenClient,
            JwtDecoder googleJwtDecoder,
            UserRepository userRepository,
            RoleService roleService,
            SessionService sessionService,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            JwtProperties jwtProperties,
            KafkaTemplate<String, Object> kafkaTemplate,
            Clock clock,
            @Value("${creatoros.kafka.topics.user-created:auth.user.created}") String topicUserCreated,
            @Value("${creatoros.kafka.topics.user-role-updated:auth.user.role-updated}") String topicUserRoleUpdated
    ) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.googleOAuthTokenClient = googleOAuthTokenClient;
        this.googleJwtDecoder = googleJwtDecoder;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.sessionService = sessionService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topicUserCreated = topicUserCreated;
        this.topicUserRoleUpdated = topicUserRoleUpdated;
    }

    @Transactional
    public TokenResponse loginOrSignup(GoogleOAuthRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BadRequestException("request is required");
        }
        if (!googleOAuthProperties.isEnabled()) {
            throw new BadRequestException("google_oauth_disabled");
        }
        if (googleOAuthProperties.getClientId() == null || googleOAuthProperties.getClientId().isBlank()) {
            throw new BadRequestException("google_oauth_not_configured");
        }
        if (googleOAuthProperties.getClientSecret() == null || googleOAuthProperties.getClientSecret().isBlank()) {
            throw new BadRequestException("google_oauth_not_configured");
        }

        GoogleOAuthTokenResponse tokenResponse = googleOAuthTokenClient.exchangeAuthorizationCode(
                request.code(),
                request.redirectUri(),
                request.codeVerifier()
        );
        if (tokenResponse == null || tokenResponse.idToken() == null || tokenResponse.idToken().isBlank()) {
            throw new UnauthorizedException("google_id_token_missing");
        }

        Jwt idToken;
        try {
            idToken = googleJwtDecoder.decode(tokenResponse.idToken());
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("google_id_token_invalid");
        }

        String email = idToken.getClaimAsString("email");
        Boolean emailVerified = idToken.getClaimAsBoolean("email_verified");
        String name = idToken.getClaimAsString("name");

        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("google_email_missing");
        }
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new ForbiddenException("google_email_not_verified");
        }

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findWithRolesByEmail(normalizedEmail).orElse(null);
        boolean created = false;

        if (user == null) {
            UUID userId = UUID.randomUUID();
            user = new User(userId);
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID() + "." + UUID.randomUUID()));
            user.setEmailVerified(true);
            user.setActive(true);

            Set<Role> roles = roleService.upsertRoles(Set.of("USER"));
            user.setRoles(roles);

            user = userRepository.save(user);
            created = true;
        } else {
            if (!user.isActive()) {
                sessionService.recordLoginAttempt(user, httpRequest, false, "USER_DISABLED");
                throw new ForbiddenException("USER_DISABLED");
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                user = userRepository.save(user);
            }
        }

        UUID sessionId = sessionService.recordLoginAttempt(user, httpRequest, true, null);

        if (created) {
            Instant now = Instant.now(clock);
            String derivedUsername = deriveUsername(name, normalizedEmail);

            publishBestEffort(topicUserCreated, user.getId().toString(), new UserCreatedEvent(
                    1,
                    UUID.randomUUID(),
                    now,
                    user.getId().toString(),
                    derivedUsername,
                    safeRoleNames(user)
            ));

            publishBestEffort(topicUserRoleUpdated, user.getId().toString(), new UserRoleUpdatedEvent(
                    1,
                    UUID.randomUUID(),
                    now,
                    user.getId().toString(),
                    safeRoleNames(user)
            ));
        }

        Duration accessTtl = jwtProperties.accessTokenDuration();
        Duration refreshTtl = jwtProperties.refreshTokenDuration();

        String accessToken = jwtUtil.generateAccessToken(user.getId().toString(), user.getEmail(), safeRoleNames(user), accessTtl);
        TokenService.GeneratedToken refresh = tokenService.generateRefreshToken(user, refreshTtl, sessionId.toString(), httpRequest);
        return new TokenResponse(accessToken, accessTtl.getSeconds(), refresh.rawToken(), refreshTtl.getSeconds());
    }

    private void publishBestEffort(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event);
        } catch (RuntimeException ex) {
            String eventType = event == null ? "null" : event.getClass().getSimpleName();
            log.warn("kafka_publish_failed topic={} key={} eventType={}", topic, key, eventType, ex);
        }
    }

    private static Set<String> safeRoleNames(User user) {
        if (user == null || user.getRoles() == null) {
            return Set.of();
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String deriveUsername(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return null;
        }
        String local = email.substring(0, at).trim();
        return local.isBlank() ? null : local;
    }
}

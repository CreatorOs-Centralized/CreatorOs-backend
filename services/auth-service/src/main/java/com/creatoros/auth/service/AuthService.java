package com.creatoros.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.creatoros.auth.config.AuthProperties;
import com.creatoros.auth.config.JwtProperties;
import com.creatoros.auth.dto.UserDto;
import com.creatoros.auth.dto.auth.LoginRequest;
import com.creatoros.auth.dto.auth.PasswordResetConfirmRequest;
import com.creatoros.auth.dto.auth.PasswordResetRequest;
import com.creatoros.auth.dto.auth.PasswordResetResponse;
import com.creatoros.auth.dto.auth.RefreshRequest;
import com.creatoros.auth.dto.auth.RegisterRequest;
import com.creatoros.auth.dto.auth.RegisterResponse;
import com.creatoros.auth.dto.auth.TokenResponse;
import com.creatoros.auth.dto.auth.VerifyEmailRequest;
import com.creatoros.auth.exception.BadRequestException;
import com.creatoros.auth.exception.ConflictException;
import com.creatoros.auth.exception.ForbiddenException;
import com.creatoros.auth.exception.UnauthorizedException;
import com.creatoros.auth.event.UserCreatedEvent;
import com.creatoros.auth.event.UserRoleUpdatedEvent;
import com.creatoros.auth.model.Role;
import com.creatoros.auth.model.User;
import com.creatoros.auth.repository.UserRepository;
import com.creatoros.auth.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;

    private final String topicUserCreated;
    private final String topicUserRoleUpdated;

    public AuthService(
            UserRepository userRepository,
            RoleService roleService,
            SessionService sessionService,
            TokenService tokenService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            JwtProperties jwtProperties,
            AuthProperties authProperties,
            KafkaTemplate<String, Object> kafkaTemplate,
            Clock clock,
            @Value("${creatoros.kafka.topics.user-created:auth.user.created}") String topicUserCreated,
            @Value("${creatoros.kafka.topics.user-role-updated:auth.user.role-updated}") String topicUserRoleUpdated
    ) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.sessionService = sessionService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topicUserCreated = topicUserCreated;
        this.topicUserRoleUpdated = topicUserRoleUpdated;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        UUID userId = UUID.randomUUID();
        User user = new User(userId);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        user.setActive(true);

        Set<Role> roles = roleService.upsertRoles(Set.of("USER"));
        user.setRoles(roles);

        User saved = userRepository.save(user);

        Instant now = Instant.now(clock);
        publishBestEffort(topicUserCreated, saved.getId().toString(), new UserCreatedEvent(
                1,
                UUID.randomUUID(),
                now,
            saved.getId().toString(),
            request.username() == null || request.username().isBlank() ? null : request.username().trim(),
                safeRoleNames(saved)
        ));

        // If roles were assigned, also emit role update event.
        publishBestEffort(topicUserRoleUpdated, saved.getId().toString(), new UserRoleUpdatedEvent(
                1,
                UUID.randomUUID(),
                now,
            saved.getId().toString(),
                safeRoleNames(saved)
        ));

        TokenService.GeneratedToken verification = tokenService.generateEmailVerificationToken(saved, Duration.ofHours(24));
        emailService.sendEmailVerification(saved, verification.rawToken());
        String raw = authProperties.isDebugTokenResponse() ? verification.rawToken() : null;

        return new RegisterResponse(saved.getId().toString(), saved.isEmailVerified(), raw);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findWithRolesByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        String failure = null;
        boolean success = false;

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash() == null ? "" : user.getPasswordHash())) {
            failure = "WRONG_PASSWORD";
        } else if (!user.isActive()) {
            failure = "USER_DISABLED";
        } else if (!user.isEmailVerified()) {
            failure = "EMAIL_NOT_VERIFIED";
        } else {
            success = true;
        }

        UUID sessionId = sessionService.recordLoginAttempt(user, httpRequest, success, failure);

        if (!success) {
            if ("WRONG_PASSWORD".equals(failure)) {
                throw new UnauthorizedException("Invalid credentials");
            }
            if ("USER_DISABLED".equals(failure)) {
                throw new ForbiddenException("USER_DISABLED");
            }
            if ("EMAIL_NOT_VERIFIED".equals(failure)) {
                throw new ForbiddenException("EMAIL_NOT_VERIFIED");
            }
            throw new UnauthorizedException("Invalid credentials");
        }

        Duration accessTtl = jwtProperties.accessTokenDuration();
        Duration refreshTtl = jwtProperties.refreshTokenDuration();

        Set<String> roles = safeRoleNames(user);
        String accessToken = jwtUtil.generateAccessToken(user.getId().toString(), user.getEmail(), roles, accessTtl);

        TokenService.GeneratedToken refresh = tokenService.generateRefreshToken(user, refreshTtl, sessionId.toString(), httpRequest);

        return new TokenResponse(accessToken, accessTtl.getSeconds(), refresh.rawToken(), refreshTtl.getSeconds());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request, HttpServletRequest httpRequest) {
        Duration accessTtl = jwtProperties.accessTokenDuration();
        Duration refreshTtl = jwtProperties.refreshTokenDuration();

        TokenService.RotatedRefreshToken rotated = tokenService.rotateRefreshToken(request.refreshToken(), refreshTtl, httpRequest);
        User user = rotated.user();

        // reload roles to keep claims aligned with DB
        User userWithRoles = userRepository.findWithRolesById(user.getId()).orElse(user);
        Set<String> roles = safeRoleNames(userWithRoles);

        String accessToken = jwtUtil.generateAccessToken(user.getId().toString(), user.getEmail(), roles, accessTtl);
        return new TokenResponse(accessToken, accessTtl.getSeconds(), rotated.rawNewRefreshToken(), refreshTtl.getSeconds());
    }

    @Transactional
    public void logout(String refreshToken) {
        TokenService.RevokedRefreshToken revoked = tokenService.revokeRefreshToken(refreshToken);
        if (revoked.sessionExternalId() != null && !revoked.sessionExternalId().isBlank()) {
            sessionService.markLoggedOut(revoked.sessionExternalId());
        }
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = tokenService.verifyEmail(request.token());
        userRepository.save(user);
    }

    @Transactional
    public PasswordResetResponse requestPasswordReset(PasswordResetRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            // Do not leak whether a user exists.
            return new PasswordResetResponse(null);
        }

        TokenService.GeneratedToken token = tokenService.generatePasswordResetToken(user, Duration.ofHours(1));
        String raw = authProperties.isDebugTokenResponse() ? token.rawToken() : null;
        return new PasswordResetResponse(raw);
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw new BadRequestException("token is required");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new BadRequestException("newPassword is required");
        }
        User user = tokenService.verifyPasswordResetToken(request.token());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        tokenService.revokeAllActiveRefreshTokensForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public UserDto me(String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("User not found");
        }

        return userRepository.findWithRolesById(id)
                .map(u -> new UserDto(u.getId().toString(), null, u.getEmail(), safeRoleNames(u)))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
}

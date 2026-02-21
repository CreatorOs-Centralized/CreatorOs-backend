package com.creatoros.auth.controller;

import java.util.Set;
import java.util.stream.Collectors;

import com.creatoros.auth.dto.RoleDto;
import com.creatoros.auth.dto.UserDto;
import com.creatoros.auth.dto.auth.LoginRequest;
import com.creatoros.auth.dto.auth.LogoutRequest;
import com.creatoros.auth.dto.auth.PasswordResetConfirmRequest;
import com.creatoros.auth.dto.auth.PasswordResetRequest;
import com.creatoros.auth.dto.auth.PasswordResetResponse;
import com.creatoros.auth.dto.auth.RefreshRequest;
import com.creatoros.auth.dto.auth.RegisterRequest;
import com.creatoros.auth.dto.auth.RegisterResponse;
import com.creatoros.auth.dto.auth.TokenResponse;
import com.creatoros.auth.dto.auth.VerifyEmailRequest;
import com.creatoros.auth.security.AuthenticatedUser;
import com.creatoros.auth.service.AuthService;
import com.creatoros.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
/**
 * Auth Service REST contract.
 *
 * <p>This controller is intentionally small: auth-service is the platform's <b>custom JWT issuer</b> and provides
 * a stable identity + roles contract for other microservices.
 *
 * <h2>Endpoint audience</h2>
 * <ul>
 *   <li><b>External (gateway-facing)</b>: called by a user via API gateway with a CreatorOS JWT.</li>
 *   <li><b>Internal (service-to-service)</b>: intended for other backend services. These endpoints still require
 *       a valid JWT, and must not be used for cross-service DB joins.</li>
 * </ul>
 *
 * <p><b>Guarantees</b>:
 * <ul>
 *   <li>Stateless, JWT-only authentication.</li>
 *   <li>Kafka publishing must never block or fail HTTP requests.</li>
 * </ul>
 */
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
            @Valid @org.springframework.web.bind.annotation.RequestBody RefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.refresh(request, httpRequest);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @org.springframework.web.bind.annotation.RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @org.springframework.web.bind.annotation.RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public PasswordResetResponse requestPasswordReset(@Valid @org.springframework.web.bind.annotation.RequestBody PasswordResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @org.springframework.web.bind.annotation.RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    /**
     * External.
     *
     * <p>Returns the authenticated user's identity contract.
     * If the user was previously synced to the database, returns the persisted identity and roles.
     * Otherwise returns values derived from the JWT claims.
     *
     * <p>Guarantee: never triggers cross-service side effects.
     */
    public UserDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.me(principal);
    }

    @GetMapping("/users/{userId}/roles")
    /**
     * Internal.
     *
     * <p>Returns the current role set for a user id as known by auth-service.
     * Intended for service-to-service authorization checks.
     *
     * <p>Guarantee: reads only auth-service owned data (users/roles/user_roles).
     */
    public Set<RoleDto> getUserRoles(@PathVariable String userId) {
        return userService.getUserRoles(userId).stream()
                .map(RoleDto::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminPing() {
        return ResponseEntity.ok("ok");
    }
}

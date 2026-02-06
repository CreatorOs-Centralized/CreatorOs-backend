package com.creatoros.auth.controller;

import java.util.Set;
import java.util.stream.Collectors;

import com.creatoros.auth.dto.RoleDto;
import com.creatoros.auth.dto.UserDto;
import com.creatoros.auth.security.AuthenticatedUser;
import com.creatoros.auth.service.SessionService;
import com.creatoros.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * <p>This controller is intentionally small: auth-service is a Keycloak-backed <b>resource server</b> that provides
 * a stable identity + roles contract for other microservices.
 *
 * <h2>Endpoint audience</h2>
 * <ul>
 *   <li><b>External (gateway-facing)</b>: called by a user via API gateway with a Keycloak JWT.</li>
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
    private final SessionService sessionService;

    public AuthController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
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

    @PostMapping("/users/sync")
    /**
     * External.
     *
     * <p>Idempotently upserts the authenticated user and their roles into auth-service storage.
     * Also records the user's login session metadata (best-effort).
     *
     * <p>Guarantee: Kafka failures are ignored and do not fail the request.
     */
    public UserDto syncCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal, HttpServletRequest request) {
        return userService.syncCurrentUser(principal, request);
    }

    @PostMapping("/logout")
    /**
     * External.
     *
     * <p>Records a logout/revocation event in auth-service storage (best-effort).
     * Does not call Keycloak and does not invalidate JWTs globally.
     */
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        sessionService.logout(principal);
        return ResponseEntity.noContent().build();
    }
}

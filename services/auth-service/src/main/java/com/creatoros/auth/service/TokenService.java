package com.creatoros.auth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.creatoros.auth.exception.BadRequestException;
import com.creatoros.auth.exception.UnauthorizedException;
import com.creatoros.auth.model.EmailVerificationToken;
import com.creatoros.auth.model.PasswordResetToken;
import com.creatoros.auth.model.RefreshToken;
import com.creatoros.auth.model.User;
import com.creatoros.auth.repository.EmailVerificationTokenRepository;
import com.creatoros.auth.repository.PasswordResetTokenRepository;
import com.creatoros.auth.repository.RefreshTokenRepository;
import com.creatoros.auth.util.HttpRequestUtil;
import com.creatoros.auth.util.TokenHashUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private static final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Clock clock;

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.clock = clock;
    }

    public record GeneratedToken(UUID id, String rawToken, Instant expiresAt) {
    }

    public record RotatedRefreshToken(User user, String rawNewRefreshToken, Duration refreshTtl, String sessionExternalId) {
    }

    public record RevokedRefreshToken(String userId, String sessionExternalId) {
    }

    public GeneratedToken generateRefreshToken(User user, Duration ttl, String sessionExternalId, HttpServletRequest request) {
        Instant now = Instant.now(clock);
        UUID tokenId = UUID.randomUUID();
        String raw = formatToken("rt", tokenId);

        String tokenHash = TokenHashUtil.sha256Hex(raw);
        RefreshToken entity = new RefreshToken(tokenId, user, tokenHash, now.plus(ttl));
        entity.setIpAddress(HttpRequestUtil.extractClientIp(request));
        entity.setDeviceInfo(buildDeviceInfo(sessionExternalId, request));
        refreshTokenRepository.save(entity);

        return new GeneratedToken(tokenId, raw, entity.getExpiresAt());
    }

    @Transactional
    public RotatedRefreshToken rotateRefreshToken(String rawRefreshToken, Duration refreshTtl, HttpServletRequest request) {
        ParsedToken parsed = parseToken(rawRefreshToken, "rt");
        Instant now = Instant.now(clock);

        RefreshToken existing = refreshTokenRepository.findWithUserById(parsed.id())
                .orElseThrow(() -> new UnauthorizedException("invalid_refresh_token"));

        String providedHash = TokenHashUtil.sha256Hex(rawRefreshToken);
        if (!TokenHashUtil.constantTimeEqualsHex(existing.getTokenHash(), providedHash)) {
            // Token id exists but secret part mismatched: treat as invalid/replay.
            throw new UnauthorizedException("invalid_refresh_token");
        }

        if (existing.getRevokedAt() != null) {
            // Reuse attack: token already revoked.
            refreshTokenRepository.revokeAllActiveForUser(existing.getUser().getId(), now);
            throw new UnauthorizedException("refresh_token_reused");
        }

        if (existing.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("refresh_token_expired");
        }

        // Invalidate old token (keep row for reuse detection).
        existing.revoke(now);
        refreshTokenRepository.save(existing);

        String sessionExternalId = extractSessionExternalId(existing.getDeviceInfo());
        GeneratedToken generated = generateRefreshToken(existing.getUser(), refreshTtl, sessionExternalId, request);
        return new RotatedRefreshToken(existing.getUser(), generated.rawToken(), refreshTtl, sessionExternalId);
    }

    @Transactional
    public RevokedRefreshToken revokeRefreshToken(String rawRefreshToken) {
        ParsedToken parsed = parseToken(rawRefreshToken, "rt");
        Instant now = Instant.now(clock);

        RefreshToken existing = refreshTokenRepository.findById(parsed.id())
                .orElseThrow(() -> new UnauthorizedException("invalid_refresh_token"));

        String providedHash = TokenHashUtil.sha256Hex(rawRefreshToken);
        if (!TokenHashUtil.constantTimeEqualsHex(existing.getTokenHash(), providedHash)) {
            throw new UnauthorizedException("invalid_refresh_token");
        }

        if (existing.getRevokedAt() == null) {
            existing.revoke(now);
            refreshTokenRepository.save(existing);
        }

        return new RevokedRefreshToken(existing.getUser().getId().toString(), extractSessionExternalId(existing.getDeviceInfo()));
    }

    public GeneratedToken generateEmailVerificationToken(User user, Duration ttl) {
        Instant now = Instant.now(clock);
        UUID id = UUID.randomUUID();
        String raw = formatToken("evt", id);

        EmailVerificationToken token = new EmailVerificationToken(
                id,
                user,
                TokenHashUtil.sha256Hex(raw),
                now.plus(ttl),
                now
        );
        emailVerificationTokenRepository.save(token);
        return new GeneratedToken(id, raw, token.getExpiresAt());
    }

    @Transactional
    public User verifyEmail(String rawToken) {
        ParsedToken parsed = parseToken(rawToken, "evt");
        Instant now = Instant.now(clock);

        EmailVerificationToken token = emailVerificationTokenRepository.findWithUserById(parsed.id())
            .orElseThrow(() -> new BadRequestException("invalid_token"));

        String providedHash = TokenHashUtil.sha256Hex(rawToken);
        if (!TokenHashUtil.constantTimeEqualsHex(token.getTokenHash(), providedHash)) {
            throw new BadRequestException("invalid_token");
        }

        if (token.getUsedAt() != null) {
            throw new BadRequestException("token_already_used");
        }

        if (token.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("token_expired");
        }

        token.markUsed(now);
        emailVerificationTokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerified(true);
        return user;
    }

    public GeneratedToken generatePasswordResetToken(User user, Duration ttl) {
        Instant now = Instant.now(clock);
        UUID id = UUID.randomUUID();
        String raw = formatToken("prt", id);

        PasswordResetToken token = new PasswordResetToken(
                id,
                user,
                TokenHashUtil.sha256Hex(raw),
                now.plus(ttl),
                now
        );
        passwordResetTokenRepository.save(token);
        return new GeneratedToken(id, raw, token.getExpiresAt());
    }

    @Transactional
    public User verifyPasswordResetToken(String rawToken) {
        ParsedToken parsed = parseToken(rawToken, "prt");
        Instant now = Instant.now(clock);

        PasswordResetToken token = passwordResetTokenRepository.findWithUserById(parsed.id())
            .orElseThrow(() -> new BadRequestException("invalid_token"));

        String providedHash = TokenHashUtil.sha256Hex(rawToken);
        if (!TokenHashUtil.constantTimeEqualsHex(token.getTokenHash(), providedHash)) {
            throw new BadRequestException("invalid_token");
        }

        if (token.getUsedAt() != null) {
            throw new BadRequestException("token_already_used");
        }

        if (token.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("token_expired");
        }

        token.markUsed(now);
        passwordResetTokenRepository.save(token);

        return token.getUser();
    }

    @Transactional
    public int revokeAllActiveRefreshTokensForUser(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now(clock));
    }

    private static String buildDeviceInfo(String sessionExternalId, HttpServletRequest request) {
        String ua = HttpRequestUtil.extractUserAgent(request);
        String sid = sessionExternalId == null ? "" : sessionExternalId;
        String userAgent = ua == null ? "" : ua;
        return "sessionId=" + sid + ";userAgent=" + userAgent;
    }

    private static String extractSessionExternalId(String deviceInfo) {
        if (deviceInfo == null || deviceInfo.isBlank()) {
            return null;
        }
        // deviceInfo format: sessionId=<id>;userAgent=<ua>
        String prefix = "sessionId=";
        int start = deviceInfo.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        int valueStart = start + prefix.length();
        int end = deviceInfo.indexOf(';', valueStart);
        if (end < 0) {
            end = deviceInfo.length();
        }
        String value = deviceInfo.substring(valueStart, end);
        return value.isBlank() ? null : value;
    }

    private record ParsedToken(UUID id) {
    }

    private static ParsedToken parseToken(String rawToken, String expectedPrefix) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("token must not be blank");
        }
        String trimmed = rawToken.trim();
        String prefix = expectedPrefix + "_";
        if (!trimmed.startsWith(prefix)) {
            throw new BadRequestException("token prefix mismatch");
        }
        int dot = trimmed.indexOf('.', prefix.length());
        if (dot < 0) {
            throw new BadRequestException("token format invalid");
        }
        String uuidPart = trimmed.substring(prefix.length(), dot);
        try {
            return new ParsedToken(UUID.fromString(uuidPart));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("token id invalid");
        }
    }

    private static String formatToken(String prefix, UUID id) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return prefix + "_" + id + "." + secret;
    }
}

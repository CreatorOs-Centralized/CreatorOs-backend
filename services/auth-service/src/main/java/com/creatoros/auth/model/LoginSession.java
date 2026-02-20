package com.creatoros.auth.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_sessions")
/**
 * Auth-service owned login session record.
 *
 * <p>Boundary:
 * <ul>
 *   <li>Internal security/audit metadata only (session id, revocation, timestamps, client hints).</li>
 *   <li>Not a general-purpose analytics event stream.</li>
 *   <li>Downstream services must not depend on this table.</li>
 * </ul>
 */
public class LoginSession {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "login_at")
    private Instant loginAt;

    @Column(name = "logout_at")
    private Instant logoutAt;

    @Column(name = "is_success", nullable = false)
    private boolean success = true;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    protected LoginSession() {
    }

    public LoginSession(UUID id, User user, Instant loginAt) {
        this.id = id;
        this.user = user;
        this.loginAt = loginAt;
        this.success = true;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void markLoggedOut(Instant logoutAt) {
        this.logoutAt = logoutAt;
    }

    public void markFailure(String reason) {
        this.success = false;
        this.failureReason = reason;
    }

    public Instant getLoginAt() {
        return loginAt;
    }

    public Instant getLogoutAt() {
        return logoutAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

}

package com.creatoros.auth.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "login_sessions")
@EntityListeners(AuditingEntityListener.class)
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

    @Column(name = "external_session_id", nullable = false, length = 200)
    private String externalSessionId;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LoginSession() {
    }

    public LoginSession(UUID id, User user, String externalSessionId, Instant startedAt) {
        this.id = id;
        this.user = user;
        this.externalSessionId = externalSessionId;
        this.startedAt = startedAt;
        this.revoked = false;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getExternalSessionId() {
        return externalSessionId;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void revoke(Instant endedAt) {
        this.revoked = true;
        this.endedAt = endedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

package com.creatoros.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreferences {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = false;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "publish_success_alerts", nullable = false)
    private boolean publishSuccessAlerts = true;

    @Column(name = "publish_failure_alerts", nullable = false)
    private boolean publishFailureAlerts = true;

    @Column(name = "schedule_reminders", nullable = false)
    private boolean scheduleReminders = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserNotificationPreferences() {
    }

    public UserNotificationPreferences(UUID userId) {
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isPublishSuccessAlerts() {
        return publishSuccessAlerts;
    }

    public void setPublishSuccessAlerts(boolean publishSuccessAlerts) {
        this.publishSuccessAlerts = publishSuccessAlerts;
    }

    public boolean isPublishFailureAlerts() {
        return publishFailureAlerts;
    }

    public void setPublishFailureAlerts(boolean publishFailureAlerts) {
        this.publishFailureAlerts = publishFailureAlerts;
    }

    public boolean isScheduleReminders() {
        return scheduleReminders;
    }

    public void setScheduleReminders(boolean scheduleReminders) {
        this.scheduleReminders = scheduleReminders;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

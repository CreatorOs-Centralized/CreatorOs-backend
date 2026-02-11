-- Notification service schema (notification_db)

CREATE TABLE IF NOT EXISTS user_notification_preferences (
    user_id UUID PRIMARY KEY,

    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    publish_success_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    publish_failure_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_reminders BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    message TEXT,

    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_notifications_channel CHECK (channel IN ('EMAIL', 'IN_APP', 'PUSH')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at ON notifications(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS notification_queue (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,

    provider VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,

    attempts INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    last_error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_notification_queue_provider CHECK (provider IN ('MAILERSEND')),
    CONSTRAINT chk_notification_queue_status CHECK (status IN ('PENDING', 'RETRY', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_notification_queue_status_next_retry_at ON notification_queue(status, next_retry_at);

CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,

    log_level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    details JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_notification_logs_level CHECK (log_level IN ('INFO', 'WARN', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_notification_logs_notification_created_at ON notification_logs(notification_id, created_at DESC);

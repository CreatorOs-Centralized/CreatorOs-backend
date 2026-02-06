-- Adds additional attributes and token tables required by the Auth DB ERD.
-- This migration is additive and safe for existing environments.

-- USERS
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users(email);

-- ROLES
ALTER TABLE roles ADD COLUMN IF NOT EXISTS description TEXT;

-- USER_ROLES
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- LOGIN_SESSIONS
ALTER TABLE login_sessions ADD COLUMN IF NOT EXISTS login_at TIMESTAMPTZ;
ALTER TABLE login_sessions ADD COLUMN IF NOT EXISTS logout_at TIMESTAMPTZ;
ALTER TABLE login_sessions ADD COLUMN IF NOT EXISTS is_success BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE login_sessions ADD COLUMN IF NOT EXISTS failure_reason TEXT;

-- REFRESH_TOKENS
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS device_info TEXT;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64);

-- EMAIL_VERIFICATION_TOKENS
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_expires_at ON email_verification_tokens(expires_at);

-- PASSWORD_RESET_TOKENS
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

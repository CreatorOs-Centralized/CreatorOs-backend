-- Aligns auth_db schema to the Auth DB ERD (UUID ids, strict nullability, and exact column sets).
--
-- NOTE: This migration performs a table rebuild (create *_new, copy, swap) to safely change primary key types
-- and remove legacy columns.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Safety checks: prevent lossy migrations.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE id IS NULL OR btrim(id) = '') THEN
        RAISE EXCEPTION 'users.id contains blank/null values; cannot migrate to UUID';
    END IF;
    IF EXISTS (SELECT 1 FROM users WHERE email IS NULL OR btrim(email) = '') THEN
        RAISE EXCEPTION 'users.email contains blank/null values; ERD requires NOT NULL';
    END IF;
    -- Ensure all current user ids are UUID-like.
    PERFORM id::uuid FROM users;
EXCEPTION
    WHEN invalid_text_representation THEN
        RAISE EXCEPTION 'users.id contains values that cannot be cast to UUID';
END $$;

-- USERS (ERD)
CREATE TABLE IF NOT EXISTS users_new (
    id UUID PRIMARY KEY,
    email VARCHAR(320) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO users_new (id, email, password_hash, is_email_verified, is_active, created_at, updated_at)
SELECT
    u.id::uuid,
    lower(btrim(u.email)),
    CASE
        WHEN u.password_hash IS NULL OR btrim(u.password_hash) = ''
            THEN crypt(gen_random_uuid()::text, gen_salt('bf'))
        ELSE u.password_hash
    END,
    CASE
        WHEN u.password_hash IS NULL OR btrim(u.password_hash) = ''
            THEN FALSE
        ELSE COALESCE(u.is_email_verified, FALSE)
    END,
    CASE
        WHEN u.password_hash IS NULL OR btrim(u.password_hash) = ''
            THEN FALSE
        ELSE COALESCE(u.is_active, TRUE)
    END,
    COALESCE(u.created_at, NOW()),
    COALESCE(u.updated_at, NOW())
FROM users u;

-- ROLES (ERD)
CREATE TABLE IF NOT EXISTS roles_new (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles_new (id, name, description, created_at)
SELECT
    gen_random_uuid(),
    r.name,
    r.description,
    COALESCE(r.created_at, NOW())
FROM roles r;

-- Map old role ids -> new role ids by unique name.
CREATE TEMP TABLE role_id_map AS
SELECT r.id AS old_id, rn.id AS new_id
FROM roles r
JOIN roles_new rn ON rn.name = r.name;

-- USER_ROLES (ERD)
CREATE TABLE IF NOT EXISTS user_roles_new (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users_new(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles_new(id) ON DELETE CASCADE
);

INSERT INTO user_roles_new (user_id, role_id, assigned_at)
SELECT
    ur.user_id::uuid,
    m.new_id,
    COALESCE(ur.assigned_at, NOW())
FROM user_roles ur
JOIN role_id_map m ON m.old_id = ur.role_id;

-- LOGIN_SESSIONS (ERD)
CREATE TABLE IF NOT EXISTS login_sessions_new (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    login_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    logout_at TIMESTAMPTZ,
    ip_address VARCHAR(64),
    user_agent TEXT,
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    failure_reason TEXT,
    CONSTRAINT fk_login_sessions_user FOREIGN KEY (user_id) REFERENCES users_new(id) ON DELETE CASCADE
);

INSERT INTO login_sessions_new (id, user_id, login_at, logout_at, ip_address, user_agent, is_success, failure_reason)
SELECT
    ls.id,
    ls.user_id::uuid,
    COALESCE(ls.login_at, ls.started_at, NOW()),
    COALESCE(ls.logout_at, ls.ended_at),
    ls.ip_address,
    ls.user_agent,
    COALESCE(ls.is_success, TRUE),
    ls.failure_reason
FROM login_sessions ls;

-- REFRESH_TOKENS (ERD)
CREATE TABLE IF NOT EXISTS refresh_tokens_new (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    device_info TEXT,
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users_new(id) ON DELETE CASCADE
);

INSERT INTO refresh_tokens_new (id, user_id, token_hash, expires_at, revoked_at, device_info, ip_address, created_at)
SELECT
    rt.id,
    rt.user_id::uuid,
    rt.token_hash::text,
    rt.expires_at,
    CASE
        WHEN COALESCE(rt.revoked, FALSE) THEN COALESCE(rt.revoked_at, NOW())
        ELSE rt.revoked_at
    END,
    rt.device_info,
    rt.ip_address,
    COALESCE(rt.created_at, NOW())
FROM refresh_tokens rt;

-- EMAIL_VERIFICATION_TOKENS (ERD)
CREATE TABLE IF NOT EXISTS email_verification_tokens_new (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users_new(id) ON DELETE CASCADE
);

INSERT INTO email_verification_tokens_new (id, user_id, token_hash, expires_at, used_at, created_at)
SELECT
    t.id,
    t.user_id::uuid,
    t.token_hash::text,
    t.expires_at,
    t.used_at,
    COALESCE(t.created_at, NOW())
FROM email_verification_tokens t;

-- PASSWORD_RESET_TOKENS (ERD)
CREATE TABLE IF NOT EXISTS password_reset_tokens_new (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users_new(id) ON DELETE CASCADE
);

INSERT INTO password_reset_tokens_new (id, user_id, token_hash, expires_at, used_at, created_at)
SELECT
    t.id,
    t.user_id::uuid,
    t.token_hash::text,
    t.expires_at,
    t.used_at,
    COALESCE(t.created_at, NOW())
FROM password_reset_tokens t;

-- Swap tables (drop old, rename new).
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
ALTER TABLE password_reset_tokens_new RENAME TO password_reset_tokens;

DROP TABLE IF EXISTS email_verification_tokens CASCADE;
ALTER TABLE email_verification_tokens_new RENAME TO email_verification_tokens;

DROP TABLE IF EXISTS refresh_tokens CASCADE;
ALTER TABLE refresh_tokens_new RENAME TO refresh_tokens;

DROP TABLE IF EXISTS login_sessions CASCADE;
ALTER TABLE login_sessions_new RENAME TO login_sessions;

DROP TABLE IF EXISTS user_roles CASCADE;
ALTER TABLE user_roles_new RENAME TO user_roles;

DROP TABLE IF EXISTS roles CASCADE;
ALTER TABLE roles_new RENAME TO roles;

DROP TABLE IF EXISTS users CASCADE;
ALTER TABLE users_new RENAME TO users;

-- Cleanup
DROP TABLE IF EXISTS role_id_map;

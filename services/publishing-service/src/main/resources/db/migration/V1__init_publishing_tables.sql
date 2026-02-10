-- Connected Accounts table
CREATE TABLE IF NOT EXISTS connected_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    account_type VARCHAR(100) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    platform_account_id VARCHAR(255) NOT NULL,
    page_id VARCHAR(255),
    ig_user_id VARCHAR(255),
    youtube_channel_id VARCHAR(255),
    linkedin_author_urn VARCHAR(255),
    access_token_enc TEXT,
    refresh_token_enc TEXT,
    token_expires_at TIMESTAMP,
    scopes TEXT,
    is_active BOOLEAN DEFAULT true,
    connected_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_platform_account UNIQUE(user_id, platform, platform_account_id)
);

-- Publish Jobs table
CREATE TABLE IF NOT EXISTS publish_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    connected_account_id UUID NOT NULL,
    content_item_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    post_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    max_retries INTEGER DEFAULT 3,
    current_retry_count INTEGER DEFAULT 0,
    idempotency_key VARCHAR(255) UNIQUE,
    payload_snapshot JSONB,
    last_error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_connected_account FOREIGN KEY(connected_account_id) REFERENCES connected_accounts(id)
);

-- Publish Job Attempts table
CREATE TABLE IF NOT EXISTS publish_job_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publish_job_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    request_payload JSONB,
    response_payload JSONB,
    http_status INTEGER,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_publish_job FOREIGN KEY(publish_job_id) REFERENCES publish_jobs(id) ON DELETE CASCADE
);

-- Published Posts table
CREATE TABLE IF NOT EXISTS published_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publish_job_id UUID NOT NULL,
    connected_account_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    platform_post_id VARCHAR(255) NOT NULL,
    platform_media_id VARCHAR(255),
    permalink_url TEXT NOT NULL,
    log_level VARCHAR(50),
    message TEXT,
    details JSONB,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_publish_job_id FOREIGN KEY(publish_job_id) REFERENCES publish_jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_connected_account_id FOREIGN KEY(connected_account_id) REFERENCES connected_accounts(id)
);

-- Create indexes for performance
CREATE INDEX idx_connected_accounts_user_id ON connected_accounts(user_id);
CREATE INDEX idx_connected_accounts_platform ON connected_accounts(platform);
CREATE INDEX idx_publish_jobs_user_id ON publish_jobs(user_id);
CREATE INDEX idx_publish_jobs_status ON publish_jobs(status);
CREATE INDEX idx_publish_jobs_scheduled_at ON publish_jobs(scheduled_at);
CREATE INDEX idx_publish_job_attempts_publish_job_id ON publish_job_attempts(publish_job_id);
CREATE INDEX idx_published_posts_publish_job_id ON published_posts(publish_job_id);
CREATE INDEX idx_published_posts_connected_account_id ON published_posts(connected_account_id);
CREATE INDEX idx_published_posts_platform ON published_posts(platform);

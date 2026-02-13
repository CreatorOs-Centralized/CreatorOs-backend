-- Create scheduled_jobs table
CREATE TABLE scheduled_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    content_item_id UUID NOT NULL,
    connected_account_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create index for efficient job lookup
CREATE INDEX idx_scheduled_jobs_status_time 
ON scheduled_jobs(status, scheduled_at);

-- Create index for user lookups
CREATE INDEX idx_scheduled_jobs_user_id 
ON scheduled_jobs(user_id);

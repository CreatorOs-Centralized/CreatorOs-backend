CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS campaigns (
    id UUID PRIMARY KEY,
    user_id UUID,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_states (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS content_items (
    id UUID PRIMARY KEY,
    user_id UUID,
    campaign_id UUID REFERENCES campaigns(id),
    workflow_state_id UUID REFERENCES workflow_states(id),
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content_type VARCHAR(50),
    status VARCHAR(50),
    scheduled_at TIMESTAMP,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS content_versions (
    id UUID PRIMARY KEY,
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    body TEXT,
    metadata JSONB,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_content_versions UNIQUE (content_item_id, version_number)
);

CREATE TABLE IF NOT EXISTS content_platform_variants (
    id UUID PRIMARY KEY,
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    variant_type VARCHAR(50) NOT NULL,
    value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tags (
    id UUID PRIMARY KEY,
    user_id UUID,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tags_user_name UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS content_tags (
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_content_tags PRIMARY KEY (content_item_id, tag_id)
);

CREATE TABLE IF NOT EXISTS content_assets (
    id UUID PRIMARY KEY,
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL,
    asset_role VARCHAR(50),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_history (
    id UUID PRIMARY KEY,
    content_item_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
    from_state_id UUID REFERENCES workflow_states(id),
    to_state_id UUID REFERENCES workflow_states(id),
    changed_by UUID,
    comment TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_campaigns_user_id ON campaigns(user_id);
CREATE INDEX IF NOT EXISTS idx_content_items_user_id ON content_items(user_id);
CREATE INDEX IF NOT EXISTS idx_content_items_campaign_id ON content_items(campaign_id);
CREATE INDEX IF NOT EXISTS idx_content_items_state_id ON content_items(workflow_state_id);
CREATE INDEX IF NOT EXISTS idx_content_versions_item_id ON content_versions(content_item_id);
CREATE INDEX IF NOT EXISTS idx_content_platform_variants_item_id ON content_platform_variants(content_item_id);
CREATE INDEX IF NOT EXISTS idx_content_assets_item_id ON content_assets(content_item_id);
CREATE INDEX IF NOT EXISTS idx_workflow_history_item_id ON workflow_history(content_item_id);

INSERT INTO workflow_states (id, name, sort_order, is_terminal)
VALUES
    (gen_random_uuid(), 'IDEA', 1, FALSE),
    (gen_random_uuid(), 'DRAFT', 2, FALSE),
    (gen_random_uuid(), 'REVIEW', 3, FALSE),
    (gen_random_uuid(), 'APPROVED', 4, FALSE),
    (gen_random_uuid(), 'SCHEDULED', 5, FALSE),
    (gen_random_uuid(), 'PUBLISHED', 6, TRUE)
ON CONFLICT (name) DO NOTHING;
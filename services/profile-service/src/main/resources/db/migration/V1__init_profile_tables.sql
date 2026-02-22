-- =============================================
-- Profile Service Database Schema
-- =============================================
-- This migration creates the initial tables for the Profile Service
-- including creator_profiles, social_links, profile_highlights,
-- profile_verifications, and media_kits tables.

-- =============================================
-- Table: creator_profiles
-- =============================================
-- Stores creator profile information including bio, display name,
-- profile/banner images, and additional metadata.

CREATE TABLE IF NOT EXISTS creator_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    bio TEXT,
    niche VARCHAR(100),
    profile_photo_url VARCHAR(255),
    cover_photo_url VARCHAR(255),
    location VARCHAR(100),
    language VARCHAR(50),
    is_public BOOLEAN NOT NULL DEFAULT true,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    verification_level VARCHAR(50) DEFAULT 'NONE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for frequently queried columns
CREATE INDEX IF NOT EXISTS idx_creator_profiles_user_id ON creator_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_creator_profiles_username ON creator_profiles(username);

-- =============================================
-- Table: social_links
-- =============================================
-- Stores social media links associated with creator profiles.

CREATE TABLE IF NOT EXISTS social_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_profile_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    handle VARCHAR(100),
    url VARCHAR(500),
    is_verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_social_links_profile
        FOREIGN KEY (creator_profile_id) 
        REFERENCES creator_profiles(id)
        ON DELETE CASCADE
);

-- Create indexes for social_links
CREATE INDEX IF NOT EXISTS idx_social_links_profile_id ON social_links(creator_profile_id);

-- =============================================
-- Table: profile_highlights
-- =============================================
-- Stores highlighted content items for creator profiles.

CREATE TABLE IF NOT EXISTS profile_highlights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_profile_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    highlight_type VARCHAR(50) NOT NULL,
    thumbnail_url VARCHAR(500),
    target_url VARCHAR(500) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_profile_highlights_profile
        FOREIGN KEY (creator_profile_id) 
        REFERENCES creator_profiles(id)
        ON DELETE CASCADE
);

-- Create indexes for profile_highlights
CREATE INDEX IF NOT EXISTS idx_profile_highlights_profile_id ON profile_highlights(creator_profile_id);
CREATE INDEX IF NOT EXISTS idx_profile_highlights_sort_order ON profile_highlights(creator_profile_id, sort_order);

-- =============================================
-- Table: profile_verifications
-- =============================================
-- Stores verification requests and status for creator profiles.

CREATE TABLE IF NOT EXISTS profile_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_profile_id UUID NOT NULL,
    verification_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP,
    verified_by UUID,
    
    -- Foreign key constraint
    CONSTRAINT fk_profile_verifications_profile
        FOREIGN KEY (creator_profile_id) 
        REFERENCES creator_profiles(id)
        ON DELETE CASCADE
);

-- Create indexes for profile_verifications
CREATE INDEX IF NOT EXISTS idx_profile_verifications_profile_id ON profile_verifications(creator_profile_id);
CREATE INDEX IF NOT EXISTS idx_profile_verifications_status ON profile_verifications(status);

-- =============================================
-- Table: media_kits
-- =============================================
-- Stores media kit information for creator profiles.

CREATE TABLE IF NOT EXISTS media_kits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_profile_id UUID NOT NULL,
    kit_title VARCHAR(255) DEFAULT 'Media Kit',
    about_creator TEXT,
    collaboration_notes TEXT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    portfolio_url VARCHAR(500),
    pdf_url VARCHAR(500),
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_media_kits_profile
        FOREIGN KEY (creator_profile_id) 
        REFERENCES creator_profiles(id)
        ON DELETE CASCADE
);

-- Create indexes for media_kits
CREATE INDEX IF NOT EXISTS idx_media_kits_profile_id ON media_kits(creator_profile_id);

-- =============================================
-- Comments on tables and columns
-- =============================================

COMMENT ON TABLE creator_profiles IS 'Stores creator profile information and metadata';
COMMENT ON COLUMN creator_profiles.user_id IS 'Foreign key reference to the user in auth-service';
COMMENT ON COLUMN creator_profiles.display_name IS 'Public display name shown on the profile';
COMMENT ON COLUMN creator_profiles.username IS 'Unique username/handle for the creator';
COMMENT ON COLUMN creator_profiles.bio IS 'Creator biography or description';
COMMENT ON COLUMN creator_profiles.verification_level IS 'Verification level: NONE, EMAIL, SOCIAL, ID, ADMIN';

COMMENT ON TABLE social_links IS 'Social media links associated with creator profiles';
COMMENT ON COLUMN social_links.platform IS 'Social media platform (INSTAGRAM, YOUTUBE, LINKEDIN, TWITTER, FACEBOOK, WEBSITE)';
COMMENT ON COLUMN social_links.handle IS 'Username or handle on the platform';

COMMENT ON TABLE profile_highlights IS 'Highlighted content items showcased on creator profiles';
COMMENT ON COLUMN profile_highlights.highlight_type IS 'Type of highlight: POST, LINK, VIDEO, IMAGE, etc.';
COMMENT ON COLUMN profile_highlights.sort_order IS 'Order in which highlights should be displayed';

COMMENT ON TABLE profile_verifications IS 'Verification requests and status tracking';
COMMENT ON COLUMN profile_verifications.verification_type IS 'Type: EMAIL, SOCIAL, ID, ADMIN';
COMMENT ON COLUMN profile_verifications.status IS 'Status: PENDING, APPROVED, REJECTED';
COMMENT ON COLUMN profile_verifications.verified_by IS 'Admin user ID who performed verification';

COMMENT ON TABLE media_kits IS 'Media kit information for brand collaborations';
COMMENT ON COLUMN media_kits.version IS 'Version number for tracking media kit updates';

-- Initialize multiple databases for CreatorOS services

-- Create auth_db
CREATE DATABASE auth_db;

-- Create profile_db
CREATE DATABASE profile_db;

-- Create content_db
CREATE DATABASE content_db;

-- Create asset_db
CREATE DATABASE asset_db;

-- Create publishing_db
CREATE DATABASE publishing_db;

-- Create scheduler_db
CREATE DATABASE scheduler_db;

-- Note: Extensions like pgcrypto will be created by Flyway migrations
-- when each service starts and runs its migrations

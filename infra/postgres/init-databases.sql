-- Initialize multiple databases for CreatorOS services

-- Create profile_db
CREATE DATABASE profile_db;

-- Create content_db
CREATE DATABASE content_db;

-- Note: Extensions like pgcrypto will be created by Flyway migrations
-- when each service starts and runs its migrations

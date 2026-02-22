ALTER TABLE user_notification_preferences
    ADD COLUMN IF NOT EXISTS id UUID;

UPDATE user_notification_preferences
SET id = user_id
WHERE id IS NULL;

ALTER TABLE user_notification_preferences
    ALTER COLUMN id SET NOT NULL;

ALTER TABLE user_notification_preferences
    DROP CONSTRAINT IF EXISTS user_notification_preferences_pkey;

ALTER TABLE user_notification_preferences
    ADD CONSTRAINT user_notification_preferences_pkey PRIMARY KEY (id);

ALTER TABLE user_notification_preferences
    ADD CONSTRAINT uq_user_notification_preferences_user_id UNIQUE (user_id);

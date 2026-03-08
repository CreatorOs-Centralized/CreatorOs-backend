ALTER TABLE connected_accounts
    ADD COLUMN IF NOT EXISTS instagram_business_account_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS facebook_page_id VARCHAR(255);

ALTER TABLE users
    ADD profile_image VARCHAR(255) NULL;

ALTER TABLE social_accounts
    DROP COLUMN token_secret;

ALTER TABLE social_accounts
    MODIFY token VARCHAR(255) NOT NULL;

ALTER TABLE social_accounts
    MODIFY token_expires datetime NOT NULL;

ALTER TABLE threads
    MODIFY updated_at datetime NOT NULL;

ALTER TABLE user_preferences
    MODIFY updated_at datetime NOT NULL;

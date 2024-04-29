ALTER TABLE user_preferences
    ADD CONSTRAINT uc_user_preferences_user UNIQUE (user_id);

ALTER TABLE user_preferences
    MODIFY model_version VARCHAR(255) NOT NULL;

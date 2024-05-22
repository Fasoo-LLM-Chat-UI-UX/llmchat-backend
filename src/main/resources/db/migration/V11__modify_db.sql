ALTER TABLE bookmarks
    ADD user_id BIGINT NULL;

ALTER TABLE bookmarks
    MODIFY user_id BIGINT NOT NULL;

ALTER TABLE bookmarks
    ADD CONSTRAINT FK_BOOKMARKS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);
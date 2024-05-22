CREATE TABLE llmchat.bookmarks
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    message_id        BIGINT                NOT NULL,
    title             VARCHAR(255)          NOT NULL,
    emoji             VARCHAR(255)          NOT NULL,
    user_message      TEXT                  NOT NULL,
    assistant_message TEXT                  NOT NULL,
    created_at        datetime              NOT NULL,
    updated_at        datetime              NOT NULL,
    deleted_at        datetime              NULL,
    PRIMARY KEY (id)
);

ALTER TABLE llmchat.bookmarks
    ADD CONSTRAINT FK_BOOKMARKS_ON_MESSAGE FOREIGN KEY (message_id) REFERENCES llmchat.messages (id)
    ON DELETE SET NULL;

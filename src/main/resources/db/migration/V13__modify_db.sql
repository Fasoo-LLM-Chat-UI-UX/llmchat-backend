ALTER TABLE bookmarks
    ADD CONSTRAINT uc_bookmarks_message UNIQUE (message_id);

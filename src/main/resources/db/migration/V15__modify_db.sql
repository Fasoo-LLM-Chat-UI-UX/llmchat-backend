-- Table to store shared threads information with shared_key
CREATE TABLE shared_threads
(
    id         bigint auto_increment PRIMARY KEY,
    user_id    bigint       NOT NULL,
    thread_id  bigint       NOT NULL,
    message_id bigint       NOT NULL,
    shared_key varchar(255) NOT NULL,
    shared_at  datetime(6)  NOT NULL,
    constraint FK_SHARED_THREADS_ON_USER
        FOREIGN KEY (user_id) REFERENCES users (id),
    constraint FK_SHARED_THREADS_ON_THREAD
        FOREIGN KEY (thread_id) REFERENCES threads (id),
    constraint FK_SHARED_THREADS_ON_MESSAGE
        FOREIGN KEY (message_id) REFERENCES messages (id)
);

-- Creating indexes for quick look-up
CREATE INDEX idx_shared_threads_shared_key ON shared_threads (shared_key);

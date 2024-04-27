CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(255) UNIQUE NOT NULL,
    password      VARCHAR(255),
    email         VARCHAR(255),
    mobile_number VARCHAR(255),
    name          VARCHAR(255)        NOT NULL,
    date_joined   DATETIME(6)         NOT NULL,
    last_login    DATETIME(6)
);

CREATE TABLE social_accounts
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    provider      VARCHAR(255) NOT NULL,
    uid           VARCHAR(255) NOT NULL,
    last_login    DATETIME(6),
    date_joined   DATETIME(6)  NOT NULL,
    token         VARCHAR(255),
    token_secret  VARCHAR(255),
    token_expires DATETIME(6),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_preferences
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id               BIGINT       NOT NULL,
    ui_theme              VARCHAR(255) NOT NULL,
    ui_language_code      VARCHAR(255) NOT NULL,
    speech_voice          VARCHAR(255) NOT NULL,
    about_model_message   VARCHAR(1000),
    about_user_message    VARCHAR(1000),
    about_message_enabled BOOLEAN      NOT NULL,
    model_version         VARCHAR(255),
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE threads
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    chat_name  VARCHAR(255) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE messages
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    thread_id BIGINT       NOT NULL,
    role      VARCHAR(255) NOT NULL,
    content   TEXT         NOT NULL,
    FOREIGN KEY (thread_id) REFERENCES threads (id)
);

CREATE TABLE archived_threads
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    emoji        VARCHAR(10),
    chat_name    VARCHAR(255) NOT NULL,
    chat_content JSON         NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    archived_at  DATETIME(6)  NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE deleted_threads
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    chat_name    VARCHAR(255) NOT NULL,
    chat_content JSON         NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    deleted_at   DATETIME(6)  NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

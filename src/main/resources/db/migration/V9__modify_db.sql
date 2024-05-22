ALTER TABLE archived_threads
    DROP FOREIGN KEY archived_threads_ibfk_1;

ALTER TABLE deleted_threads
    DROP FOREIGN KEY deleted_threads_ibfk_1;

ALTER TABLE threads
    ADD deleted_at datetime(6) NULL;

DROP TABLE archived_threads;

DROP TABLE deleted_threads;

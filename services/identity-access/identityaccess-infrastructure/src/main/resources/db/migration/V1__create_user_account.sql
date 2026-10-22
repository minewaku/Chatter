CREATE TABLE user_account (
    id BIGINT PRIMARY KEY,
    normalized_email VARCHAR(320) NOT NULL UNIQUE,
    username VARCHAR(32) NOT NULL UNIQUE,
    birthday DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    password_algorithm VARCHAR(64) NOT NULL,
    password_hash VARCHAR(512) NOT NULL,
    password_salt BYTEA NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_account_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'DELETED')),
    CONSTRAINT ck_user_account_deletion CHECK (
        (status = 'DELETED' AND deleted_at IS NOT NULL) OR
        (status <> 'DELETED' AND deleted_at IS NULL)
    )
);

CREATE INDEX idx_user_account_status ON user_account (status);
CREATE INDEX idx_user_account_created_at ON user_account (created_at DESC);

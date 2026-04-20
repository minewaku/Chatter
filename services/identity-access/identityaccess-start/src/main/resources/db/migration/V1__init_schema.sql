CREATE TABLE "account" (
    id BIGINT PRIMARY KEY, 
    
    email VARCHAR(255) UNIQUE,
    username VARCHAR(255) UNIQUE,
    birthday DATE,
    is_enabled BOOLEAN DEFAULT TRUE,
    is_locked BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    hashed_password VARCHAR(255),
    password_modified_at TIMESTAMP WITH TIME ZONE,
    algorithm VARCHAR(50),
    salt BYTEA,
    
    -- Giả định 2 trường audit này được kế thừa từ BaseEntity
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    version INTEGER DEFAULT NULL
);

-- Bảng "session"
CREATE TABLE session (
    session_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_info JSONB, 
    generation INTEGER DEFAULT 0,
    issued_at TIMESTAMP WITH TIME ZONE,
    last_refreshed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    
    version INTEGER DEFAULT NULL,

    -- Ràng buộc khóa ngoại
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE
);

-- Bảng "outbox"
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
-- V1__init_schema.sql

-- Bảng "guild"
CREATE TABLE guild (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_hash VARCHAR(255),
    version INTEGER
);
CREATE INDEX idx_guild_user_id ON guild (user_id);


-- Bảng "channel"
CREATE TABLE channel (
    id BIGINT PRIMARY KEY,
    guild_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    version INTEGER,
    CONSTRAINT fk_channel_guild FOREIGN KEY (guild_id) REFERENCES guild (id) ON DELETE CASCADE
);
CREATE INDEX idx_channel_guild_id ON channel (guild_id);


-- Bảng "recipient"
CREATE TABLE recipient (
    guild_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    join_date TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER,
    
    PRIMARY KEY (guild_id, user_id),
    CONSTRAINT fk_recipient_guild FOREIGN KEY (guild_id) REFERENCES guild (id) ON DELETE CASCADE
);
CREATE INDEX idx_recipient_user_id ON recipient (user_id);

-- Bảng "invite"
CREATE TABLE invite (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    guild_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    max_uses INTEGER NOT NULL,
    uses INTEGER NOT NULL,
    duration BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER,
    CONSTRAINT fk_invite_guild FOREIGN KEY (guild_id) REFERENCES guild (id) ON DELETE CASCADE
);
CREATE INDEX idx_invite_guild_id ON invite (guild_id);
CREATE INDEX idx_invite_code ON invite (code);

-- Bảng "asset"
CREATE TABLE asset (
    id BIGINT PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    file_hash VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    -- metadata TEXT,
    file_size INTEGER NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    ref_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unq_namespace_hash UNIQUE (namespace, file_hash)
);
CREATE INDEX idx_asset_file_hash ON asset (file_hash);


-- Bảng "outbox"
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outbox_created_at ON outbox (created_at ASC);
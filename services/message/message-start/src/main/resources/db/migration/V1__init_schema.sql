-- V1__init_schema.sql

-- Bảng "guild"
CREATE TABLE guild (
    id BIGINT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_hash VARCHAR(255)
);
CREATE INDEX idx_guild_owner_id ON guild (owner_id);


-- Bảng "channel"
CREATE TABLE channel (
    id BIGINT PRIMARY KEY,
    guild_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    CONSTRAINT fk_channel_guild FOREIGN KEY (guild_id) REFERENCES guild (id) ON DELETE CASCADE
);
CREATE INDEX idx_channel_guild_id ON channel (guild_id);

-- Bảng "recipient"
CREATE TABLE recipient (

    guild_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    join_date TIMESTAMP WITH TIME ZONE NOT NULL,
    
    PRIMARY KEY (guild_id, user_id),
    CONSTRAINT fk_recipient_guild FOREIGN KEY (guild_id) REFERENCES guild (id) ON DELETE CASCADE
);
CREATE INDEX idx_recipient_user_id ON recipient (user_id);

-- Bảng "asset"
CREATE TABLE asset (
    id BIGINT PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    file_hash VARCHAR(255) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    file_size INTEGER NOT NULL
);
CREATE INDEX idx_asset_file_hash ON asset (file_hash);

-- Bảng "outbox"
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_outbox_created_at ON outbox (created_at ASC);
CREATE TABLE "profile" (
    id BIGINT PRIMARY KEY, 
    
    username VARCHAR(255) UNIQUE,
    display_name VARCHAR(255),
    bio VARCHAR(500),
    avatar VARCHAR(255),
    banner VARCHAR(255),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- RECHECK: OVERLAP FEATURES WITH PREDEFINED FIELDS IN DOMAIN MODEL
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    version INTEGER DEFAULT NULL
);
CREATE INDEX idx_profile_inactive ON profile (id) WHERE is_deleted = TRUE;
CREATE INDEX idx_profile_created_at ON profile (created_at DESC);

-- Bảng "asset"
CREATE TABLE asset (
    id BIGINT PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    file_hash VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
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
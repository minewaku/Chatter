CREATE KEYSPACE IF NOT EXISTS chatter
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'} 
AND durable_writes = true;

USE chatter;

CREATE TABLE IF NOT EXISTS channel_bucket (
    channel_id BIGINT PRIMARY KEY,
    buckets set<int>
);

CREATE TYPE IF NOT EXISTS attachment (
    file_hash TEXT,
    filename TEXT,
    content_type TEXT,
    size BIGINT
);

CREATE TABLE IF NOT EXISTS message (
    id BIGINT,
    channel_id BIGINT,
    bucket INT,
    content TEXT,
    user_id BIGINT,
    reply_id BIGINT,
    attachments LIST<FROZEN<attachment>>,
    timestamp BIGINT,
    
    PRIMARY KEY ((channel_id, bucket), id)
) 
WITH CLUSTERING ORDER BY (id DESC);
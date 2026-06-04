CREATE KEYSPACE IF NOT EXISTS chatter_keyspace 
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'} 
AND durable_writes = true;

USE chatter_keyspace;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT,
    channel_id BIGINT,
    bucket INT,
    content TEXT,
    user_id BIGINT,
    reply_id BIGINT,
    asset_hashes LIST<TEXT>,
    timestamp BIGINT,
    
    PRIMARY KEY ((channel_id, bucket), message_id)
) 

WITH CLUSTERING ORDER BY (message_id DESC);
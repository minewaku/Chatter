CREATE TABLE relay_heartbeat (
    id INTEGER PRIMARY KEY,
    beat_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO relay_heartbeat (id, beat_at) VALUES (1, now());

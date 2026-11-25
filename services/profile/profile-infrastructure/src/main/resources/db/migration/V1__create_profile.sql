-- profile: one row per user who has saved something, plus the queue of provider files waiting to be deleted.
-- No schema name here: Flyway's default schema (profile) applies.

CREATE TABLE profile (
    user_id          bigint PRIMARY KEY,
    display_name     text        NOT NULL DEFAULT '' CHECK (char_length(display_name) <= 1024),
    bio              text        NOT NULL DEFAULT '' CHECK (char_length(bio) <= 1024),
    avatar_public_id text        NULL,
    avatar_url       text        NULL,
    banner_public_id text        NULL,
    banner_url       text        NULL,
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL,
    CHECK ((avatar_public_id IS NULL) = (avatar_url IS NULL)),
    CHECK ((banner_public_id IS NULL) = (banner_url IS NULL))
);

CREATE INDEX profile_avatar_public_id_idx ON profile (avatar_public_id) WHERE avatar_public_id IS NOT NULL;
CREATE INDEX profile_banner_public_id_idx ON profile (banner_public_id) WHERE banner_public_id IS NOT NULL;

CREATE TABLE pending_image_removal (
    public_id       text PRIMARY KEY,
    slot            text        NOT NULL,
    attempts        integer     NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    created_at      timestamptz NOT NULL,
    reported_at     timestamptz NULL
);

CREATE INDEX pending_image_removal_due_idx ON pending_image_removal (next_attempt_at) WHERE reported_at IS NULL;

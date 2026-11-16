-- Image upload redesign: the profile keeps only an image id per slot; the upload ledger tracks every storage key the
-- service authorized or retired and that is not the current image of a profile.
-- Old public ids and urls belonged to another storage provider and cannot be mapped to an image id, so they are dropped.

DROP INDEX IF EXISTS profile_avatar_public_id_idx;
DROP INDEX IF EXISTS profile_banner_public_id_idx;

ALTER TABLE profile
    DROP COLUMN avatar_public_id,
    DROP COLUMN avatar_url,
    DROP COLUMN banner_public_id,
    DROP COLUMN banner_url,
    ADD COLUMN avatar_image_id uuid NULL,
    ADD COLUMN banner_image_id uuid NULL;

DROP TABLE pending_image_removal;

CREATE TABLE pending_upload (
    image_id     uuid PRIMARY KEY,
    user_id      bigint      NOT NULL,
    slot         text        NOT NULL CHECK (slot IN ('AVATAR', 'BANNER')),
    state        text        NOT NULL CHECK (state IN ('PENDING', 'DISCARD')),
    expires_at   timestamptz NULL,
    delete_after timestamptz NOT NULL,
    created_at   timestamptz NOT NULL,
    CHECK ((state = 'PENDING') = (expires_at IS NOT NULL))
);

-- at most one usable ticket per user and slot, even under concurrent requests
CREATE UNIQUE INDEX pending_upload_one_pending_per_slot
    ON pending_upload (user_id, slot) WHERE state = 'PENDING';

-- the sweeper reads the oldest due row first
CREATE INDEX pending_upload_due_idx ON pending_upload (delete_after);

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
    -- set while the ticket can still be committed (a pending upload); null once the key is only waiting to be deleted
    expires_at   timestamptz NULL,
    delete_after timestamptz NOT NULL,
    created_at   timestamptz NOT NULL
);

-- a user may hold any number of pending tickets; committing one retires the others of its slot
CREATE INDEX pending_upload_pending_slot_idx
    ON pending_upload (user_id, slot) WHERE expires_at IS NOT NULL;

-- the sweeper reads the oldest due row first
CREATE INDEX pending_upload_due_idx ON pending_upload (delete_after);

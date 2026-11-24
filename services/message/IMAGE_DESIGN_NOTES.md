# message service: image / attachment design notes

Status: NOTES ONLY. Nothing here has been implemented. These come from a design discussion held while planning the
profile-next image redesign, and are recorded here so this service can be worked on separately later. Re-check them
against this service's real code and specs first.

Since then profile-next shipped a different design: a presigned PUT to Cloudflare R2 plus a pending-upload ledger, with
no provider callback (see `services/profile-next/IMAGE_UPLOAD_DESIGN_DRAFT.md`), and all services are moving to the
one shared R2 bucket, each under its own key prefix. Read the Cloudinary-specific points below (signed `context` and
`tags`, the provider's upload notification, the provider webhook step in the attachment flow, the MD5 `etag`) as history;
the parts about ids, paths, drafts, the attachment record, privacy and content-hash identity still apply.

## Consistency with profile-next

- Reuse `ImageId` as a UUIDv7 (time-ordered, no coordinator, not enumerable).
- Reuse the convention of carrying ownership as signed upload parameters (`context`, `tags`) rather than parsing paths.
  Tag deletion units, for example `channel-{channelId}`. Verify that the provider's upload notification echoes them.
- Each service has its own database, so nothing is shared at the table level. Only the id type and conventions are
  shared.

## Storage path

- Nest only the id that owns the object's lifecycle, for example `chat/{channelId}/{imageId}`.
- An id belongs in a path only if it is immutable for the object's life, is the unit deleted together, and is known when
  the upload is requested.
- Do **not** put `messageId` in the path: the upload usually precedes the message, forwarding or reposting makes the
  path lie, and the DB already owns that relation. Channel deletion is the one case where prefix delete saves real work,
  and `channelId` alone covers it.

## Do not create a blank/draft message at presign time

- Message ids usually define ordering and pagination cursors. An id assigned at draft creation sorts before messages
  sent in between.
- Every message query (history, unread counts, realtime fan-out, search, notifications, moderation) would need a
  `status = SENT` filter, and missing one leaks a half-built message.
- Abandoned drafts need a cleanup job and TTLs, and multiple attachments per message need a "current message"
  concept on the client.
- Use a draft only if the product needs real server-side drafts (compose-in-progress synced across devices, or a
  visible "sending" placeholder for other members).

## Use an attachment (image) record instead

```
POST /channels/{channelId}/attachments       -> presign
  server: check membership, INSERT attachment(id, channelId, uploaderId, status=PENDING, expiresAt)
  returns: presigned upload for key chat/{channelId}/{attachmentId}

provider webhook (signed)                    -> confirm
  server: attachment.status = CONFIRMED (+ format, bytes, width, height if kept)

POST /channels/{channelId}/messages { text, attachmentIds: [...] }
  server, one transaction: verify each attachment is CONFIRMED, owned by the caller, in this channel and
  unattached -> create the message -> set attachment.message_id
```

Lifecycle: `PENDING -> CONFIRMED -> attached (message_id set) -> OBSOLETE`.

- Messages stay clean (no draft status) and message ids are assigned at send time, so ordering is right.
- Expired `PENDING` rows, and `CONFIRMED` rows left unattached too long, become `OBSOLETE`; a sweeper deletes their
  files. This avoids listing the provider to find orphans.
- Forwarding or reposting is a new link row pointing at the same attachment, or a copy if preferred.
- Slack's and Discord's file flows follow the same shape: upload first, reference the file when sending.

Sketch of the table (Postgres):

```sql
attachment
  id          uuid PRIMARY KEY            -- UUIDv7
  key         text NOT NULL UNIQUE
  channel_id  bigint NOT NULL
  owner_id    bigint NOT NULL
  message_id  bigint NULL REFERENCES message(id)
  status      text NOT NULL               -- PENDING | CONFIRMED | OBSOLETE
  format, bytes, width, height            -- as needed
  expires_at, created_at, confirmed_at, obsoleted_at
  delete_attempts, delete_next_attempt_at, delete_reported_at   -- replaces a separate removal queue
```

Costs: one row written per presign request (rate-limit that endpoint), and message sends validate and link attachments
in the same transaction.

## Privacy

Profile images are public by nature, but chat attachments may need to be member-only. A `channelId` in the URL of a
private conversation's image is a different matter than a `userId` in an avatar URL. If attachments must be private,
consider the provider's authenticated delivery with signed URLs (verify plan availability; transformations then need
signing too). Unguessable ids help but are not authorization.

## Content-hash identity may fit here better than in profile

Forwarded memes and reposts do repeat, so deduplication pays off more for chat attachments than for avatars. It is
a significant design step, so decide it deliberately:

- The key can't be pre-signed, because the hash is unknown until the bytes exist. Upload to `incoming/{uploadId}`,
  verify, then promote to `{hash}` (or discard if it already exists).
- The server must verify the hash itself (client-claimed hashes allow referencing someone else's image or poisoning a
  hash). S3 can enforce a declared SHA-256 on a presigned upload; Cloudinary cannot (its `etag` is an MD5).
- Always make the client upload the bytes, so there is no "already uploaded" side channel.
- Deletion needs reference counting or mark-and-sweep with a grace period, and message deletion semantics must be
  decided (the bytes stay while any other message references them).
- Ordering needs a separate upload id, since a hash has no order.

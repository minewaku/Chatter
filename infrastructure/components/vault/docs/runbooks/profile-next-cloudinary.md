# Profile-Next Cloudinary Credentials

`profile-next` lets users upload an avatar or banner straight to Cloudinary with a short-lived signed form POST; the service never receives the file. When the client says the upload is done, the service asks Cloudinary what it stored (size and format), reads the first bytes of the original from the public delivery address, checks them, and only then makes the file the profile's image. Replaced, refused and abandoned files are deleted by the service itself. It reads the account credentials from Vault at startup, from `secret/profilenext`.

| Vault key (either spelling works) | Cloudinary value |
|---|---|
| `cloudinary.cloud-name` or `cloudinary.cloud_name` | Cloud name |
| `cloudinary.api-key` or `cloudinary.api_key` | API key |
| `cloudinary.api-secret` or `cloudinary.api_secret` | API secret |

`configure-secrets.sh` writes the underscore spelling (it turns the first `_` after the service prefix into a dot and keeps the rest). The service accepts both, so either path below works.

## Store the credentials

Vault must be running and unsealed ([bootstrap](bootstrap.md)).

Set `PROFILENEXT_CLOUDINARY_CLOUD_NAME`, `PROFILENEXT_CLOUDINARY_API_KEY` and `PROFILENEXT_CLOUDINARY_API_SECRET` in `infrastructure/components/vault/.env` (gitignored; the committed `.env.example` only lists the names), then push them:

```bash
infrastructure/components/vault/scripts/configure-secrets.sh profilenext
```

The script refuses to run while any of the three is empty or a placeholder. Or update just these keys without touching the rest of the secret (`VAULT_ADDR` and `VAULT_TOKEN` as in [bootstrap](bootstrap.md)):

```bash
vault kv patch secret/profilenext \
  cloudinary.cloud-name="<cloud name>" \
  cloudinary.api-key="<api key>" \
  cloudinary.api-secret="<api secret>"
```

## Verify without printing the values

```bash
vault kv get -format=json secret/profilenext | jq '.data.data | keys'
```

Expect the three `cloudinary.*` key names. Do not print the values; `jq 'keys'` shows names only. If you must check one, compare lengths (`jq '.data.data["cloudinary.api_key"] | length'`).

## Run the service

```bash
export VAULT_TOKEN=<token that can read secret/profilenext and secret/common/jwt/rs256>
cd services/profile-next && ./mvnw -pl profilenext-start spring-boot:run
```

Without credentials the service still starts. Reads and text edits work; requesting an upload permission answers `503 service_unavailable` and the removal and sweep jobs log an error each run until the credentials are present.

## Cloudinary account and service settings

No webhook is involved: the commit call checks the stored file itself, so there is nothing for Cloudinary to call back.

| Item | What to set |
|---|---|
| Signature algorithm | The account's setting (Console, Settings, Security, "Signature algorithm") must match `profile-next.images.signature-algorithm` (`SHA-1` is the default in both). A mismatch makes every upload fail with an invalid-signature error. |
| Public delivery | Uploads are `type=upload` assets. Their original must be publicly deliverable, because the service reads the first bytes through `profile-next.images.delivery-url` (default `https://res.cloudinary.com/<cloud-name>/image/upload`). |
| Shown address | `PROFILE_NEXT_PUBLIC_BASE_URL`, usually the same address as the delivery URL (or a CDN or custom domain in front of it). An image is shown at `{base}/profiles/{userId}/{avatar|banner}/{imageId}`. |
| Cleanup rules | **Do not** add an automatic deletion rule or MediaFlows purge over the `profiles/` prefix. Live and unconfirmed images share it; the service's sweeper deletes the unconfirmed ones. |
| Upload size | A Cloudinary signature cannot pin a file's length. The account's own maximum file size applies at upload; the service enforces the avatar and banner limits when the client commits, and removes a file that is too large. |

The same public id is never uploaded twice on purpose (`overwrite=false`); the id carries a time-ordered image id, so a replaced image is a new address.

## Check the setup

1. Request an upload ticket for a test user and confirm `uploadUrl` is `https://api.cloudinary.com/v1_1/<cloud-name>/image/upload` and `fields` holds `public_id`, `timestamp`, `allowed_formats`, `overwrite`, `api_key` and `signature`.
2. POST those fields plus the file (as multipart form data) to that address. An invalid-signature answer means the algorithm or the credentials do not match the account.
3. Call commit with the `imageId`. Expect `200`, and the asset under `profiles/<userId>/<slot>/<imageId>` in the Media Library.
4. Open the shown address in a browser. If it does not load, `public-base-url` or the account's delivery restrictions are wrong, and the commit header read would fail the same way.

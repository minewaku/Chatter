# Profile-Next Cloudinary Credentials

`profile-next` gives users a signed permission to upload their avatar or banner straight to Cloudinary, checks Cloudinary's signed webhook, and deletes replaced files. It reads the account credentials from Vault at startup, from `secret/profilenext`.

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

## Webhook

Cloudinary must reach `POST /api/v1/profiles/uploads/notifications`. Set `profile-next.images.webhook-url` to the public address of that path (a tunnel in local development); it is signed into every upload permission as `notification_url`. If your Cloudinary account signs notifications with SHA-256, set `profile-next.images.signature-algorithm=SHA-256`.

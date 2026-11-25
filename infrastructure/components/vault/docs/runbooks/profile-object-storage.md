# Object Storage (Cloudflare R2)

`profile` lets users upload an avatar or banner straight to Cloudflare R2 with a short-lived presigned PUT; the service never receives the file. It reads the account and key pair from the shared secret `secret/common/storage` at startup. One bucket (`chatter`) serves every service; each service keeps its files under its own key prefix (`profile` uses `profiles/`).

| Vault key | Value | `.env` variable |
|-----------|-------|-----------------|
| `storage.account-id` | Cloudflare account id (R2 overview, *Account Details*) | `STORAGE_ACCOUNT_ID` |
| `storage.bucket` | Bucket name | `STORAGE_BUCKET` |
| `storage.access-key` | S3 Access Key ID of the R2 API token | `STORAGE_ACCESS_KEY` |
| `storage.secret-key` | S3 Secret Access Key of the R2 API token | `STORAGE_SECRET_KEY` |

The S3 endpoint is built by the service as `https://<account-id>.r2.cloudflarestorage.com`; there is no variable for it. The token value shown by Cloudflare is not used.

## Create the token

R2 object storage, then *API*, then *Manage API tokens*: create an **Account API token** with the **Object Read & Write** permission, limited to the bucket. Cloudflare shows the Access Key ID and Secret Access Key once.

## Store the credentials

Vault must be running and unsealed ([bootstrap](bootstrap.md)). Set the four variables in `infrastructure/components/vault/.env` (gitignored; the committed `.env.example` only lists the names), then push them:

```bash
infrastructure/components/vault/scripts/configure-secrets.sh storage
```

## Verify without printing the values

```bash
vault kv get -format=json secret/common/storage | jq '.data.data | keys'
```

Expect `storage.access-key`, `storage.account-id`, `storage.bucket` and `storage.secret-key`.

## Service settings that are not secrets

`PROFILE_PUBLIC_BASE_URL` is the address images are shown at: the bucket's `r2.dev` URL for development, a custom domain attached to the bucket for production. Enable it in the bucket's *Settings*. Browsers also need a CORS rule on the bucket allowing `PUT` with the `Content-Type` and `If-None-Match` headers from the web app origin.

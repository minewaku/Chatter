# Identity Access Mail Credentials

`identity-access-next` sends verification and email-change mail over SMTP (Gmail by default) and reads the account credentials from Vault at startup.

## Store the credentials

Vault must be running and unsealed ([bootstrap](bootstrap.md)).

Set `IDENTITYACCESS_MAIL_USERNAME` and `IDENTITYACCESS_MAIL_PASSWORD` in `infrastructure/components/vault/.env`, then push them:

```bash
infrastructure/components/vault/scripts/configure-secrets.sh identityaccess
```

Or update just those two keys without touching the rest of `secret/identityaccess` (`VAULT_ADDR` and `VAULT_TOKEN` as in [bootstrap](bootstrap.md)):

```bash
vault kv patch secret/identityaccess \
  mail.username="<gmail address>" \
  mail.password="<gmail app password>"
```

For Gmail the password must be an app password, not the account password.

## Run the service

The service reads `secret/identityaccess` through Spring Cloud Vault, so it needs a token:

```bash
export VAULT_TOKEN=<token that can read secret/identityaccess>   # local dev: the value in .vault-init, or run `vault login`
export VAULT_HOST=localhost VAULT_PORT=5002                       # defaults
```

| Setting | Default | Purpose |
|---------|---------|---------|
| `IDENTITY_ACCESS_MAIL_HOST` / `_PORT` | `smtp.gmail.com` / `465` | SMTP server |
| `IDENTITY_ACCESS_MAIL_SSL` | `true` | Implicit TLS on port 465 |
| `IDENTITY_ACCESS_MAIL_FROM` | the mail username | Sender address |

The host in the mailed link is `identity-access.verification.mail.public-base-url` (default `http://localhost:5001`).

With no token at all, startup fails with "requires either a Token ... or a token file at ~/.vault-token".

## JWT signing keys

The service also reads its RSA key pair from Vault at startup, from `secret/common/jwt/rs256` (fields `private-key` and `public-key`). That is the pair `configure-secrets.sh jwt` writes and `profile` and the gateway already read, so the same `VAULT_TOKEN` must be allowed to read it too:

```bash
infrastructure/components/vault/scripts/configure-secrets.sh jwt   # only if the pair is not in Vault yet
```

Unlike the mail credentials, the keys are required. The service refuses to start, and says why, if Vault is unreachable, the token is rejected, the secret or a field is missing, a key is not valid, or the two keys do not match. The keys are read once, so a later Vault outage does not affect a running service; replacing the pair takes effect on the next restart.

There is no local setting for the keys. `IDENTITY_ACCESS_JWT_PRIVATE_KEY` and `IDENTITY_ACCESS_JWT_PUBLIC_KEY` are no longer read and can be removed from any `.env`. For local development run the Vault stack and seed the pair as above.

## Local mail catcher

Mail can still go to a catcher instead of Gmail; Vault stays required for the keys.

```bash
IDENTITY_ACCESS_MAIL_HOST=localhost IDENTITY_ACCESS_MAIL_PORT=1025 \
IDENTITY_ACCESS_MAIL_SSL=false IDENTITY_ACCESS_MAIL_AUTH=false
```

Start Mailpit with `docker start mailpit-chatter` and read mail at its web UI.

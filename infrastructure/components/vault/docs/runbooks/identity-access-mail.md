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
| `IDENTITY_ACCESS_VAULT_ENABLED` | `true` | Set `false` to run without Vault |
| `IDENTITY_ACCESS_MAIL_HOST` / `_PORT` | `smtp.gmail.com` / `465` | SMTP server |
| `IDENTITY_ACCESS_MAIL_SSL` | `true` | Implicit TLS on port 465 |
| `IDENTITY_ACCESS_MAIL_FROM` | the mail username | Sender address |

The host in the mailed link is `identity-access.verification.mail.public-base-url` (default `http://localhost:5001`).

With a token but an unreachable Vault the service logs a warning and starts with empty mail credentials, so sending fails until Vault is back. With no token at all, startup fails with "requires either a Token ... or a token file at ~/.vault-token".

## Run without Vault (local mail catcher)

```bash
IDENTITY_ACCESS_VAULT_ENABLED=false IDENTITY_ACCESS_MAIL_HOST=localhost IDENTITY_ACCESS_MAIL_PORT=1025 \
IDENTITY_ACCESS_MAIL_SSL=false IDENTITY_ACCESS_MAIL_AUTH=false
```

Start Mailpit with `docker start mailpit-chatter` and read mail at its web UI.

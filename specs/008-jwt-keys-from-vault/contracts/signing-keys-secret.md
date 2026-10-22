# Contract: Signing Keys in Vault

Between the operator (and `configure-secrets.sh`) and the identity service. Same pair, same location the other services already read.

## Secret layout

```text
KV v2 mount:  secret
path:         common/jwt/rs256
fields:       private-key   PKCS#8 RSA private key, PEM
              public-key    X.509 RSA public key, PEM
```

Equivalent CLI check (`VAULT_ADDR` and `VAULT_TOKEN` as in the Vault bootstrap runbook):

```bash
vault kv get -mount=secret -field=public-key common/jwt/rs256 | head -1   # -----BEGIN PUBLIC KEY-----
```

## What the service needs from its environment

| Setting | Purpose | Default |
|---|---|---|
| `VAULT_HOST`, `VAULT_PORT`, `VAULT_SCHEME` | where Vault is | `localhost`, `5002`, `http` |
| `VAULT_TOKEN` | the credential; must be allowed to read `secret/data/common/jwt/rs256` | none |

It no longer reads `IDENTITY_ACCESS_JWT_PRIVATE_KEY`, `IDENTITY_ACCESS_JWT_PUBLIC_KEY`, or `IDENTITY_ACCESS_VAULT_ENABLED`. Setting them has no effect (spec clarification 2026-10-21).

Optional location overrides (tests, or a future path split): `identity-access.security.jwt.keys.backend` and `identity-access.security.jwt.keys.path`.

## Start-up outcomes

The keys are read once while the application context is being built. Success lets the service continue to start. Every failure stops start-up with the message below in the failure report, and no other part of the service runs.

| Reason | When | Message begins |
|---|---|---|
| `VAULT_DISABLED` | Vault support switched off | `Signing keys can only be loaded from Vault, but Vault support is disabled` |
| `STORE_UNREACHABLE` | no answer from Vault | `Signing keys could not be loaded: Vault is not reachable at <scheme>://<host>:<port>` |
| `CREDENTIAL_REJECTED` | Vault returns 403, or no token is configured (a missing token is normally reported earlier by Spring Cloud Vault itself: "requires either a Token (spring.cloud.vault.token) ...") | `Signing keys could not be loaded: Vault rejected the service credential for <mount>/<path>` |
| `SECRET_NOT_FOUND` | nothing at the path | `Signing keys could not be loaded: no secret at <mount>/<path>` |
| `FIELD_MISSING` | field absent or blank | `Signing keys could not be loaded: <mount>/<path> has no <field>` |
| `KEY_INVALID` | not a valid PEM / RSA key | `Signing keys could not be loaded: <field> is not a valid <PKCS#8 RSA private key \| X.509 RSA public key> (<mount>/<path>)` |
| `KEY_PAIR_MISMATCH` | halves do not match | `Signing keys could not be loaded: private-key and public-key do not form a matching key pair (<mount>/<path>)` |

Rules for every message:

- Contains only mount, path, field name, endpoint and the reason. No key text, no fragment, no digest.
- The underlying parser or HTTP exception is **not** chained for `KEY_INVALID` (its message can quote input). It may be chained for `STORE_UNREACHABLE` and `CREDENTIAL_REJECTED`, whose messages hold no secret.
- After the cause is corrected, starting again needs no other change (spec User Story 2, scenario 5).

## After start-up

- A later Vault outage does not affect the service: the pair is held in memory (FR-009).
- Replacing the pair in Vault takes effect on the next restart. Tokens signed with the old private key then fail signature validation, which is the expected effect of rotation.

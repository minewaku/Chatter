# Quickstart: Validating JWT Keys from Vault

Prerequisites: Docker running; the Vault stack from `infrastructure/components/vault` (see its `docs/runbooks/bootstrap.md`); Java 21. Build commands use the per-service wrapper, never bare `mvn`, from `services/identity-access-next`.

## 1. Unit and slice tests

```bash
cd services/identity-access-next
./mvnw -pl identityaccess-infrastructure -am verify
```

Expect: `VaultSigningKeyLoaderTest` (needs Docker for a Vault container) passes for success and all seven reasons; `JwtDecoderTest` accepts a token with another audience and with none, and still rejects expired, foreign-signature and wrong-issuer tokens; `PemKeysTest` has no `file:` case; Spotless check passes.

## 2. End-to-end suite

```bash
./mvnw -pl identityaccess-start -am test
```

Expect: all existing e2e classes pass against PostgreSQL, Redis and a Vault container seeded with the test key pair; `AudienceE2eTest` passes; `SecretLeakageE2eTest` finds no key text in any response or log capture. (`verify` on the full reactor fails on a pre-existing `check` goal of `identityaccess-start`; use `test`.)

## 3. Manual run against the real Vault

1. Make sure the pair is in Vault:

   ```bash
   export VAULT_ADDR=http://127.0.0.1:5002 VAULT_TOKEN=<root or dev token from .vault-init>
   vault kv get -mount=secret -field=public-key common/jwt/rs256 | head -1
   # not found? from the repo root: infrastructure/components/vault/scripts/configure-secrets.sh jwt
   ```

2. Start the service with **no** JWT variables set:

   ```bash
   env -u IDENTITY_ACCESS_JWT_PRIVATE_KEY -u IDENTITY_ACCESS_JWT_PUBLIC_KEY \
     VAULT_TOKEN="$VAULT_TOKEN" java -jar identityaccess-start/target/identityaccess-start-*.jar
   ```

   Expect: it starts. Sign in with a verified account and call a protected endpoint with the token. Both succeed (spec SC-001).

3. Leftover values are ignored: start again with `IDENTITY_ACCESS_JWT_PRIVATE_KEY` and `IDENTITY_ACCESS_JWT_PUBLIC_KEY` set to a different, valid pair. Expect: tokens still verify against the **Vault** pair, for example by the `profile` service (spec User Story 1, scenario 4; SC-007).

## 4. Failure cases

Each should end with a refused start and the matching message from [contracts/signing-keys-secret.md](contracts/signing-keys-secret.md):

| Do this | Expect |
|---|---|
| stop Vault (`docker compose stop` in its component) | `STORE_UNREACHABLE` |
| start with `VAULT_TOKEN=wrong` | `CREDENTIAL_REJECTED` |
| start with `VAULT_TOKEN` unset | `CREDENTIAL_REJECTED` (no credential configured) |
| `vault kv delete -mount=secret common/jwt/rs256` | `SECRET_NOT_FOUND` |
| `vault kv patch -mount=secret common/jwt/rs256 public-key=` | `FIELD_MISSING` for `public-key` |
| `vault kv patch -mount=secret common/jwt/rs256 private-key=garbage` | `KEY_INVALID`, and `garbage` is not in the message |
| put another valid public key with the original private key | `KEY_PAIR_MISMATCH` |

Restore the pair with `configure-secrets.sh jwt` afterwards and confirm the service starts with no other change.

## 5. Audience

With the service running, build tokens signed by the Vault private key with `aud` set to `another-app` and with no `aud`; both are accepted on a protected endpoint. A token that is expired, signed with another key, or issued by another issuer still returns 401. A freshly issued token still carries `"aud": ["chatter"]`.

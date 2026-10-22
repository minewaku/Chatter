# Data Model: JWT Keys from Vault and Audience-Free Token Validation

No database or Redis structure changes. The "data" of this feature is one secret in Vault and the configuration record that locates it.

## Signing key pair (Vault secret)

Stored once, read by the identity service at start-up, and also read by `profile` and the gateway (public key).

| Location | Value |
|---|---|
| Mount | `secret` (KV version 2) |
| Path | `common/jwt/rs256` |
| Field `private-key` | PEM, `-----BEGIN PRIVATE KEY-----`, PKCS#8, RSA |
| Field `public-key` | PEM, `-----BEGIN PUBLIC KEY-----`, X.509 SubjectPublicKeyInfo, RSA |

Written by `infrastructure/components/vault/scripts/configure-secrets.sh jwt` from `assets/keys/private-key.pem` and `public-key.pem`. Not written by this feature.

Validation rules, applied in this order by the loader:

1. The secret exists.
2. Both fields exist and are not blank.
3. Each parses: surrounding begin/end lines and whitespace are tolerated (FR-008); the body is Base64 of a PKCS#8 RSA private key or an X.509 RSA public key.
4. The private key is an RSA CRT key whose modulus and public exponent equal the public key's.

## `RsaKeyPair` (in memory)

`PemKeys.RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey)`, unchanged. Created once by `SigningKeysConfiguration`; consumed by `JwtAccessTokenIssuer` (both keys) and `JwtDecoderConfiguration` (public key). Never logged, never serialized, no `toString` that prints key text (records print components, so the bean is not logged anywhere and the failure messages never include it).

## `JwtProperties` (configuration record)

Prefix `identity-access.security.jwt`.

| Field | Before | After |
|---|---|---|
| `privateKey` | required PEM or `file:`/`classpath:` | **removed** |
| `publicKey` | required PEM or `file:`/`classpath:` | **removed** |
| `issuer` | default `identity-access` | unchanged |
| `audience` | default `chatter`; issued **and** required on incoming tokens | default `chatter`; **issued only** |
| `accessTokenTtl` | default `PT15M`, must be positive | unchanged |
| `keys.backend` | n/a | new, default `secret` |
| `keys.path` | n/a | new, default `common/jwt/rs256` |

Constructor sites that pass the removed fields (`new JwtProperties(priv, pub, issuer, audience, ttl)`) change to the three-argument-plus-keys shape. The affected tests are listed in [plan.md](plan.md).

## `SigningKeysUnavailableException`

Unchecked, extends `IllegalStateException` so a failure inside a `@Bean` method surfaces as an ordinary context start-up failure.

| Field | Meaning |
|---|---|
| `reason` | one of `VAULT_DISABLED`, `STORE_UNREACHABLE`, `CREDENTIAL_REJECTED`, `SECRET_NOT_FOUND`, `FIELD_MISSING`, `KEY_INVALID`, `KEY_PAIR_MISMATCH` |
| message | mount, path, field name when relevant, and the reason; never key text or a fragment of it |

State transitions: none. The pair is loaded once per process; there is no reload.

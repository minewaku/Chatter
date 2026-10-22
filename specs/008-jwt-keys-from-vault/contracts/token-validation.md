# Contract: Incoming Access Token Validation

Applies to every request with a bearer token to `identity-access-next`. The only behavior change from before this feature is the audience row.

| Token | Before | After |
|---|---|---|
| Valid signature, issuer `identity-access`, not expired, live session, audience `chatter` | accepted | accepted |
| Same, audience names something else (for example `another-app`) | **rejected, 401** | **accepted** |
| Same, no audience claim at all | **rejected, 401** | **accepted** |
| Invalid or foreign signature | rejected, 401 | rejected, 401 |
| Issuer other than `identity-access` | rejected, 401 | rejected, 401 |
| Expired (`exp` in the past, beyond the default clock skew) | rejected, 401 | rejected, 401 |
| Not yet valid (`nbf` in the future, beyond skew) | rejected, 401 | rejected, 401 |
| No `sid` claim, or session revoked | rejected, 401 | rejected, 401 |
| Session store unreadable | rejected, 503 | rejected, 503 |

## Where each check happens

| Check | Place | Changed |
|---|---|---|
| Signature (RS256, public key from the loaded pair) | `NimbusJwtDecoder` | key now comes from the `RsaKeyPair` bean |
| `exp`, `nbf`, `iss` | `JwtValidators.createDefaultWithIssuer(issuer)` | no |
| Audience | custom validator in `JwtDecoderConfiguration` | **deleted** |
| `sid` present and not revoked | `RevokedAccessTokenFilter` | no |

## Issued tokens

Unchanged: header `RS256`; claims `iss` = `identity-access`, `aud` = `["chatter"]` (configured `audience`), `sub`, `email`, `sid`, `iat`, `exp`. FR-012.

## Configuration

`identity-access.security.jwt.audience` is still read, with default `chatter`, but only by the issuer. Validating incoming tokens needs no audience value (FR-013).

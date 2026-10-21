# Data Model: Refresh Credential Reuse Detection

The domain model does not change. `AuthenticationSession` still holds no credential and gains no field; "revoked because of reuse" is an ordinary revocation (`revokedAt` set) whose reason lives only in the log record. The change is one new key namespace in Redis and one edited script.

## Redis keys (prefix `identity-access:`)

| Key | Type | Value | Expires | Status |
|---|---|---|---|---|
| `session:<sessionId>` | hash | session fields and `tokenDigest` | `expiresAt + retention` | unchanged |
| `session:token:<digest>` | string | `sessionId` | `expiresAt + retention` | unchanged; the **current** credential only |
| `session:user:<userId>` | sorted set | session ids | longest session | unchanged |
| `access-revoked:<userId>:<sessionId>` | string | revocation time | access-token lifetime | unchanged; also written on reuse |
| `session:replaced:<digest>` | string | `sessionId` | replacement time + `retention` | **new**: a credential that a renewal has replaced |

`<digest>` is the SHA-256 hex of the raw credential, as for `session:token:`. No raw credential is stored. `<sessionId>` is a generated identifier, so `replaced:` and `token:` cannot collide with a session hash key.

## State of one credential

```text
issued ──(sign-in or renewal)──► current ──(renewal replaces it)──► replaced ──(retention elapses)──► forgotten
                                    │                                  │
                                    │                                  └─ presented for renewal ─► refused, session revoked*
                                    └─ presented for renewal ─► renews
```

\* only if the session is still unrevoked and unexpired; otherwise refused with no change.

A credential is in exactly one of: `session:token:` (current), `session:replaced:` (replaced), or neither (unknown or forgotten). The rotation script moves it from the first to the second in one step.

## Lifetimes and bounds

- A replaced pointer expires `retention` after the replacement (`PEXPIREAT` from the application clock). The session hash expires at `newExpiry + retention`, which is later, so the pointer never outlives the session's records (spec FR-012).
- Renewals keep adding pointers; nothing removes them early. Volume is described in [research.md](research.md) decision 4.
- When the session record is gone, its replaced pointers resolve to a missing session; the script returns nothing and changes nothing.

## Validation rules (from the spec)

| Rule | Where it is enforced |
|---|---|
| Replaced credential never accepted for renewal (FR-001, FR-014 of the spec) | it is not in `session:token:`, so `findByPresentedToken` finds nothing |
| Revoke only the session the pointer names (FR-004) | the script derives the session from the pointer value alone |
| No change to a revoked or expired session, no event (FR-008) | `session-revoke-reused.lua` returns nothing in both cases |
| Blank or unknown credential never revokes anything (FR-007) | the adapter returns false for blank input; an unknown digest has no replaced pointer |
| Reuse and revocation are one step (FR-010) | single script call |

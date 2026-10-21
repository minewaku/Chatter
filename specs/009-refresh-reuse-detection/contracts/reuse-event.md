# Contract: Reuse Event (operator-visible record)

One record per session newly revoked because a replaced refresh credential was presented for renewal (spec FR-011).

## Form

A structured application log line at `WARN`, written by `RedisAuthenticationSessionRepository` from the result of `session-revoke-reused.lua`:

```text
Refresh reuse detected: session revoked reason=refresh-reuse sessionId=<sessionId> userId=<userId>
```

| Field | Value |
|---|---|
| level | `WARN` |
| `reason` | the constant `refresh-reuse`, so operators can tell it from ordinary sign-outs |
| `sessionId` | the revoked session's identifier |
| `userId` | the owning account's identifier |

The wording avoids the words the repository's `LoggingRuleTest` forbids in infrastructure log calls (`token`, `credential`, `secret`, and so on), whose allow-list stays empty. The record names no value, only the reason and two identifiers.

## Rules

- It is written only when the script actually revokes a session. A reused credential whose session was already revoked, expired, or forgotten writes nothing (spec FR-008).
- Ordinary sign-outs, account-change revocations, and unknown credentials never write it.
- It never contains a refresh credential, its digest, an access credential, an email address, or the device label.
- One line per revocation: a second presentation of the same old credential finds the session already revoked and writes nothing more.

## How it is tested

- Redis adapter test: revoking through a replaced pointer logs exactly one line with the fields above; the three no-change cases log nothing (captured with the test's log appender).
- End-to-end: `SecretLeakageE2eTest` adds a reuse step and asserts that no raw credential, digest, or access token appears in captured log output.

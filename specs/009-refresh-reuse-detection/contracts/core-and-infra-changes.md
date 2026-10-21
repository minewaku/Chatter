# Contract: Core and Infrastructure Changes

No HTTP contract changes: `POST` renewal still answers with the same success body and the same refusal for any unusable credential, and sign-out is unchanged. This file lists the internal interfaces that change.

## 1. Port: `AuthenticationSessionRepository` (core)

New method, added after `rotate`:

```java
/**
 * Called when a presented refresh credential matched no current session. If the credential was replaced by an earlier
 * renewal and its session is still unrevoked and unexpired at {@code now}, revokes that session and returns true;
 * otherwise changes nothing and returns false. A blank credential returns false. A session newly revoked here must
 * also make the access credentials issued for it unusable for the access credential lifetime, and the adapter
 * records the event (adapter obligation, same as revokeBySessionId).
 */
boolean revokeOnReuse(String presentedRefreshToken, Instant now);
```

All other methods keep their signatures and meaning. `findByPresentedToken` still resolves **current** credentials only.

## 2. Service: `RefreshAccessTokenService` (core)

```text
handle(command):
  blank credential                                   -> throw AuthenticationFailedException          (unchanged)
  found = sessions.findByPresentedToken(token)
  if found is empty:
      sessions.revokeOnReuse(token, now)                                                              (new)
      throw AuthenticationFailedException                                                             (same refusal)
  if session not usable or owner not accessible      -> throw AuthenticationFailedException          (unchanged, no reuse check)
  issue access credential; generate new refresh credential
  rotated = sessions.rotate(...)
  if not rotated                                     -> throw AuthenticationFailedException          (unchanged: concurrent loser, session kept)
  return Refreshed(...)
```

`LogoutCurrentSessionService`, `LogoutSessionService`, `LogoutOtherSessionsService` and the account-change services are not changed. A failure of `revokeOnReuse` (store unavailable) propagates like any other store failure and no credential is issued.

## 3. Adapter: `RedisAuthenticationSessionRepository` (infrastructure)

- `rotate(...)`: also passes the key `session:replaced:<oldDigest>` and its expiry in epoch millis (`toMillisFloor(micros(now)) + retentionMillis`) to the script.
- `revokeOnReuse(token, now)`: blank returns false; otherwise runs `session-revoke-reused.lua` with the key `session:replaced:<digest>`; a non-null result `"<userId>:<sessionId>"` returns true and logs the event per [reuse-event.md](reuse-event.md); a null result returns false.

## 4. Script: `session-rotate.lua` (edited)

Existing contract is unchanged, and the call keeps its three keys and six arguments in order. Two additions:

```text
KEYS[4]   replaced pointer key for the presented credential: <prefix>session:replaced:<oldDigest>
ARGV[7]   replaced pointer expiry (epoch millis)
```

After the existing checks pass, in place of `DEL KEYS[2]` alone:

```lua
redis.call('SET', KEYS[4], ARGV[1])
redis.call('PEXPIREAT', KEYS[4], tonumber(ARGV[7]))
redis.call('DEL', KEYS[2])
```

On every failure path (`return 0`) nothing is written, as today.

## 5. Script: `session-revoke-reused.lua` (new)

```text
KEYS[1]   replaced pointer key
ARGV[1]   session hash key prefix (<prefix>session:)
ARGV[2]   now (epoch micros)
ARGV[3]   revocation entry key prefix (ends with "access-revoked:")
ARGV[4]   access credential lifetime (millis)
returns   "<userId>:<sessionId>" when it revoked a session, otherwise nil
```

Steps, all in one script:

1. `GET KEYS[1]`; nil returns nil.
2. Session key is `ARGV[1] .. sessionId`; missing returns nil.
3. `HEXISTS revokedAt` is 1 returns nil (already revoked: no change, no event).
4. `now >= expiresAt` returns nil (expired: no change, no event).
5. `HSET revokedAt ARGV[2]`.
6. `SET ARGV[3] .. userId .. ':' .. sessionId  ARGV[2]  PX ARGV[4]` (same entry as `session-revoke-one.lua`).
7. Return `userId .. ':' .. sessionId`.

The replaced pointer is left in place; it expires on its own, and a second presentation finds the session already revoked (step 3).

## 6. Test doubles (core)

`AuthenticationTestFakes` session repository: `rotate` records `presentedRefreshToken -> sessionId` in a `replacedByToken` map; `revokeOnReuse` looks the credential up there and revokes a usable session (`session.revoke(now)`), returning whether it did; a counter `revokeOnReuseCalls` lets tests assert it is not called on the happy path.

## 7. Documentation

- `specs/002-authentication-sessions/spec.md` FR-015: add "(replay detection added by spec 009)".
- `specs/004-presentation-api/spec.md` FR-029 and the closing note on spec 003 FR-006: add "(superseded by spec 009)".

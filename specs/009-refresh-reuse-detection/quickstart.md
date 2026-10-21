# Quickstart: Validating Refresh Credential Reuse Detection

Prerequisites: Docker running (the tests start `postgres:17` and `redis:7` containers); Java 21. Build commands use the per-service wrapper, never bare `mvn`, from `services/identity-access-next`.

## 1. Core and adapter tests

```bash
cd services/identity-access-next
./mvnw -pl identityaccess-core,identityaccess-infrastructure -am verify
```

Expect:

- `RefreshAccessTokenServiceTest`: a replaced credential presented for renewal is refused and calls `revokeOnReuse`; the same refusal type as for an unknown credential; no reuse call on the happy path or for a revoked/expired session; a concurrent loser (rotate returns false) does not revoke; a failing `revokeOnReuse` propagates and issues nothing.
- `RedisAuthenticationSessionRepositoryTest`: after `rotate`, `session:replaced:<digest>` exists with the session id and a lifetime of `retention` from the replacement; the old digest is gone from `session:token:`; `revokeOnReuse` on the old credential revokes the session, writes the `access-revoked` entry, and logs one `refresh-reuse` line; it returns false and writes nothing for a current, unknown, blank, already-revoked, or expired case; many concurrent rotations with one credential still produce one winner and leave the session unrevoked.
- Spotless check passes.

## 2. End-to-end suite

```bash
./mvnw -pl identityaccess-start -am test
```

(`verify` on the full reactor fails on a pre-existing `check` goal of `identityaccess-start`; use `test`.)

Expect `SessionLifecycleE2eTest` to cover, over HTTP:

1. Sign in, renew once, present the first refresh credential again: refused, same body as for an unknown credential.
2. The credential issued by that renewal no longer renews access.
3. An access credential from that session is rejected on its next use, with no wait.
4. A second session of the same account keeps working.
5. Several renewals in a row, then the oldest credential: session revoked.
6. Sign-out with a replaced credential answers `revoked = false` and the session keeps working.
7. 50 renewals in order, always with the newest credential: none refused.

`SecretLeakageE2eTest` finds no credential, digest, or access token in the captured logs after a reuse.

## 3. Manual check against a running service

1. Start the shared stack and the service as described in the project README, then sign in and keep both tokens (`R1` is the refresh credential).
2. Renew with `R1` and keep the new refresh credential `R2`.
3. Renew again with `R1`. Expect the usual refusal, and in the service log one line: `Refresh reuse detected: session revoked reason=refresh-reuse sessionId=... userId=...`.
4. Renew with `R2`. Expect a refusal (the session is revoked).
5. Inspect Redis for the replaced pointer and its lifetime (digest only, never the raw credential):

   ```bash
   redis-cli --scan --pattern 'identity-access:session:replaced:*' | head -1 | xargs -I{} redis-cli pttl {}
   ```

   Expect a positive TTL of about `retention` (30 days by default) and a value equal to the session id.

## 4. Acceptance map

| Spec item | Where it is checked |
|---|---|
| SC-001 reuse refused and session revoked | e2e scenarios 1 and 5; adapter test |
| SC-002 access credentials rejected at once | e2e scenario 3 |
| SC-003 other sessions unaffected | e2e scenario 4 |
| SC-004 50 ordered renewals | e2e scenario 7 |
| SC-005 identical refusal | e2e scenario 1 (body and status compared with an unknown credential) |
| SC-006 one record per revocation, no credential value | adapter log capture; leakage e2e |
| SC-007 memory gone after retention | adapter test with a short retention (existing short-lived factory) |
| FR-013 simultaneous renewals | adapter concurrency test; service test (loser) |
| Sign-out unchanged | e2e scenario 6 |

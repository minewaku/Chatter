# Identity Access Next Infrastructure Migration

## Contract

- Migrate infrastructure from `services/identity-access` into `services/identity-access-next`.
- Do not migrate `messaging`, `cache`, `persistence`, `cdc`, `exception`, or `scheduler` packages.
- Use only third-party dependencies already declared by the legacy identity-access parent or infrastructure POM.
- Keep the Clean Architecture dependency direction `infrastructure -> core`.
- Preserve the user's existing core and presentation changes.

## Implementation checklist

- [ ] Replace the non-legacy Bouncy Castle dependency with legacy Password4j.
- [ ] Add only legacy dependencies required by migrated, non-excluded infrastructure.
- [ ] Migrate the user password hasher to the new `PasswordHasher` port.
- [ ] Migrate the Snowflake generator to the new `UserIdGenerator` port without depending on excluded persistence code.
- [ ] Migrate Spring Data JDBC repository scanning and value-object conversions.
- [ ] Migrate Flyway configuration.
- [ ] Migrate RSA/JWT verification and stateless HTTP security needed by the current authenticated user endpoint.
- [ ] Migrate Vault-backed RSA key and datasource-credential rotation configuration without logging secrets.
- [ ] Migrate request logging without logging request or response bodies.
- [ ] Update application configuration for Snowflake and security infrastructure.
- [ ] Verify Maven dependency resolution, formatting, compilation, and tests.
- [ ] Record deferred legacy adapters and the missing core contracts that block them.

## Deferred legacy adapters

These adapters are outside the six explicitly excluded packages, but cannot correctly implement Clean Architecture ports because the corresponding new-core features do not exist yet:

- `AesGcmRefreshTokenEncryptor`: needs the future session model and refresh-token port.
- `Rs256JwtTokenProvider`: needs the future auth access-token generation port.
- `TemplatedEmailService` and `verifyEmailTemplate.html`: need the future verification notification port and mail type.
- `ConfirmTokenUrlGeneratorIml`: needs the future verification URL port.
- `UuidV7UniqueStringGenerator`: has no current core consumer or port.
- The legacy `AppConfig` beans that exist only for those deferred adapters.
- `VaultRefreshProperties`: used only by the deferred refresh-token adapter.

## Compatibility and security notes

- The project targets Java 21 and Spring Boot 4.0.8; Spring-managed dependency versions are preferred where available.
- Password4j remains pinned to the legacy version until compilation/tests prove whether an upgrade is necessary.
- JWT libraries remain pinned to the legacy version.
- Legacy code that logs passwords, JWT key material, refresh secrets, or complete HTTP bodies must not be copied unchanged.
- The legacy Snowflake generator's database-backed datacenter allocation is not migrated because it depends on the excluded persistence package; datacenter and worker IDs become explicit configuration.

## Rollback

All work is isolated to new files and configuration under `identity-access-next` plus this plan. Reverting those paths restores the pre-migration state without changing the legacy service.

# Domain implementation order

Implement the replacement in this order:

1. **Project/bootstrap foundation**: new Maven modules, Spring Data JDBC, PostgreSQL/Flyway, and composition root.
2. **User Account foundation**: first implementation domain.
3. **Account Verification**: verification token and email delivery; transition `PENDING_VERIFICATION -> ACTIVE`.
4. **Authentication and Session Lifecycle**: login, JWT issuance, refresh rotation, logout, and revocation.
5. **Self-service account maintenance**: password, email, and username changes with their session-revocation effects.
6. **External integration/outbox**: only for confirmed cross-service contracts.

## Dependency rationale

User Account owns identity, credentials, and accessibility. Verification, authentication, and sessions depend on it; self-service maintenance changes account-owned data. Implementing those first would duplicate its identity or state decisions.

## Logical feature modules, not Maven artifacts

The items above are logical features. All first-phase feature code belongs in the single `identityaccess-core` artifact, organized by feature package and intentional public API. Do not create a Maven artifact per feature. Infrastructure, presentation, and start remain technical/composition modules.

The User Account phase does not add verification, auth, session, profile, event, or outbox code simply because those domains come later.

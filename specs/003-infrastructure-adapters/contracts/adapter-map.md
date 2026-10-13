# Contract: Core Port → Adapter Map

**Feature**: [spec.md](../spec.md)

Convention (FR-015): storage adapters live at `adapter/outbound/<capability>/<write|read>[/cache]/<technology>`; non-storage adapters sit directly under their capability. Base package `com.minewaku.chatter.identityaccess.infrastructure`. Exactly one implementation per port (FR-001); the structure test reads this table.

| Core port (package under `com.minewaku.chatter.identityaccess`) | Adapter class | Package under `…infrastructure.adapter.outbound` | Store / library |
|---|---|---|---|
| `user.internal.port.PasswordHasher` | `Argon2PasswordHasher` | `user.password` | Password4j Argon2id |
| `user.internal.port.repository.UserAccountRepository` | `JdbcUserAccountRepository` | `user.write.jdbc` | PostgreSQL (Spring Data JDBC rows) |
| `user.internal.port.query.UserAccountQueryRepository` | `JdbcUserAccountQueryRepository` | `user.read.jdbc` | PostgreSQL (`JdbcClient`) |
| `shared.port.TimeBasedIdGenerator` | `SnowflakeIdGenerator` | `shared.id` | in-process |
| `shared.port.UniqueStringIdGenerator` | `UuidV7IdGenerator` | `shared.id` | java-uuid-generator |
| `authentication.internal.port.AccessTokenIssuer` | `JwtAccessTokenIssuer` | `authentication` | Nimbus (via Spring Security JOSE) |
| `authentication.internal.port.RefreshTokenGenerator` | `SecureRandomRefreshTokenGenerator` | `authentication` | `SecureRandom` |
| `authentication.internal.port.repository.AuthenticationSessionRepository` | `RedisAuthenticationSessionRepository` | `session.write.redis` | Redis + Lua |
| `authentication.internal.port.query.AuthenticationSessionViewRepository` | `RedisAuthenticationSessionViewRepository` | `session.read.redis` | Redis |
| `emailverification.internal.port.repository.EmailVerificationRepository` | `RedisEmailVerificationRepository` | `verification.write.redis` | Redis + Lua |
| `emailverification.internal.port.VerificationNotifier` | `MailVerificationNotifier` | `verification.mail` | JavaMail + Thymeleaf |

## Package tree (target)

```text
infrastructure/
├── adapter/outbound/
│   ├── shared/id/            SnowflakeIdGenerator, SnowflakeProperties, UuidV7IdGenerator
│   ├── user/
│   │   ├── password/         Argon2PasswordHasher
│   │   ├── write/jdbc/       JdbcUserAccountRepository, UserAccountRow, UserAccountRows, UserAccountRowMapper
│   │   └── read/jdbc/        JdbcUserAccountQueryRepository
│   ├── authentication/       JwtAccessTokenIssuer, SecureRandomRefreshTokenGenerator
│   ├── session/
│   │   ├── SessionProperties, SessionConfiguration   (Duration sessionLifetime bean)
│   │   ├── write/redis/      RedisAuthenticationSessionRepository, SessionRecord, SessionRecordMapper
│   │   └── read/redis/       RedisAuthenticationSessionViewRepository
│   └── verification/
│       ├── mail/             MailVerificationNotifier, VerificationLinkFactory, VerificationMailProperties
│       └── write/redis/      RedisEmailVerificationRepository, VerificationRecord, VerificationRecordMapper
├── security/                 SecurityConfiguration, JwtDecoderConfiguration, JwtProperties,
│                             SecurityProperties, PemKeys, TokenDigest
└── integration/
    ├── postgresql/           PostgreSqlConfiguration
    ├── redis/                RedisConfiguration, RedisKeyProperties
    └── mail/                 MailConfiguration (@EnableResilientMethods)

resources/
├── db/migration/V1__create_user_account.sql   (exists, unchanged)
├── redis/*.lua                                (session and verification scripts)
└── templates/email-verification.html
```

Rules enforced by the structure test:
1. No class under `identityaccess.(user|authentication|emailverification|shared)..` imports `…infrastructure..`, Spring Data/JDBC/Redis, Nimbus/JWT, Jakarta Mail, Servlet, or Spring Web/Security types.
2. Each port above has exactly one implementing class and it sits in the stated package.
3. No class named `*Row`, `*Record`, or `*Properties` is `public` outside `security`, `integration`, or the adapter `*Properties`/`*Configuration` classes (persistence shapes stay package-private).
4. No package named `service`, `helper`, `config`, or `persistence` exists under `infrastructure`.

## Behavioural contract

Adapters implement the javadoc of each port exactly (for example, `AuthenticationSessionRepository.touchIfUsable` sets last-use only when unrevoked and `now` is before expiry). Shared adapter contract tests encode those sentences and run against the real stores.

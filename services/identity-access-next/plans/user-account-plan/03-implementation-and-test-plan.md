# Implementation and test plan

## New module shape

```text
identityaccess-parent
|- identityaccess-core
|- identityaccess-infrastructure
|- identityaccess-presentation
\`- identityaccess-start
```

Dependencies are one-way: `presentation -> core`, `infrastructure -> core`, and `start -> presentation + infrastructure`.

Start core with this initial User Account layout. This structure is part of the handoff contract: new sessions must keep feature internals private and add future feature packages as siblings, not new Maven artifacts.

```text
com.minewaku.chatter.identityaccess
\`- user
   |- RegisterUserUseCase.java
   |- FindUserByIdUseCase.java
   |- FindUserByEmailUseCase.java
   |- EnableUserAccountUseCase.java
   |- DisableUserAccountUseCase.java
   |- LockUserAccountUseCase.java
   |- UnlockUserAccountUseCase.java
   |- SoftDeleteUserAccountUseCase.java
   |- ActivateAfterVerificationUseCase.java
   \`- internal
      |- application
      |  |- RegisterUserService.java
      |  |- FindUserAccountService.java
      |  \`- ManageUserAccountService.java
      |- model
      |  |- UserAccount.java
      |  |- UserId.java
      |  |- Email.java
      |  |- Username.java
      |  |- Birthday.java
      |  |- PlainPassword.java
      |  |- PasswordHash.java
      |  \`- AccountStatus.java
      \`- port
         |- UserAccountRepository.java
         |- PasswordHasher.java
         \`- UserIdGenerator.java
```

`identityaccess-infrastructure` supplies JDBC configuration/repository wiring, PostgreSQL/Flyway, Argon2 hashing, and the Snowflake generator. `identityaccess-presentation` supplies only registration and self-delete HTTP adapters. It uses request/response DTOs and never exposes core model classes as API DTOs. `identityaccess-start` is the composition root.

Test the existing routes `POST /api/v1/auth/register` and authenticated `DELETE /api/v1/users`; confirm their detailed request/response/error contract from legacy controllers and tests before implementation.

## Public core operations and boundary DTOs

Expose a separate use-case interface for register, find by ID, find by email, enable, disable, lock, unlock, soft-delete, and future verification activation. Keep repositories, Spring Data detail, and mutable models in `user.internal`. Core exposes password-hashing and `UserIdGenerator` ports; infrastructure implements them.

Define request and response DTOs as nested types on the use case that owns them. For example, `RegisterUserUseCase.Command` is a `record` containing registration inputs, and `FindUserByIdUseCase.Result` is a `record` containing only the data that lookup operation intentionally returns. Records are immutable, which is desirable for boundary DTOs. Do not create global `UserAccountView`, `RegisterUserCommand`, or generic result DTO classes.

Operation-specific DTO duplication is intentional: it prevents unrelated use cases from coupling to a shared, growing transport shape. Commands carry stable public scalar values or public identifier types as appropriate; they must not expose `user.internal.model.UserAccount` or another internal mutable model. For duplicate registration, use a named public outcome owned by `RegisterUserUseCase` (for example a nested `AccountAlreadyExists` result variant) rather than a globally reusable `AccountAlreadyExistsResult` type.

Registration and every state transition require a transaction. Translate database uniqueness violations during registration into an account-already-exists result; do not leak driver exceptions. Keep database constraints even if code pre-checks uniqueness.

Do not add domain events, Kafka/integration publishing, or an outbox in this phase.

## Test plan

1. **Unit**: all value-object validations and allowed/disallowed state transitions, including terminal deletion and anonymization.
2. **JDBC integration**: conversions, direct aggregate persistence, embedded `PasswordHash`, uniqueness, and optimistic locking.
3. **Application**: plain password is hashed through the port; valid registration persists pending/inaccessible; duplicate registration returns account-already-exists.
4. **HTTP contract**: registration and self-delete routes, DTO validation/error mapping, and no core model JSON.

## Acceptance criteria

- Valid registration persists an inaccessible `PENDING_VERIFICATION` account.
- Invalid values never persist.
- A deleted account contains no original email, username, or password hash; original unique values are reusable.
- Concurrent updates cannot silently overwrite each other.
- The first phase emits no domain-event, Kafka/integration event, or outbox record.

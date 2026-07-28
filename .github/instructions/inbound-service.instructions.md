---
applyTo: "services/**/*-application/port/inbound/**"
name: "Clean Architecture Inbound Port DTO Refactor"
description: "Refactor inbound ports to use application-specific nested DTOs and prevent domain model leakage."
---

You are a specialized agent for refactoring inbound ports in the `*-application` layer.

## Pre-Refactor Analysis

Analyze the target inbound port before making changes.

If the port already:
- uses only application-specific DTOs,
- does not expose domain models in `UseCaseHandler` generics,
- uses nested DTOs,
- and follows the rules below,

output:

> Service is already compliant with Clean Architecture

Then stop.

## Rules

- Apply these rules only to `*-application/port/inbound/**`.
- Do not modify outbound ports.
- Do not modify domain, infrastructure, or presentation layers.
- Every inbound port must expose only application DTOs.
- Never expose domain models in the input or output of `UseCaseHandler`.
- Define request and response DTOs as nested records inside the use case interface.
- Use `Void` directly when a request or response contains no data.
- Replace any external Application DTO used by a single inbound port with nested records.
- Remove obsolete standalone DTO classes after migration.
- Keep `UseCaseHandler<C, R>` unchanged.

## Examples

Expose nested DTOs when data is required:

```java
public interface ExampleUseCase extends UseCaseHandler<ExampleUseCase.Command, ExampleUseCase.Result> {

    record Command(
        String value,
        Long id
    ) {
    }

    record Result(
        Long id
    ) {
    }
}
```

Use `Void` when no request or response data is needed:

```java
public interface ExampleUseCase extends UseCaseHandler<Void, Void> {
}
```

## Validation Checklist

- No domain model appears in `UseCaseHandler` generic types.
- Request and response DTOs are nested records when they contain data.
- `Void` is used instead of an empty `Command` or `Result`.
- Standalone DTOs used only by a single inbound port are removed.
- Only inbound ports are refactored.
- Public API behavior remains unchanged.
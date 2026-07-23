# Copilot Cloud Agent Onboarding Instructions

## Repository shape
- This repository is a Java 21, Spring Boot microservices workspace under `/services`.
- There is **no root `pom.xml`** for all services; build/test per service.
- Core services:
  - `/home/runner/work/Chatter/Chatter/services/identity-access` (multi-module DDD)
  - `/home/runner/work/Chatter/Chatter/services/profile` (multi-module DDD)
  - `/home/runner/work/Chatter/Chatter/services/message` (multi-module DDD)
  - `/home/runner/work/Chatter/Chatter/services/api-gateway` (single module)
  - `/home/runner/work/Chatter/Chatter/services/service-registry` (single module)
- Infrastructure docs and compose files live under `/home/runner/work/Chatter/Chatter/infrastructure`.

## Architecture conventions
- DDD modules typically map as:
  - `*-domain`: business rules/entities/value objects
  - `*-application`: use-cases/services/orchestration
  - `*-infrastructure`: persistence/external systems
  - `*-presentation`: controllers/APIs/input-output adapters
  - `*-start`: Spring Boot entrypoint/runtime wiring
- Keep logic in the correct layer; avoid leaking infrastructure concerns into domain/application.

## Efficient workflow for changes
1. Scope to one service first, then to one module.
2. Read that service `pom.xml` and relevant module `pom.xml` before editing.
3. Make minimal edits in the affected module(s).
4. Validate only the affected service/module first, then expand if needed.

## Build/test commands (from repo root)
- Identity Access: `./mvnw -f services/identity-access/pom.xml test`
- Profile: `./mvnw -f services/profile/pom.xml test`
- Message: `./mvnw -f services/message/pom.xml test`
- API Gateway: `./mvnw -f services/api-gateway/pom.xml test`
- Service Registry: `./mvnw -f services/service-registry/pom.xml test`

If you need one module only, use `-pl <module> -am` with that service parent POM.

## Formatting/linting notes
- `services/api-gateway/pom.xml` configures `spotless-maven-plugin`.
- Other services may rely mostly on compiler/test checks; always inspect each service `pom.xml` before assuming shared tooling.

## Known errors encountered and workarounds
1. **Error:** `services/api-gateway/README.md` does not exist.
   - **Workaround:** use `services/api-gateway/pom.xml`, `services/api-gateway/src/main/**`, and `services/api-gateway/src/main/resources/application.properties` as the primary source of truth.
2. **Error:** Kafka topic scripts edited on Windows can fail in Linux containers with `command not found` / shell syntax errors due to CRLF.
   - **Workaround:** convert to LF before execution:
     - `sed -i 's/\r$//' ../../opt/kafka/scripts/create-topics.sh`
     - Then run the script normally (documented in `infrastructure/kafka/README.md`).

## Safety and secrets
- Never commit real secrets/tokens (Cloudflare token, Vault keys/tokens, DB credentials, JWT keys).
- Keep placeholder/masked values intact in docs and configs.

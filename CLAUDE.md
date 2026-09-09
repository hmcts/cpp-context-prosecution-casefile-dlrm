# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service is

`pcfdlrm` (Prosecution Casefile DLRM — Data Lake / Reporting / Migration) is one of the HMCTS CPP bounded contexts. It sits **alongside** the main `prosecutioncasefile` context: it ingests **migrated** prosecution case files from the legacy estate, validates and accepts them, layers in court‑document material, and emits a single public event (`public.pcfdlrm.migrated-case-file-processed`) that `progression` consumes to bring the migrated case into the live pipeline.

Built on the CPP framework (`uk.gov.moj.cpp.common:service-parent-pom`), packaged as a WAR, deployed to WildFly. Java 17.

## Build, test, run

```bash
# Full build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Build + unit tests
mvn clean install

# Single module (with deps)
mvn -pl pcfdlrm-domain/pcfdlrm-domain-aggregate -am clean install

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName
```

### Integration tests

The `pcfdlrm-integration-test` module is **not** run by `mvn verify`. It needs WildFly + Postgres + ActiveMQ in Docker first:

```bash
./runIntegrationTests.sh
```

Prerequisites:
- `CPP_DOCKER_DIR` env var pointing at a local checkout of `hmcts/cpp-developers-docker`.
- Docker daemon running and authenticated to the `crmdvrepo01` registry.

The script builds WARs → undeploys old → starts containers → runs Liquibase (event log, aggregate snapshot, viewstore, event buffer, system, event tracking) → deploys WireMock stubs → deploys WARs → healthchecks → runs ITs.

Once the env is up, run a single IT against it:

```bash
mvn -pl pcfdlrm-integration-test test -Dit.test=<IT class name>
```

### Framework JMX commands

```bash
./runSystemCommand.sh           # help
./runSystemCommand.sh --list    # list available commands (CATCHUP, etc.)
./runSystemCommand.sh CATCHUP   # run one
```

Uses `admin/admin` against local WildFly. Downloads `framework-jmx-command-client` on first use.

### CI

Azure DevOps (`azure-pipelines.yaml`):
- PR builds → `pipelines/context-verify.yaml` (Sonar + unit tests).
- `IndividualCI` on `main` / `team/*` → `pipelines/context-validation.yaml` with `serviceName=pcfdlrm` and `itTestFolder=pcfdlrm-integration-test`.
- `dev/release-*` branches are excluded.
- Agent pool: `MDV-ADO-AGENT-AKS-01`, demand `centos8-j17`.
- SonarQube project: `uk.gov.moj.cpp.prosecutioncasefile.dlrm:prosecutioncasefile-dlrm-parent`.

## Architecture — the three layers you must reason across

For a full source-cited trace of the end-to-end migration flow (stagingdlrm → pcfdlrm →
progression → listing), the validation rule set, material-upload mechanics, cross-context
dependencies, and known edge cases, see `docs/architecture/pcfdlrm-flow-reference.md`.

Every change touching events needs to be reasoned about across **three layers**. Breaking one without the others produces silent data drift:

1. **Command side** — RAML‑declared commands hit `@Handles`-annotated handler classes (`MigratedCaseFileHandler`, `AddMaterialHandler`) which load the `MigratedCaseFileAggregate`, run validation rules under `pcfdlrm-domain/pcfdlrm-domain-aggregate/.../validation/`, and apply domain events.
2. **Event listener** — `pcfdlrm-event/pcfdlrm-event-listener` is wired into the reactor but is currently a stub (no `subscriptions-descriptor.yaml`, no Java listeners). The viewstore Liquibase (`pcfdlrm-viewstore-liquibase`) is in place ready for read‑model wiring.
3. **Event processor** — consumes domain + public events and emits the single public event for downstream contexts. Lives under `pcfdlrm-event/pcfdlrm-event-processor`. Existing processors: `MigratedCaseFileProcessedProcessor`, `MigratedCaseReceivedProcessor`, `MaterialEventProcessor`, `MaterialReadyForCourtDocumentProcessor`, `ProgressionPublicEventProcessor`.

### Three subscription sources

The processor (and, eventually, the listener) can be triggered by:

1. **Internal event topic** `pcfdlrm.event` — replay of this context's own domain events (`migrated-case-file-received`, `migrated-case-file-processed`, `material-added`, `material-ready-for-court-document`).
2. **Public event bus** `public.event` — events from other contexts. Today consumes `public.progression.prosecution-case-created` and `material.material-added`.
3. **Command queue** `pcfdlrm.handler.command` — RAML‑declared commands.

### Authoritative routing files (always re-read before reasoning about a flow)

- `pcfdlrm-event-sources/src/yaml/event-sources.yaml` — internal + public topic declarations.
- `pcfdlrm-event/pcfdlrm-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `pcfdlrm-event/pcfdlrm-event-processor/src/yaml/public-publications-descriptor.yaml` — public events emitted (today: `public.pcfdlrm.migrated-case-file-processed`).
- `pcfdlrm-event/pcfdlrm-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions (currently absent; create when introducing the read‑model).
- `pcfdlrm-command/pcfdlrm-command-handler/src/raml/pcfdlrm-command-handler.messaging.raml` — command → handler mapping.
- `pcfdlrm-service/src/main/descriptors/resource-descriptor.yml` — JMS queues, topics, datasources, service URI mapping (`/pcfdlrm-[^/]+`).
- Per-command/per-event JSON schemas: `src/raml/json/` and `src/raml/json/schema/` under each command/domain-event module.

### Intake surface (today)

| Source | Mechanism | Handler / Processor |
|---|---|---|
| Migration command — receive | `pcfdlrm.command.receive-migrated-case-file` | `MigratedCaseFileHandler` |
| Migration command — accept  | `pcfdlrm.command.accept-migrated-case`       | `MigratedCaseFileHandler` |
| Material command            | `pcfdlrm.command.add-case-court-document`    | `AddMaterialHandler` |
| Internal replay             | `pcfdlrm.event` topic                        | processors above |
| Public event — progression  | `public.progression.prosecution-case-created`| `ProgressionPublicEventProcessor` |
| Public event — material     | `material.material-added`                    | `MaterialEventProcessor` |

### Data stores

- `java:/app/pcfdlrm-service/DS.eventstore` — event store (event‑repository‑liquibase + aggregate‑snapshot‑repository‑liquibase).
- `java:/DS.pcfdlrm` — viewstore (event‑buffer‑liquibase + `pcfdlrm-viewstore-liquibase`).

## Critical gotcha — when adding/removing an event

**Always update both** the relevant `subscriptions-descriptor.yaml` **and** the JSON schema tree under `*/src/main/resources/json/schema/` (or `*/src/raml/json/schema/`). A subscription without a matching schema produces a runtime 500 on dispatch.

For public events: also update `public-publications-descriptor.yaml`. The processor‑side descriptor for the `progression` consumer is in this repo for reference at `pcfdlrm-event/pcfdlrm-event-processor/src/yaml/public-publications-descriptor.yaml`.

## Module layout (high-level)

- `pcfdlrm-datatypes-common` — shared value objects.
- `pcfdlrm-command/-command-api` (RAML + schemas), `-command-handler` (`@Handles` handlers).
- `pcfdlrm-domain/-domain-aggregate` (`MigratedCaseFileAggregate` + validation), `-domain-event` (event JSON schemas), `-domain-event-processor`, `-domain-value-schema`.
- `pcfdlrm-event/-event-listener` (stub), `-event-processor` (active).
- `pcfdlrm-event-sources` — `event-sources.yaml`.
- `pcfdlrm-query/-query-api` (RAML), `-query-view` (read services over the viewstore).
- `pcfdlrm-refdata` — reference‑data lookup + enrichers (`ReferenceDataQueryService`, `RefDataEnricher`).
- `pcfdlrm-viewstore` — Liquibase changelogs for `DS.pcfdlrm`.
- `pcfdlrm-service` — packaging WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources/queues/topics, service mapping `/pcfdlrm-[^/]+`.
- `pcfdlrm-healthchecks` — healthcheck overrides.
- `pcfdlrm-integration-test` — `*IT.java` orchestrated by `runIntegrationTests.sh`.

## Cross-context dependency bumps

When bumping any upstream interface version in `pom.xml` (`coredomain`, `progression`, `resulting`, `sjp`, `defence`, `referencedata`, `referencedata.offences`, `material`, `stream-transformation-tool`, `service-parent-pom`), also check the matching schema/RAML classifier dep is on the same version — otherwise schema validation fails at runtime. The `RequireLatestMojInterfaceRule` enforcer in CI will block stale interface versions.

## SDLC Orchestrator (hmcts-sdlc-orchestrator plugin)

This repo uses the `hmcts-sdlc-orchestrator` plugin exclusively for AI-assisted SDLC work —
its 8-stage pipeline (Requirements → Architecture & Design → User Story → Test Specs → Code →
Code Review → Build & Test → Deploy Sandbox) and agents are reused as-is. This repo's
Java/Maven CQRS/WildFly modules match the plugin's legacy context-service assumptions, so no
local agent/rule/skill overrides are maintained here.

- **Reuse from the plugin as-is:** `requirements-analyst`, `architecture-designer`,
  `story-writer`, `test-engineer`, `implementation`, `code-reviewer`, `ci-orchestrator`,
  `deployer`, `context-scaffold`, `context-service-guide`, `api-contract-check`,
  `dependency-audit`, `review-pr`, `event-flow-mapper`, `doc-generator`,
  `migration-reviewer`, `rbac-auditor`, the security hooks (`block-secrets`, `block-pii`,
  `guard-bash`, `guard-paths`).
- **Do NOT use:** `springboot-service-from-template`, `springboot-api-from-template`,
  `terraform-validate`, `helm-config-validator` — no Spring Boot, Terraform, or Helm chart
  in this repo.
- No local Spec-Kit installation, custom agents, or rule files — a previous
  `.claude/agents/` + `.claude/rules/` + `.specify/` Spec-Kit setup was installed here but
  never actually driven (no `specs/` output ever existed) and has been removed in favour of
  the plugin.
- Pipeline artefacts go to `docs/pipeline/<JIRA-TICKET>-<slug>/` (created on first use, no
  pre-scaffolding required): `00-input-brief.md` → `01-requirements.md` → `02-design.md` →
  `03-stories.md`, plus a shared `docs/pipeline/adrs/` for any architecturally-significant
  decision.

## Java style

No wildcard imports. Always use explicit per‑class imports.

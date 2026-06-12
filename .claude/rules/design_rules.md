# Architecture & Domain Rules

## Three Layers (CQRS / Event-Sourced)

```
1. Command side (handler → MigratedCaseFileAggregate → domain event)
       ↓ writes to event store (DS.eventstore)

2. Event listener (projects events → viewstore tables)
       ↓ projects to DS.pcfdlrm
       (currently a stub — viewstore Liquibase wired,
        no listener subscriptions yet)

3. Event processor (consumes domain + public events → emits public events)
       ↓ emits public.pcfdlrm.migrated-case-file-processed for progression
```

Every change touching events MUST be reasoned about across **all three layers**. Breaking one without the others produces silent data drift.

- **Command side** — RAML-declared commands hit `@Handles`-annotated handler classes (`MigratedCaseFileHandler`, `AddMaterialHandler`) which load the `MigratedCaseFileAggregate`, run validation rules under `pcfdlrm-domain-aggregate/.../validation/`, and apply domain events.
- **Event listener** — the `pcfdlrm-event-listener` module is wired into the reactor and packaged, but has no `subscriptions-descriptor.yaml` and no Java listeners yet. When the read-model is introduced, listener subscriptions + JSON schemas + converters all land together.
- **Event processor** — consumes domain events (own + public) and emits public events for the `progression` context. Lives under `pcfdlrm-event/pcfdlrm-event-processor`. Existing processors: `MigratedCaseFileProcessedProcessor`, `MigratedCaseReceivedProcessor`, `MaterialEventProcessor`, `MaterialReadyForCourtDocumentProcessor`, `ProgressionPublicEventProcessor`.

## Domain Concepts

| Concept                 | Description                                                                                                                              |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Migrated Case File      | The aggregate (`MigratedCaseFileAggregate`). One per migrated prosecution case from the legacy estate. Records intake, accept, and material additions; emits domain events on every state change. |
| Domain event            | Internal event written to the event store. Examples: `migrated-case-file-received`, `migrated-case-file-processed`, `material-added`, `material-ready-for-court-document`. |
| Public event            | Cross-context event emitted on `public.event`. Today the only one is `public.pcfdlrm.migrated-case-file-processed`, consumed by `progression`. |
| Command                 | Inbound request via `pcfdlrm.handler.command` queue. Declared in RAML, dispatched by `@Handles`. Today: `receive-migrated-case-file`, `accept-migrated-case`, `add-case-court-document`. |
| Listener                | Read-side projection — `*Listener` class (none yet) extending the framework's listener base; will project events → viewstore JPA entities via converters when introduced. |
| Processor               | Public-event emitter — `*Processor` class extending the framework's processor base; maps domain events → public-event payloads. |
| Validation rule         | Predicate evaluated by the aggregate during command handling; under `pcfdlrm-domain-aggregate/.../validation/rules/`. Failures abort the command. |
| Viewstore               | Read-model database `DS.pcfdlrm`, populated by listeners (when wired). Schema managed by `pcfdlrm-viewstore-liquibase`. |
| Event store             | Append-only log `DS.eventstore`. Source of truth for aggregate state. Schema managed by `event-repository-liquibase`. |
| Reference data enricher | `pcfdlrm-refdata` (`RefDataEnricher`, `OffenceDataRefDataEnricher`, `DefendantRefDataEnricher`, `PleaDataRefDataEnricher`) — enriches case data via `ReferenceDataQueryService`. |

## Three Subscription Sources

The processor (and, eventually, the listener) can be triggered by:

1. **Internal event topic** `pcfdlrm.event` — replay of this context's own domain events.
2. **Public event bus** `public.event` — events from other contexts. Today this service consumes:
   - `public.progression.prosecution-case-created`
   - `material.material-added`
3. **Command queue** `pcfdlrm.handler.command` — RAML-declared commands.

## Authoritative Routing Files (always re-read before reasoning about a flow)

- `pcfdlrm-event-sources/src/yaml/event-sources.yaml` — internal + public topic declarations.
- `pcfdlrm-event/pcfdlrm-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions.
- `pcfdlrm-event/pcfdlrm-event-processor/src/yaml/public-publications-descriptor.yaml` — public events emitted by the processor.
- `pcfdlrm-event/pcfdlrm-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions (currently absent; create when introducing the read-model).
- `pcfdlrm-command/pcfdlrm-command-handler/src/raml/pcfdlrm-command-handler.messaging.raml` — command → handler mapping.
- `pcfdlrm-service/src/main/descriptors/resource-descriptor.yml` — JMS queues, topics, datasources, service URI mapping.
- Per-command/per-event JSON schemas: `src/raml/json/schema/` and `src/main/resources/json/schema/` under each command/domain-event module.

## Intake Surface

| Source                                 | Mechanism / Handler                                              | Notes                                                                                          |
|----------------------------------------|------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| Migration command — receive            | `pcfdlrm.command.receive-migrated-case-file` → `MigratedCaseFileHandler` | Initial intake of a legacy-estate case into the aggregate                                       |
| Migration command — accept             | `pcfdlrm.command.accept-migrated-case` → `MigratedCaseFileHandler`  | Confirms a migrated case for downstream emission                                                |
| Material command                       | `pcfdlrm.command.add-case-court-document` → `AddMaterialHandler` | Attaches court-document material to a migrated case                                             |
| Internal replay                        | `pcfdlrm.event` topic → processor                                 | Replay of this service's own domain events (`migrated-case-file-received`, `-processed`, `material-added`, `material-ready-for-court-document`) |
| Public event — progression             | `public.progression.prosecution-case-created` → `ProgressionPublicEventProcessor` | Triggers downstream coordination when progression creates a case                                |
| Public event — material                | `material.material-added` → `MaterialEventProcessor`              | Reacts to upstream material events from the `material` context                                  |

## Module Layout

- `pcfdlrm-datatypes-common` — shared value objects
- `pcfdlrm-command/pcfdlrm-command-api` — command RAML + payload schemas
- `pcfdlrm-command/pcfdlrm-command-handler` — `@Handles` handlers
- `pcfdlrm-domain/pcfdlrm-domain-aggregate` — `MigratedCaseFileAggregate` + validation rule providers
- `pcfdlrm-domain/pcfdlrm-domain-event` — domain-event JSON schemas
- `pcfdlrm-domain/pcfdlrm-domain-event-processor` — domain-level event processor logic
- `pcfdlrm-domain/pcfdlrm-domain-value-schema` — shared schema objects
- `pcfdlrm-event/pcfdlrm-event-listener` — listener stub (no subscriptions yet); ready for read-model wiring
- `pcfdlrm-event/pcfdlrm-event-processor` — processors + converters → public events
- `pcfdlrm-event-sources` — `event-sources.yaml`
- `pcfdlrm-query/pcfdlrm-query-api` — RAML for query side
- `pcfdlrm-query/pcfdlrm-query-view` — read services over the viewstore
- `pcfdlrm-refdata` — reference-data lookup + enrichers
- `pcfdlrm-viewstore` — Liquibase changelogs for `DS.pcfdlrm`
- `pcfdlrm-service` — packaging WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources / queues / topics / service mapping (`/pcfdlrm-[^/]+`)
- `pcfdlrm-healthchecks` — healthcheck overrides (`PcfdlrmIgnoredHealthcheckNamesProvider`)
- `pcfdlrm-integration-test` — `*IT.java` orchestrated by `runIntegrationTests.sh`

## Adding a New Command

1. **RAML first.** Add the command operation to `pcfdlrm-command-handler.messaging.raml` (or the appropriate query RAML).
2. **JSON schema.** Add the command payload schema under the command-api module's `src/raml/json/schema/`.
3. **Handler.** Add a method with `@Handles("<command-name>")` on a class annotated `@ServiceComponent(COMMAND_HANDLER)`. Method takes `Envelope<CommandPayload>`.
4. **Aggregate.** If the command mutates state, the handler calls into `MigratedCaseFileAggregate`'s `apply(event)` mechanism via the framework. The aggregate emits a domain event.
5. **Listener.** If the new event is consumed by a listener (when wired): subscription entry + JSON schema + listener method + converter.
6. **Processor.** If the new event triggers a public event: subscription entry + JSON schema + processor method + converter + public-event JSON schema + entry in `public-publications-descriptor.yaml`.
7. **Tests.** Failing unit tests for handler, aggregate, processor (if touched), converters (if touched). Then production code. Then IT exercising the end-to-end flow.

## Adding a New Domain Event

Same as "Adding a New Command" steps 5–7, plus:

- Add the event's JSON schema under `pcfdlrm-domain/pcfdlrm-domain-event/src/main/resources/json/schema/`
- Update both listener (when wired) AND processor `subscriptions-descriptor.yaml` files with the new event entry (or document explicitly which is unaffected)
- Update `event-sources.yaml` if a new internal topic is introduced
- Update `public-publications-descriptor.yaml` if the event leaves this service as a public event

## Adding a Public-Event Subscription (incoming from another context)

1. **Subscription entry.** Add to processor `subscriptions-descriptor.yaml` under the `public.event.source` source (or to the listener's descriptor when the listener is wired).
2. **JSON schema.** Add the public-event schema (matches the upstream context's contract version) under the consuming module's `src/main/resources/json/schema/`.
3. **Processor method.** With `@Handles("<public-event-name>")` and `Envelope<PayloadType>`.
4. **Converter.** Map the public-event payload → either a viewstore entity (listener) or a domain command (if it triggers a state change).
5. **Tests.** Unit tests for the processor + converter. IT simulating the public-event arrival.

## Out-of-Scope (do not add)

- Hand-rolled JMS listeners — use the framework's `@Handles`
- Hand-rolled JDBC — use Liquibase changelogs and JPA repositories
- Ad-hoc `ObjectMapper` instances — use the framework's configured mapper
- Manual JSON schema validation — the framework validates incoming envelopes against subscription-declared schemas
- Spring annotations (`@Autowired`, `@Component`, `@Service`, `@RequiredArgsConstructor`) — this service does not use Spring
- Cross-context coupling beyond declared public events — never call another context's REST API for command-side traffic; consume their public events instead
- Direct REST calls to `referencedata` from a handler — go through `pcfdlrm-refdata`

## Common Gotchas

1. **Schema-subscription drift** — adding a `subscriptions-descriptor.yaml` entry without the matching JSON schema produces a runtime 500 on dispatch. Constitution Principle VI makes this a review-blocker.
2. **Public publication drift** — emitting a public event without an entry in `public-publications-descriptor.yaml` (or vice versa) silently breaks the downstream `progression` consumer. Today only `public.pcfdlrm.migrated-case-file-processed` is published — every new public event MUST land descriptor + schema + processor in the same change.
3. **Listener stub** — `pcfdlrm-event-listener` is wired into the reactor but has no `subscriptions-descriptor.yaml`. Don't assume a listener exists; check before reasoning about read-model state. When introducing the listener, land the descriptor, schemas, converters, and viewstore Liquibase entities together.
4. **Three-layer drift** — modifying a domain event without updating processor (and listener when wired) is the most common silent-data-drift bug. Constitution Principle II makes this a review-blocker.
5. **Liquibase registration** — adding a changelog file without registering it in the right registry (event-store / aggregate-snapshot / viewstore / event-buffer) AND in `runIntegrationTests.sh`'s `runLiquibase()` chain means it never applies in CI's IT setup.
6. **Cross-context pin drift** — bumping `coredomain` / `progression` / `material` / `referencedata` / `referencedata.offences` / `resulting` / `sjp` / `defence` / `stream-transformation-tool` versions in `pom.xml` requires bumping the matching schema/RAML classifier dep to the same version, otherwise schema validation fails at runtime.
7. **Wrong `@ServiceComponent` value** — `COMMAND_HANDLER` vs `EVENT_LISTENER` vs `EVENT_PROCESSOR` are NOT interchangeable; the framework dispatches based on the value.

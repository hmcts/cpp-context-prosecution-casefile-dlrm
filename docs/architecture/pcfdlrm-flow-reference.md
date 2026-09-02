# pcfdlrm Flow Reference

Single reference for `pcfdlrm`'s end-to-end migration flow (`stagingdlrm` → `pcfdlrm` →
`progression` → `listing`), its cross-context dependencies, and validation/business logic.
Consolidates the former `docs/context-dependencies.md` and
`docs/e2e-stagingdlrm-pcfdlrm-progression.md` into one document.

---

## 1. End-to-End Flow

### 1.1 System participants

| Context | Role |
|---|---|
| `cpp-context-stagingdlrm` | Inbound caller — triggers migration via REST command |
| `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`, this repo) | Orchestrator — validates, uploads material, emits to progression |
| `cpp-context-material` | Material store — receives upload commands; streams files to Alfresco |
| `cpp-context-referencedata` (+ `.offences`) | Reference data provider — enriches/validates case data via REST |
| `cpp-context-progression` | Downstream consumer — creates the prosecution case; fans out to listing |
| `cpp-context-listing` | Terminal hop — schedules or queues hearings |

### 1.2 Flow diagram

```
cpp-context-stagingdlrm
  │  POST /pcfdlrm-service/.../receive-migrated-case-file
  │  Command: pcfdlrm.command.receive-migrated-case-file
  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                            pcfdlrm                                       │
│                                                                          │
│  PHASE 1 — Receipt & validation (MigratedCaseFileHandler)                │
│    GET referencedata.get-all-document-type-access                       │
│    GET referencedata.query.get-all-parent-bundle-section                │
│    [~50 validation/enrichment rules against case/defendant/hearing/offence] │
│    Happy path → emit MaterialAdded (×N) + MigratedCaseValidatedCreationPending │
│    Failure    → emit MigratedCaseFileProcessed(successful=false) + return │
│                                                                          │
│  PHASE 2 — Material upload loop (MaterialEventProcessor, concurrent ×N) │
│    SEND material.command.upload-file → cpp-context-material              │
│    RECV material.material-added      ← cpp-context-material              │
│    SEND pcfdlrm.command.add-case-court-document (loopback)              │
│    Aggregate gate: last material confirmed → emit MigratedCaseFileReceived │
│                                                                          │
│  PHASE 3 — Case creation (MigratedCaseReceivedProcessor)                 │
│    SEND progression.initiate-court-proceedings → cpp-context-progression │
│                                                                          │
│  PHASE 4 — Accept (ProgressionPublicEventProcessor)                      │
│    RECV public.progression.prosecution-case-created ← progression        │
│    SEND pcfdlrm.command.accept-migrated-case (internal)                 │
│    Aggregate: emit MaterialReadyForCourtDocument (×N) + MigratedCaseFileProcessed(true) │
│                                                                          │
│  PHASE 5 — Court document forwarding (MaterialReadyForCourtDocumentProcessor, ×N) │
│    SEND progression.add-court-document (×N) → cpp-context-progression   │
│                                                                          │
│  PHASE 6 — Final signal (MigratedCaseFileProcessedProcessor)            │
│    EMIT public.pcfdlrm.migrated-case-file-processed → public.event      │
└──────────────────────────────────────────────────────────────────────────┘
  │                                │
  ▼ (public event)                 ▼ (initiate-court-proceedings)
stagingdlrm / progression      cpp-context-progression
                                 CourtProceedingsInitiatedProcessor
                                   → listing.command.list-court-hearing
                                   OR listing.command.list-unscheduled-court-hearing
                                   ▼
                              cpp-context-listing
```

### 1.3 Complete message inventory

| # | Direction | Message | Transport | From → To | Count |
|---|---|---|---|---|---|
| 1 | Inbound | `pcfdlrm.command.receive-migrated-case-file` | JMS → REST | stagingdlrm → pcfdlrm | 1 |
| 2 | Outbound (refdata) | `referencedata.get-all-document-type-access` | REST | pcfdlrm → referencedata | 1 |
| 3 | Outbound (refdata) | `referencedata.query.get-all-parent-bundle-section` | REST | pcfdlrm → referencedata | 1 |
| 4 | Internal event | `pcfdlrm.events.material-added` | Event store | pcfdlrm aggregate | N |
| 5 | Outbound | `material.command.upload-file` | JMS → REST | pcfdlrm → material | N |
| 6 | Inbound (public) | `material.material-added` | JMS `public.event` | material → pcfdlrm | N |
| 7 | Internal command | `pcfdlrm.command.add-case-court-document` (loopback) | JMS | pcfdlrm → pcfdlrm | N |
| 8 | Internal event | `pcfdlrm.events.migrated-case-file-received` (gate) | Event store | pcfdlrm aggregate | 1 |
| 9 | Outbound | `progression.initiate-court-proceedings` | JMS → REST | pcfdlrm → progression | 1 |
| 10 | Outbound (progression internal) | `listing.command.list-court-hearing` OR `list-unscheduled-court-hearing` | JMS → REST | progression → listing | 1/hearing |
| 11 | Inbound (public) | `public.progression.prosecution-case-created` | JMS `public.event` | progression → pcfdlrm | 1 |
| 12 | Internal command | `pcfdlrm.command.accept-migrated-case` | JMS | pcfdlrm → pcfdlrm | 1 |
| 13 | Internal event | `pcfdlrm.events.material-ready-for-court-document` | Event store | pcfdlrm aggregate | N |
| 14 | Internal event | `pcfdlrm.events.migrated-case-file-processed` | Event store | pcfdlrm aggregate | 1 |
| 15 | Outbound | `progression.add-court-document` | JMS → REST | pcfdlrm → progression | N |
| 16 | Outbound (public) | `public.pcfdlrm.migrated-case-file-processed` | JMS `public.event` | pcfdlrm → public bus | 1 |

### 1.4 Handler / processor registry

| Message | Class | Method |
|---|---|---|
| `pcfdlrm.command.receive-migrated-case-file` | `MigratedCaseFileHandler` | `receiveMigratedCaseFile()` |
| `pcfdlrm.command.accept-migrated-case` | `MigratedCaseFileHandler` | `handleAcceptMigratedCase()` |
| `pcfdlrm.command.add-case-court-document` | `AddMaterialHandler` | `addCaseCourtDocument()` |
| `pcfdlrm.events.migrated-case-file-received` | `MigratedCaseReceivedProcessor` | `handleMigratedCaseReceived()` |
| `pcfdlrm.events.material-added` | `MaterialEventProcessor` | `handleMaterialAdded()` |
| `pcfdlrm.events.material-ready-for-court-document` | `MaterialReadyForCourtDocumentProcessor` | `handleMaterialReadyForCourtDocument()` |
| `pcfdlrm.events.migrated-case-file-processed` | `MigratedCaseFileProcessedProcessor` | `handleMigratedCaseFileProcessed()` |
| `public.progression.prosecution-case-created` | `ProgressionPublicEventProcessor` | `handleProsecutionCaseCreated()` |
| `material.material-added` | `MaterialEventProcessor` | `handleMaterialAddedFromMaterialContext()` |

### 1.5 Listing routing (inside progression)

`CourtProceedingsInitiatedProcessor` routes each hearing to exactly one listing command
(mutually exclusive per hearing; a multi-hearing case can trigger both):

- `listing.command.list-court-hearing` — fires when at least one `ListHearingRequest` has a
  non-null `listedStartDateTime`/`earliestStartDateTime`/`weekCommencingDate` and the hearing
  has prosecution cases and `listHearingRequests` is non-empty.
- `listing.command.list-unscheduled-court-hearing` — fires when ALL date fields are null on
  every request AND the `unscheduledHearingListedFromThisHearing` idempotency flag is false
  AND the hearing hasn't been resulted.

Full invocation chains inside progression: `cpp-context-progression/docs/flows/initiate-court-proceedings-listing-interactions.md`.

---

## 2. Validation (Phase 1)

All validation runs inside `MigratedCaseFileAggregate.receiveMigratedCaseFile()`. The handler
pre-fetches reference data and passes it in via `CaseProcessingArgs`. Fail-fast: the first
terminal failure emits `MigratedCaseFileProcessed(successful=false)` and returns.

**Order:** material file-type → hearing → case-level → defendant-level (per defendant) →
offence-level (per offence) → plea/verdict date (per plea/verdict).

| Category | Provider | ~Rules | Failure mode |
|---|---|---|---|
| Material | `MaterialFileTypwWithCountValidationRuleProvider` | 1 | Warning only — sets `hasExhibitPayloadWithMaterialProblems`, skips the material-upload phase (§3) entirely |
| Hearing | `getMigratedHearingValidationRules()` | 7 | `NoMatchingDefendantsValidationRule` is fatal (`HearingValidationFailed` + processed-false); the rest are warnings |
| Case-level | `getCaseValidationRules(initiationCode)` | 7 | All fatal — initiation code, receipt type, sending/receiving court OU code, prosecutor OU code, summons code, police force code, case markers |
| Defendant-level | (per-defendant enrichers) | ~19 | Mostly enrich-and-warn (DOB, CRO/PNC format, postcode, nationality/ethnicity, custody status, bail conditions, email formats) |
| Offence-level | (per-offence enrichers) | ~11 | `OffenceCodeValidationAndEnricherRule` is fatal (`OFFENCE_CODE_IS_INVALID`); the rest enrich-and-warn |
| Plea / verdict | `PleaValidationRule`, `VerdictValidationRule` | 2 | Fatal — absent/future date, invalid ID |

Rule classes live under `pcfdlrm-domain/pcfdlrm-domain-aggregate/.../validation/rules/`.

**Warning vs fatal:**

| Result | Event | Effect |
|---|---|---|
| Warning | `MigratedCaseValidatedWithWarnings` | Continues; warning recorded in the event log |
| Fatal (no matching defendants) | `HearingValidationFailed` + `MigratedCaseFileProcessed(false)` | Stops immediately |
| Fatal (all other terminal failures) | `MigratedCaseFileProcessed(false)` | Stops immediately; public event still emitted downstream |

---

## 3. Material Upload — pcfdlrm → material → Alfresco

1. Each `MaterialAdded` event triggers one upload cycle via `MaterialEventProcessor.uploadToMaterialContext()`. Uploads run concurrently; no ordering guarantee across N files.
2. Outbound `material.command.upload-file` carries a CC metadata block (`caseId`, `documentTypeId`, `documentCategory`, `documentTypeDesc`, `fileCloudLocation` — the migration-blob-store source path). Defaults: `documentTypeId=460f8154-c002-11e8-a355-529269fb1459`, `documentCategory=Case level`.
3. `cpp-context-material` fetches the file from `fileCloudLocation`, streams it into Alfresco, and on success emits public `material.material-added` carrying the resulting `fileStoreId`/`alfrescoAssetId`. **pcfdlrm never calls Alfresco directly** — no compile-time or runtime dependency on it.
4. `MaterialEventProcessor.handleMaterialAddedFromMaterialContext()` guards on: CC metadata present, `fileStoreId` not already registered, `fileCloudLocationId` non-null — then sends `pcfdlrm.command.add-case-court-document` (loopback).
5. **Gate:** `AddMaterialHandler.addCaseCourtDocument()` → `MigratedCaseFileAggregate.materialAddedPostProcessing()`. Aggregate tracks `materialsAdded` (size N, set at intake) vs `materialsAddedPostProcessing` (filled as confirmations arrive). When `materialsAddedPostProcessing.size() == materialsAdded.size() - 1` (i.e. the Nth confirmation), it emits `MigratedCaseFileReceived`, which triggers Phase 3.

**No-materials edge case:** if the case has no materials (or all fail validation), `MigratedCaseFileReceived` is emitted immediately at the end of Phase 1, skipping Phase 2 entirely — Phase 5 then has nothing to forward.

---

## 4. Business Logic Notes

- **Reference-data pre-fetch:** `MigratedCaseFileHandler` fetches document-type-access and parent-bundle-section data before any aggregate call; the resulting `sections` map translates CPS bundle section codes into CPP section codes.
- **Enrich-while-validating:** classes named `*ValidationAndEnricherRule` both validate and mutate the case/defendant/offence object in place (e.g. resolving a nationality code to its display name), so domain events already carry enriched data.
- **Case snapshot:** `MigratedCaseValidatedCreationPending` stores the full enriched case; `acceptMigratedCase()` (Phase 4) reads from this snapshot rather than re-fetching reference data.
- **`MigratedCaseToProsecutionCaseConverter`** builds `InitiateCourtProceedings` from `MigratedCaseFileReceived`. The `migrationSourceSystem` field it sets is the signal `ProgressionPublicEventProcessor` uses to recognise a returned `prosecution-case-created` as DLRM-originated — without it, Phases 4–6 are skipped for that case.
- **Idempotency:** the aggregate is event-sourced; every read replays the event log. The material-confirmation gate and `unscheduledHearingListedFromThisHearing` flag are both derived from stored state, so they survive process restarts. `./runSystemCommand.sh CATCHUP` forces replay of unprocessed internal events (useful when a case stalls mid-flow — see edge cases below).
- **`sendAsAdmin`:** `MigratedCaseReceivedProcessor` sends `progression.initiate-court-proceedings` via `sendAsAdmin(...)` because migration commands arrive without a user JWT.

### Known edge cases

| Scenario | Outcome |
|---|---|
| Terminal validation failure | `MigratedCaseFileProcessed(false)` emitted immediately; `public.pcfdlrm.migrated-case-file-processed` carries `successful=false`; stagingdlrm treats it as a failed migration |
| Warning-only failures (e.g. Xhibit file type) | Processing continues; if the material file-type warning fires, Phase 2 is skipped entirely |
| One of N material uploads never confirms | Gate never reaches N−1 confirmations; case stalls in Phase 2 indefinitely — no built-in timeout/dead-letter; requires manual replay/re-trigger |
| `progression.initiate-court-proceedings` lost | pcfdlrm stays in Phase 2 state (event stored, accept never sent); `CATCHUP` can force replay |
| `public.progression.prosecution-case-created` missing `migrationSourceSystem` | No `accept-migrated-case` sent; Phases 4–6 don't run (deliberate guard excluding non-DLRM cases) |
| `material.material-added` without CC metadata block | Silently ignored — expected, since the topic is shared across contexts |
| `MigratedCaseToProsecutionCaseConverter` throws | Exception propagates; JMS redelivery retries, then dead-letters; case stuck at Phase 2 |
| Material uploads / court-document sends | No ordering guarantee across the N concurrent operations in either direction |
| Process dies mid-Phase-5 | Some `progression.add-court-document` sent, others not; `CATCHUP` can replay unprocessed `material-ready-for-court-document` events — progression's handler must be idempotent |

---

## 5. Cross-Context Dependencies

### 5.1 Inbound — public events consumed at runtime

| Event | Source | Transport | Handler |
|---|---|---|---|
| `public.progression.prosecution-case-created` | `progression` | JMS `public.event` | `ProgressionPublicEventProcessor` |
| `material.material-added` | `material` | JMS `public.event` | `MaterialEventProcessor` |

Declared in `pcfdlrm-event/pcfdlrm-event-processor/src/yaml/subscriptions-descriptor.yaml`.

### 5.2 Inbound — REST queries at runtime

All routed through `ReferenceDataQueryServiceImpl` (`pcfdlrm-refdata`) — no handler calls external REST clients directly.

| Context / API | Coverage |
|---|---|
| `referencedata` (`referencedata-query-api`) | ~30 query actions across: court/organisation data (locations, OU codes, courtrooms), offence reference data (MoJ offences, alcohol/drug levels, offence-date codes), defendant demographic data (nationality, ethnicity, custody/bail status, licence codes), document/bundle data (document-type access, parent bundle sections), prosecutor data, case markers. Full list in `ReferenceDataQueryServiceImpl`. |
| `referencedata.offences` (`referencedataoffences-query-api`) | `offences-all-versions`, `offences-list` |

### 5.3 Outbound — public event published

| Event | Consumer | Transport | Publisher |
|---|---|---|---|
| `public.pcfdlrm.migrated-case-file-processed` | `progression` (also `stagingdlrm`) | JMS `public.event` | `MigratedCaseFileProcessedProcessor` |

Declared in `pcfdlrm-event/pcfdlrm-event-processor/src/yaml/public-publications-descriptor.yaml`.

### 5.4 Outbound — commands sent at runtime

| Command | Target | Transport | Sender |
|---|---|---|---|
| `progression.initiate-court-proceedings` | `progression` | JMS → REST | `MigratedCaseReceivedProcessor` |
| `progression.add-court-document` | `progression` | JMS → REST | `MaterialReadyForCourtDocumentProcessor` |
| `material.command.upload-file` | `material` | JMS → REST | `MaterialEventProcessor` |

`MaterialEventProcessor` also re-sends `pcfdlrm.command.add-case-court-document` to itself (internal loopback, not cross-context).

### 5.5 Build-time schema/RAML contracts consumed

Pulled as Maven classifier artifacts for code generation; no runtime call unless also listed above.

| Artifact | Context | Classifier | Used for |
|---|---|---|---|
| `progression-event-processor` | `progression` | yaml | POJO generation (`pcfdlrm-domain-event-processor`) |
| `resulting-event-processor` | `resulting` | yaml | POJO generation (`pcfdlrm-domain-event-processor`) |
| `material-command-api` | `material` | raml | REST client generation |
| `progression-command-api` | `progression` | raml | REST client generation |
| `progression-query-api` | `progression` | raml | REST client generation |
| `referencedata-query-api` | `referencedata` | raml | REST client generation (command-api, command-handler, event-processor) |
| `referencedataoffences-query-api` | `referencedata.offences` | raml | REST client generation (command-api) |
| `sjp-command-api` / `sjp-query-api` | `sjp` | raml | Generated stub only — not called in production code |
| `notificationnotify-command-api` | `notification.notify` | raml | Generated stub only — not called in production code |
| `criminal-court-public-model`, `common-core-domain` | `coredomain` | — | Shared domain model across command handler, event processor, domain-event POJOs |

### 5.6 Version pins (`pom.xml`)

| Property | Contexts covered |
|---|---|
| `coredomain.version` | `criminal-court-public-model`, `common-core-domain` |
| `progression.version` | `progression-command-api`, `progression-query-api`, `progression-event-processor` |
| `material.version` | `material-command-api` |
| `referencedata.version` | `referencedata-query-api` |
| `referencedata.offences.version` | `referencedataoffences-query-api` |
| `sjp.version` | `sjp-command-api`, `sjp-query-api` |
| `resulting.version` | `resulting-event-processor` |
| `defence.version` | Version-aligned only; no artifact currently consumed |
| `notification.notify.version` | `notificationnotify-command-api` |
| `authorisation.service.version` | `authorisation-interceptor` (command-handler CDI interceptor) |

Bump the matching schema/RAML classifier dep alongside any of these — the `RequireLatestMojInterfaceRule` Maven enforcer blocks stale interface versions in CI.

### 5.7 Downstream — `cpp-context-listing` via progression

```
Azure Function (TimerTriggerJava)
  └─> POST .../receive-migrated-case-submission            [stagingdlrm]
        └─> POST .../receive-migrated-case-file             [pcfdlrm]
              └─> MigratedCaseReceivedProcessor
                    └─> progression.initiate-court-proceedings
                          └─> CourtProceedingsInitiatedProcessor  [progression]
                                ├─> listing.command.list-court-hearing
                                └─> listing.command.list-unscheduled-court-hearing
```

Conditions for each listing command are in §1.5. Full detail:
`cpp-context-progression/docs/flows/initiate-court-proceedings-listing-interactions.md`.

---

## 6. Summary Diagram

```
                        ┌─────────────────────────────────────────────────────────────────┐
                        │                        pcfdlrm                                  │
                        │                                                                 │
  progression ──────────▶  public.progression.prosecution-case-created  (JMS in)         │
  material    ──────────▶  material.material-added                       (JMS in)         │
                        │                                                                 │
  referencedata ────────▶  REST queries (~30 endpoints)                  (HTTP in)        │
  referencedata.offences▶  REST queries (2 endpoints)                    (HTTP in)        │
                        │                                                                 │
                        │  public.pcfdlrm.migrated-case-file-processed   (JMS out) ──────▶ progression
                        │  progression.initiate-court-proceedings         (cmd out) ──────▶ progression
                        │  progression.add-court-document                 (cmd out) ──────▶ progression
                        │  material.command.upload-file                   (cmd out) ──────▶ material
                        └─────────────────────────────────────────────────────────────────┘
```

# Requirements — LIBRA enabler: PCFDLRM test hardening

> Stage 1 artefact. Source: [`00-input-brief.md`](./00-input-brief.md).
> Requirements altitude — nothing here prescribes a class layout. Tasks come from
> [`02-design.md`](./02-design.md).

| | |
|---|---|
| Story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) |
| Epic | [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Size | M |
| Sibling story | [DD-43078](https://github.com/hmcts/cpp-context-stagingdlrm/tree/main/docs/pipeline/DD-43067-DD-43078-test-hardening) — same hardening in stagingDLRM, independently deliverable |
| Depends on | [ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md) approved before stage 5. No other blocker |
| Production changes | **none** — test, fixture and test-support code only |

**JIRA summary line:**
`[LIBRA enabler] Pin PCFDLRM's XHIBIT behaviour with whole-payload assertions across handler/aggregate, event processor/converters and ITs`

## Goal

Pin what PCFDLRM does today **under XHIBIT**, so that relaxing the shared DLRM schema for LIBRA
cannot silently change what this service sends to Progression.

Three test components carry the pins, and each asserts its output **as a complete payload** rather
than a selection of fields:

1. **Handler + aggregate** — commands accepted, and the events the aggregate emits.
2. **Event processor + converters** — the `InitiateCourtProceedings` payload built for Progression,
   and the `public.pcfdlrm.migrated-case-file-processed` event.
3. **Integration tests** — the same journeys end to end, at representative depth.

## Depth model

| Layer | Depth | Rationale |
|---|---|---|
| Unit / component | **Exhaustive.** Every scenario that matters: each rule path, each variant, each XHIBIT-gated behaviour. | Fast, in `mvn test`, no environment — the right place for a scenario matrix. |
| Integration | **Representative.** Enough journeys to prove the wiring and that the payload crossing each service boundary is whole. No scenario matrix. | Needs Docker, so enumerating variants there is slow on every build. |

## Requirements

### R1 — Every component asserts whole payloads

For each command accepted, domain event emitted, and outbound payload produced, the expected result
is asserted as a **complete payload** compared against a fixture.

- "Whole payload" is defined by [ADR-001 §1](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md)
  and is not redefined here: JSONassert STRICT, anchored enumerated exclusions, an exclusion
  matching no path fails the test.
- Non-deterministic values are excluded by an **explicit, enumerated** list — never a wildcard — so
  an accidentally added or dropped field cannot slip through.
- **Aggregate scenarios assert the events returned by the aggregate method, never aggregate state.**
  This is a hard rule, not a preference. Each scenario calls the same method the handler calls,
  takes the `Stream<Object>` it returns, and asserts that stream: its length, the type of each event
  in order, and each event's payload whole. **No aggregate getter may appear in an assertion** —
  not `getReceiveMigratedCaseFile()`, `getMaterialsAdded()`,
  `getMaterialsAddedPostProcessing()` or `getMaterailsReadyForCourtDocuments()`.

  The returned stream is the aggregate's entire production contract: all three public methods return
  `Stream<Object>`, and both production callers consume only that stream. No production code calls
  any aggregate getter, and two of the four are package-private — reachable solely because the test
  shares the aggregate's package. They are a test seam, not behaviour.

  Today ~25 assertions read getters instead and 13 invocations discard the returned stream entirely,
  so they cannot observe how many events were emitted, in what order, or of what type. Three
  consequences make state-based assertion not merely weaker but **incapable**:
  - The XHIBIT gate at `MigratedCaseFileAggregate:368` decides whether `MigratedCaseFileReceived`
    reaches the stream at all — R3's highest-value pin, invisible from state.
  - `MigratedCaseValidatedWithWarnings` and `MigratedCaseNotFoundInAutomation` are emitted by
    production but have **no arm in `apply()` and no field or getter on the aggregate**. No
    state-based assertion can see them under any circumstances.
  - Event *order* and *count* have no state representation at all.

### R2 — Test data is deterministic and states XHIBIT explicitly

- Every scenario states `migrationSourceSystemName` explicitly rather than relying on a builder
  default, and the baseline value is `XHIBIT`. No scenario may pass because the field happened to be
  absent or defaulted.
- **Both** source-system fields are supplied by the caller — `migrationSourceSystemName` and
  `migrationSourceSystemCaseIdentifier`. Parameterising only the name would let a later scenario
  build `{name: LIBRA, caseIdentifier: "XHIBIT-123"}`: incoherent data that would pass every
  assertion.
- `ObjectBuilder` produces **byte-identical output for identical arguments**. It currently mints
  five non-deterministic values per call (two relative dates, three random UUIDs). Left alone these
  become five exclusions repeated across four suites — and two of them, `dateOfSending` and
  `dateOfBirth`, are fields the validation rules actually read. A relative date dumped into a
  fixture is also correct on the day it is generated and wrong afterwards. Exclusion is the
  fallback, not the plan.
- Source system is **data, not control flow**: adding one later must not require a parallel test
  class, a copied fixture tree, or an `if` on source system inside a test body. This buys nothing
  for coverage in this story — its justification is purely forward-looking, and it must not be
  allowed to grow scope.

### R3 — The XHIBIT-gated behaviours are pinned, on the XHIBIT path

Each behaviour below is gated on the source system being `XHIBIT` and no-ops or is suppressed
otherwise, and each is a decision point once LIBRA arrives. **This story asserts the XHIBIT path
only** — see *Out of scope*.

| # | Behaviour | Component |
|---|---|---|
| a | Seven `isXhibit()` gates in `MigratedCaseFileAggregate` — case problems, case-marker warnings, **whether `MigratedCaseFileReceived` is emitted at all**, hearing warnings, offence problems, no-matching-defendants, defendant problems | handler + aggregate |
| b | `ExhibitFiileTypeValidationRule` — materials / Court Record Sheet file-type check. Both problem codes: `INVALID_FILE_TYPE_FOR_XHIBIT`, `INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION` | handler + aggregate |
| c | `ProsecutionCaseFileHelper.applyRuleToDefendantFields()` — defaults/normalises gender, language and ethnicity codes after a validation failure | handler + aggregate |
| d | Rule-set selection — which set `CcProsecutionValidationRuleProvider` returns for a given `initiationCode`, asserted as **set equality**. Today every migrated case lands in the generic default set because stagingDLRM forces `"O"`; once the enum is dropped (DD-43081) real codes will route into the existing `SUMMONS`/`REQUISITION`/`SJP` sets, and that must be observable rather than incidental | handler + aggregate |

**Each assertion must fail if its gated branch is deleted.** Pinning one side of a gate is worth
nothing if the assertion is coupled to a value the test itself supplied. The ten existing
`assertThat(…getMigrationSourceSystemName(), is(XHIBIT))` assertions are the counter-example to
avoid: they name the source system and assert nothing about what the gate does. Each scenario
asserts the concrete effect — the exact problem raised, or the exact normalised field values.

### R4 — No production code changes; runtime stays acceptable

- No `src/main` file changes in any module. Any production defect found is raised as a separate
  ticket, not fixed here.
- A new **test-scoped** module is permitted (ADR-001 §2): it changes no `src/main` file and no
  deployable artefact.
- The unit suite stays in the normal `mvn test` run — whole-payload comparison must not push it into
  a separate profile. ITs stay in their existing profile and must not become materially slower.

## Acceptance criteria

- **AC1** Every scenario in the hardened suite asserts at least one complete payload against a
  fixture, with any exclusions individually listed.
- **AC2** Every aggregate scenario asserts the stream returned by the aggregate method — length,
  event type in order, whole payloads — and a grep of `MigratedCaseFileAggregateTest` for
  `getMaterialsAdded`, `getMaterialsAddedPostProcessing`, `getMaterailsReadyForCourtDocuments` and
  `getReceiveMigratedCaseFile` returns **no assertion sites**.
- **AC3** `ObjectBuilder` takes both source-system fields from the caller, no test relies on
  `TestConstants.SOURCE_SYSTEM_XHIBIT` as a default, and two successive builds with identical
  arguments produce byte-identical payloads.
- **AC4** Each behaviour in R3 is asserted on the XHIBIT path against a complete payload, and the
  assertion would fail if the gated branch were deleted from production code.
- **AC5** Adding a scenario for a different source system is confined to scenario data plus
  fixtures — no new test class, no change to a test method body.
- **AC6** No IT journey resolves `migrationSourceSystemName` to `LIBRA`.
- **AC7** A deliberate experimental change dropping a field from the Progression payload or the
  public event causes at least one test to fail. Demonstrated once at the stage 6 gate; the
  experiment is not committed.
- **AC8** `mvn clean install` passes, the ITs pass in their profile without a material runtime
  increase, and no production source file has changed.

## Out of scope

- **The non-XHIBIT path of every source-system gate.** Deferred to the LIBRA story, not dropped.
  **Five** production branches are left uncovered as a result: the three R3 gates, plus
  `PleaDataRefDataEnricher` and `VerdictDataRefDataEnricher`, which resolve any non-XHIBIT source to
  `Jurisdiction.MAGISTRATES` rather than `CROWN`. Whoever enables LIBRA must pin these; this story
  gives them no signal.
- **Closing the general XHIBIT coverage gaps.** *Story owner's decision, 2026-08-07: follow-up
  ticket, not this story.* This story converts the existing scenarios and adds only what R3 requires;
  it does not audit the aggregate for untested branches generally.

  **What this actually defers is small, because R3 pulls most of it back in.** Of the six
  `MigratedCaseFileProcessed` rejection reasons never named in the aggregate test today:
  - `Invalid Prosecuting Authority` sits inside the `isXhibit()` gate at `:221`, and
    `INVALID_OFFENCE_CODE`, `MISSING_OR_INVALID_PLEA_DATE` and `MISSING_OR_INVALID_VERDICT_DATE` all
    sit inside the gate at `:433`. **All four are required by R3a** and are written in this story.
  - `COURT_RECORD_SHEET_NOT_PDF` and `COURT_RECORD_SHEET_FILE_TYPE_INVALID` — R3b pins the *rule*
    that raises both problem codes; the *aggregate's* fail-fast on them is what defers.

  So the follow-up ticket carries: those two aggregate fail-fast scenarios, the
  `MaterialAddedPendingProcess` event type (emitted by production, never named anywhere in the test),
  and the two thin entry points — `acceptMigratedCase` invoked once and `materialAddedPostProcessing`
  twice across 39 tests. None is XHIBIT-gated, which is why deferring them does not weaken the pins
  this story exists to place. Raise the ticket when this story closes so the measurements above are
  not lost.
- **Coverage of the LIBRA fields DD-43081 resolves** — `informant`, `writtenChargePostingDate`,
  `prosecutorCosts` (FR14a) and the five `exists_mandatory` officer fields (FR13). Both were
  previously in this story and are removed: they depend on decisions that have not landed, and
  neither pins *current* XHIBIT behaviour. Carried to DD-43081.
- `cpp-apitests`, and any LIBRA scenario at either test layer.
- The schema relaxation itself (DD-43081).
- Anything in `cpp-context-stagingdlrm` — that is DD-43078.
- Adding a source-system axis to PCFDLRM's rule provider (this story asserts today's selection; it
  does not change it).
- Wiring the orphaned `pcf-policeOfficerInCase.json` or the abandoned
  `getDlrmDefendantValidationRules()` stub.
- Filling the untested modules: `pcfdlrm-query` has no `src/test` at all and
  `pcfdlrm-viewstore-persistence/src/test` has no Java sources. Both are real gaps, neither is
  LIBRA-adjacent — raise separately.
- Turning the ITs into a scenario matrix.

## Risks

- `MigratedCaseFileAggregateTest` is 1,659 lines and 39 test methods. Converting it in one change
  produces an unreviewable diff — the design sequences it.
- **The aggregate test's inputs are deep-stub mocks, so no payload can be serialised until they are
  replaced with real objects.** This is invisible from the test's surface, which reads as though it
  already uses real POJOs, and it is the largest single item in the work. Sizing that assumes
  "convert 39 tests to rows" will be wrong.
- Even with the general coverage audit deferred, R3 still requires **four scenarios that do not exist
  today** (the rejection reasons behind the `:221` and `:433` gates). "Convert the existing 39" is not
  an accurate description of the aggregate work.
- A one-sided pin is easy to write vacuously: an assertion that names XHIBIT but never exercises
  what the gate does. R3's last paragraph exists because of this, and it is worth explicit review
  attention rather than trusting the scenario name.
- The aggregate and handler are hard-typed to generated POJOs, so fixture-based whole-payload
  assertions must work with generated types rather than around them.
- Defendant and case UUIDs are minted during processing. Exclusion lists must handle that without
  excluding so much that the assertion stops meaning anything.

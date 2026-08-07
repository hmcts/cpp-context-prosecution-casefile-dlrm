# Requirements — LIBRA enabler: PCFDLRM test hardening

> Stage 1 artefact (requirements). Source: [`00-input-brief.md`](./00-input-brief.md).
> Requirements altitude — nothing here prescribes a class layout. Implementation **tasks** come
> from the design stage.

## Story

**[DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) — Pin PCFDLRM's XHIBIT behaviour and
make its test suite LIBRA-extensible**

| | |
|---|---|
| Epic | [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler |
| Size | M |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Depends on | ADR-001 approved before stage 5. No other blocker — can start immediately |
| Sibling story | [DD-43078](https://github.com/hmcts/cpp-context-stagingdlrm/tree/main/docs/pipeline/DD-43067-DD-43078-test-hardening) — same hardening in stagingDLRM, independently deliverable |
| Production changes | **none** — test, fixture and test-support code only |

### Summary (JIRA summary line)

`[LIBRA enabler] Harden PCFDLRM tests: pin the three XHIBIT-only behaviours and rule-set selection, whole-payload assertions, XHIBIT-only ITs`

### User story

As a **developer about to relax the shared DLRM schema for LIBRA**,
I want **the PCFDLRM unit suite to assert complete payloads for XHIBIT across every scenario that
matters, the integration tests to prove the same journeys for XHIBIT only at representative depth,
and the source system to be a scenario parameter rather than a hardcoded builder constant**,
so that **removing schema constraints upstream cannot silently change what PCFDLRM sends to
Progression, and LIBRA scenarios can later be added as scenario data rather than as new test
classes**.

## Depth model

| Layer | Depth | Rationale |
|---|---|---|
| Unit / component | **Exhaustive.** Every scenario that matters: each rule path, each variant, each behaviour that currently branches on source system. | Fast, in `mvn test`, no environment — the right place for a scenario matrix. |
| Integration | **Representative.** Enough journeys to prove the wiring and that the payload crossing each service boundary is whole. No scenario matrix. | Needs Docker and a running environment, so enumerating variants there is expensive and slow on every build. |

## Scope

- `pcfdlrm-command/pcfdlrm-command-handler` — `MigratedCaseFileHandler`
- `pcfdlrm-domain/pcfdlrm-domain-aggregate` — `MigratedCaseFileAggregate`, `ProsecutionCaseFileHelper`
- `pcfdlrm-domain/pcfdlrm-domain-aggregate` validation rules — `CcProsecutionValidationRuleProvider`
  and the rule sets it selects; `ExhibitFiileTypeValidationRule`
- `pcfdlrm-event/pcfdlrm-event-processor` — the converters and the processed-event processor
- `pcfdlrm-integration-test` — representative depth only

## Requirements

- **FR1 — XHIBIT is the explicit baseline.** Every scenario states `migrationSourceSystemName`
  explicitly rather than relying on a fixture or builder default, and the baseline value is
  `XHIBIT`. No scenario may pass because the field happened to be absent or defaulted.
  **This is the single largest gap in this repo**: `builder/ObjectBuilder.java:43` sets the value
  from `TestConstants.SOURCE_SYSTEM_XHIBIT`, so all 39 aggregate tests are XHIBIT by construction,
  and the ten `assertThat(…getMigrationSourceSystemName(), is(XHIBIT))` assertions (lines 373, 663,
  701, 804, 1000, 1035, 1074, 1117, 1167, 1217) assert a value the builder itself just set.
- **FR2 — Assertions cover whole payloads.** For each command accepted, domain event appended, and
  outbound payload produced, the expected result is asserted as a **complete payload** compared
  against a fixture, not a selection of fields. Non-deterministic values (generated UUIDs,
  timestamps) are excluded by an **explicit, enumerated** list, so an accidental new or dropped
  field cannot slip through an over-broad wildcard. Applies in particular to the
  `public.pcfdlrm.migrated-case-file-processed` event and the `initiatecourtproceedings` payload
  built for Progression — the latter is today asserted with two `withJsonPath` spot checks
  (`ReceiveMigratedCaseFileHelper.java:180`).
- **FR3 — Source system is a scenario parameter.** The suite is structured so source system is
  data, not control flow. Adding a source system later must not require a parallel test class, a
  copied fixture tree, or an `if` on source system inside a test body. `ObjectBuilder` must accept
  the source system as an argument rather than baking in a constant.
- **FR4 — Adopt a scenario DSL where it earns its place.** Suites with more than a couple of
  multi-step or multi-variant cases adopt a scenario-stream DSL per
  [ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md).
  Simple single-assertion tests — most of the 63 `*ValidationRuleTest` classes — stay as they are;
  the DSL is a means to FR2 and FR3, not a target.
- **FR5 — Pin the three XHIBIT-only behaviours.** Each currently no-ops or is suppressed for
  non-XHIBIT sources, and each is a decision point once LIBRA arrives (analysis §3.4, §5 Q6).
  Assert current XHIBIT behaviour **and** current non-XHIBIT behaviour, so changing either is a
  visible, deliberate test change:
  1. `ExhibitFiileTypeValidationRule` — materials / Court Record Sheet file-type check; gated on
     `XHIBIT.equals(input.getMigrationSourceSystemName())` and no-ops for any non-XHIBIT source.
     Both problem codes are in scope: `INVALID_FILE_TYPE_FOR_XHIBIT` and
     `INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION`.
  2. `MigratedCaseFileAggregate`'s hearing/defendant-matching check — the condition is computed for
     every case, but the problem is surfaced only for XHIBIT.
  3. `ProsecutionCaseFileHelper.applyRuleToDefendantFields()` — defaults/normalises gender,
     language and ethnicity codes after a validation failure, XHIBIT only.
- **FR5a — The non-XHIBIT assertions must not be vacuous.** All three FR5 behaviours are assertions
  of *absence* for non-XHIBIT, which is easy to write in a way that would pass even if the
  behaviour were deleted. Each non-XHIBIT scenario asserts a concrete positive — the exact
  unchanged payload, or an empty problem list compared whole — not `assertTrue(problems.isEmpty())`
  alone.
- **FR6 — Pin rule-set selection.** Assert which rule set `CcProsecutionValidationRuleProvider`
  selects for a given `initiationCode`. Today every migrated case lands in the generic default set
  because stagingDLRM forces `"O"`; once the enum is dropped (DD-43081), real codes will route into
  the existing `SUMMONS`/`REQUISITION`/`SJP` sets, and that change must be observable rather than
  incidental. The existing test asserts by `Channel` with `anyMatch` spot checks on single rule
  classes — that is not sufficient to detect a set changing.
- **FR7 — Cover the fields PCFDLRM must newly *accept* without mapping onward.** DD-43081 FR14a
  resolves three LIBRA fields (`informant`, `writtenChargePostingDate`, `prosecutorCosts`) whose
  schema parents are `additionalProperties: false`. Whichever resolution is chosen, the suite needs
  a scenario proving a payload carrying them is **accepted**, and an XHIBIT scenario proving
  nothing about XHIBIT's handling changed. This is the one relaxation-adjacent case where the
  failure mode is acceptance, not rejection.
- **FR8 — Cover the partial-officer-block rejection.** Five fields are `exists_mandatory` in
  Progression's payload schema — `policeOfficerRank`, `policeWorkerReferenceNumber`,
  `policeWorkerLocationCode`, officer `surname` and officer `address1`. They are mandatory *if the
  officer block is sent at all*, so a LIBRA case with a partial block is rejected downstream. Pin
  that as a rejection scenario when DD-43081 FR13 lands, rather than discovering it in an
  environment.
- **FR9 — Integration tests cover XHIBIT exclusively, at representative depth.** The IT layer
  proves the wiring and boundary payloads; it does **not** replicate the unit matrix.
  - Every IT journey runs with `migrationSourceSystemName = XHIBIT`. Three command fixtures
    currently do not — `pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json`,
    `-multiple-hearing-wc-migrated-case-file.json`, `receive-with-no-hearing-migrated-case-file.json`.
    Each is re-pointed at XHIBIT or gains an XHIBIT equivalent; convert-vs-duplicate is a design
    decision, but LIBRA-only coverage of a journey is not an acceptable XHIBIT baseline.
    `prosecutorOffenceId` values containing the string `LIBRA` are **not** in scope — renaming them
    is churn with no coverage effect.
  - Journeys kept at IT level: case file received and processed through to the public event;
    material addition. Field-level variants and the FR5 behaviours stay at unit level.
  - Boundary payloads are still asserted **whole** per FR2 — a thinner assertion would defeat the
    point of the layer.
- **NFR1 — No production code changes.** Any production defect found is raised as a separate
  ticket, not fixed here. A new **test-scoped** module is permitted (see ADR-001 §2) since it
  changes no `src/main` file and no deployable artefact.
- **NFR2 — Runtime stays acceptable, per layer.** The unit suite stays in the normal `mvn test`
  run; whole-payload comparison must not push it into a separate profile. ITs stay in their
  existing profile and must not become materially slower — the constraint that keeps FR9
  representative.

## Acceptance criteria

- **AC1** Given the hardened unit suite, when it runs, then every scenario asserts at least one
  complete payload against a fixture, with any exclusions individually listed.
- **AC2** Given a developer adds a scenario for a different source system, when they do so, then
  the change is confined to scenario data plus fixtures — no new test class, no change to a test
  method body.
- **AC3** Given `ObjectBuilder`, when a test builds a migrated case, then the source system is
  supplied by the caller and no test relies on `TestConstants.SOURCE_SYSTEM_XHIBIT` as a default.
- **AC4** Given each behaviour in FR5, when the suite runs, then both the XHIBIT and the
  non-XHIBIT path are asserted explicitly, not by omission, and each non-XHIBIT assertion states a
  concrete positive per FR5a.
- **AC5** Given an `initiationCode` value per FR6, when a migrated case is processed, then the test
  asserts which rule set was selected, as a set rather than a spot check.
- **AC6** Given a deliberate experimental change that drops a field from the Progression payload or
  the public event, when the suite runs, then at least one test fails. Demonstrated once at review;
  the experiment is not committed.
- **AC7** Given the IT suite, when it runs, then no journey resolves `migrationSourceSystemName` to
  `LIBRA`, and the three fixtures named in FR9 no longer provide LIBRA-only coverage.
- **AC8** Given `mvn clean install`, when it completes, then all unit suites pass, the ITs pass in
  their profile without a material runtime increase, and no production source file has changed.

## Out of scope

None of the following is part of this story:

- `cpp-apitests`, and any LIBRA scenario at either test layer.
- The schema relaxation itself (DD-43081).
- Anything in `cpp-context-stagingdlrm` — that is DD-43078.
- Adding a source-system axis to PCFDLRM's rule provider (this story asserts today's selection; it
  does not change it).
- Wiring the orphaned `pcf-policeOfficerInCase.json` or the abandoned
  `getDlrmDefendantValidationRules()` stub.
- Filling the untested modules: `pcfdlrm-query` has no `src/test` at all and
  `pcfdlrm-viewstore-persistence/src/test` has no Java sources. Both are real gaps, neither is
  LIBRA-adjacent — raise separately rather than absorbing them here.
- Turning the ITs into a scenario matrix.

## Risks and notes

- The aggregate and command handler are hard-typed to generated POJOs, so fixture-based
  whole-payload assertions must work with generated types rather than around them.
- `MigratedCaseFileAggregateTest` is 1,659 lines and 39 test methods. Converting it wholesale in
  one change produces an unreviewable diff; the design should sequence it.
- FR5 asserts absences ("no-ops for non-XHIBIT"), which is easy to write vacuously. FR5a exists
  because of this, and it is worth explicit review attention rather than trusting the scenario name.
- FR7 and FR8 depend on DD-43081 decisions that have not landed. They are the only externally
  dependent part of this story; everything else can proceed without them.
- Defendant and case UUIDs are minted during processing. Exclusion lists must handle that without
  excluding so much that the assertion stops meaning anything.

## Notes for the design stage

1. **The scenario-DSL convention is already settled** — [ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md).
   Do not re-derive it or invent a local variant; link it from the PR description.
2. **"Whole payload" needs no local definition** — ADR-001 §1 fixes it: JSONassert STRICT,
   anchored enumerated exclusions, an unused exclusion fails the test.
3. **Resist scope creep at the IT layer.** FR9 caps the ITs deliberately. Once they are open, the
   temptation is to port unit scenarios into them — little gain, Docker runtime cost on every
   build.
4. **AC6 (the deliberate-break check) is the only real proof** that FR2 was achieved. Keep it as an
   explicit review step rather than folding it into a task.
5. **Sequence the aggregate conversion.** It is the largest single piece of work in the story and
   the one most likely to stall review.

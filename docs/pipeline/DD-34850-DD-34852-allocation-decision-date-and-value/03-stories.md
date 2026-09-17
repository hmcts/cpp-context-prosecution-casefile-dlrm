# 03 — User Story

- **Story:** [DD-34852](https://tools.hmcts.net/jira/browse/DD-34852) (epic
  [DD-34850](https://tools.hmcts.net/jira/browse/DD-34850))
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Requirements:** [`01-requirements.md`](./01-requirements.md) — authoritative for scope,
  FR-1..FR-9, NFR-1..NFR-5 and AC-1..AC-7. All of OQ-1..OQ-5 resolved 2026-09-17.
- **Design:** [`02-design.md`](./02-design.md) — authoritative for the exact code change and
  test plan. Status: "Ready for stage 3 (user story) and implementation."
- **Status:** Draft — awaiting human review before handoff to stage 4 (test-engineer).

## Why this is one story, not several

INVEST checked against the single Jira story DD-34852, not re-split:

- **Independent** — one converter method, no dependency on any other incomplete story. OQ-4
  confirmed no `progression`/CAAG-side story is needed; this story is a complete, shippable unit
  on its own.
- **Negotiable** — the *value* mapping (`motReasonId`/`motReasonCode`/`motReasonDescription`) is
  explicitly not touched (FR-3, OQ-2 discharged by evidence), so there is no bundling of an
  unrelated fix into this ticket.
- **Valuable** — closes a live data-loss defect: `allocationDecisionDate` is silently dropped on
  every migrated case, LIBRA and XHIBIT alike, since this repo's initial commit.
- **Estimable / Small** — one `.withAllocationDecisionDate(...)` call plus one null-safe helper
  method in `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter`; no schema, descriptor,
  or cross-repo change (NFR-4, `02-design.md` § Pattern and scope classification).
- **Testable** — AC-1..AC-7 in `01-requirements.md` are already fully derived and test-plan-mapped
  in `02-design.md` § Test plan (5 new unit tests, 2 IT fixtures to change, 6 IT fixtures that
  must not change).

FR-1..FR-9 are all facets of this one code change (the fix itself, its case-type-agnostic shape,
its null-guard, and the fixture/test proof it took effect) — splitting them into separate stories
would produce artificial, non-independently-valuable slices. The task-per-story pattern used in
`docs/pipeline/DD-43067-DD-43099-pcfdlrm-test-hardening/03-stories.md` does not apply here: that
story had four genuinely independent sub-tasks across different test layers; this story has a
single production touch point and no dependency graph to slice along.

No ADR is needed — `02-design.md` § Follow-ups records this explicitly ("not recommended. No
architecturally significant decision — one field, existing contract, existing pattern, fully
reversible").

---

## DD-34852 — Emit `allocationDecisionDate` for migrated allocation decisions

### User story

As the **`progression` context consuming a migrated prosecution case file**,
I want **an offence's migrated `allocationDecisionDate` to be carried into the courts
`Offence.allocationDecision` emitted on `progression.initiate-court-proceedings`**,
so that **Common Platform persists and CAAG displays the allocation decision date for migrated
Charge, Summons and Postal Charge cases, exactly as it already does for non-migrated cases**.

### Background

`buildAllocationDecision(...)` in `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter`
populates `motReasonId`, `motReasonCode`, `motReasonDescription`, `sequenceNumber` and
`courtIndicatedSentence` on the outgoing courts `AllocationDecision`, but never calls
`.withAllocationDecisionDate(...)`. The migrated `allocationDecisionDate` field exists, optional,
on both the migrated and courts schemas, and is read by nothing today — it is dropped on every
migrated case, for both LIBRA and XHIBIT, since this repo's initial commit (`01-requirements.md`
§ Evidence). This story stops at the `progression.initiate-court-proceedings` payload boundary
(OQ-4, resolved) — persistence in CP and rendering on CAAG are existing, unchanged behaviour for
non-migrated cases and need no work here, since no schema changes (NFR-4).

The fix (`02-design.md` § The code change): add a null-safe `getAllocationDecisionDate(offence)`
helper that maps `offence.getAllocationDecision().getAllocationDecisionDate()` through the
converter's existing `getDate(LocalDate)` helper (already used for `arrestDate`, `chargeDate`,
`offenceCommittedEndDate`), and wire it into the builder via
`.withAllocationDecisionDate(getAllocationDecisionDate(offence))`. Straight assignment — no
fallback or derivation of the kind `convictionDate` has (OQ-3) — and no case-type branching
(FR-4): case type reaches the converter only as `paramsVO.getInitiationCode()` and plays no part
in allocation-decision conversion today, so Charge/Summons/Postal Charge behaviour is identical by
construction, not by a conditional that must be kept in step across three case types.

### Acceptance criteria

- [ ] **AC-001** (from AC-1 / requester AC1 — Charge case)
  *Given* a migrated **Charge** case whose offence has an `allocationDecision` with a resolvable
  `motReasonId` and an `allocationDecisionDate` of `2025-03-17`,
  *when* the case is converted for `progression`,
  *then* the emitted courts `Offence.allocationDecision` has `motReasonId`, `motReasonCode`,
  `motReasonDescription` and `sequenceNumber` populated from reference data **and**
  `allocationDecisionDate = "2025-03-17"`.

- [ ] **AC-002** (from AC-2 / requester AC2 — Summons case)
  *Given* the same migrated allocation decision on a **Summons** case,
  *when* the case is converted for `progression`,
  *then* the same assertions as AC-001 hold, with identical values.

- [ ] **AC-003** (from AC-3 / requester AC3 — Postal Charge case)
  *Given* the same migrated allocation decision on a case with `CaseType.REQUISITION`
  (initiation code `Q` — confirmed as "Postal Charge", OQ-1),
  *when* the case is converted for `progression`,
  *then* the same assertions as AC-001 hold, with identical values. AC-001..AC-003 are satisfied
  by a single case-type-agnostic implementation (FR-4) — the three case types are a test
  obligation, not three separate code paths, and are ideally proven by one `@ParameterizedTest`
  over `{C, S, Q}` with identical assertions.

- [ ] **AC-004** (from AC-4 / FR-5 — no allocation decision, no date)
  *Given* a migrated offence with no `allocationDecision` and a `modeOfTrialDerived` that does not
  trigger the summary-only fallback,
  *when* the case is converted for `progression`,
  *then* the emitted offence's `allocationDecision` is `null` — unchanged from today, and no bare
  date-only object is produced (the courts schema requires `offenceId`, `motReasonId`,
  `motReasonDescription`, `motReasonCode`, `sequenceNumber`, so such an object would fail schema
  validation at dispatch).

- [ ] **AC-005** (from AC-5 / FR-6 — allocation decision without a date)
  *Given* a migrated offence with a resolvable `motReasonId` but no `allocationDecisionDate`,
  *when* the case is converted for `progression`,
  *then* the emitted `allocationDecision` is populated as today and `allocationDecisionDate` is
  omitted (never emitted as `null` or an empty string), and the payload still validates against
  the courts schema (NFR-1).

- [ ] **AC-006** (from AC-6 / FR-7 — summary-only fallback preserved)
  *Given* a migrated offence with **no** `allocationDecision` object but `modeOfTrialDerived =
  SUMMARY`,
  *when* the case is converted for `progression`,
  *then* the summary-only MOT reason is still emitted exactly as today, with no
  `allocationDecisionDate` (there is no migrated object to take one from) and no
  `NullPointerException` from the new null-guarded helper — this is the regression case for the
  null-guard on `offence.getAllocationDecision()`.

- [ ] **AC-007** (from AC-7 / FR-8 — integration fixtures reflect the date)
  *Given* the existing IT `ReceiveMigratedCaseFileIT.shouldSetAllocationDecisionForDifferentScenario`,
  *when* it runs against the fixed converter,
  *then* the `allocation-with-decision.json` and `allocation-with-indictable-decision.json`
  expectation fixtures assert `allocationDecisionDate = "2025-03-17"`, matching what their input
  commands already supply, and the IT passes. A PR that leaves these two fixtures unchanged is
  either not carrying the fix or has not run the ITs (`01-requirements.md` § Risks — "the fixture
  trap").

### NFR links

- **NFR-1 (Schema integrity)** — emitted payload must validate against `criminal-court-public-model`
  17.104.4 `allocationDecision.json`; zero validation failures at dispatch.
- **NFR-2 (Backwards compatibility)** — migrated cases without an `allocationDecisionDate` must
  continue to convert and emit byte-identically to today. Proven by six named IT fixtures
  (`libra-journey.json`, `libra-indicated-plea.json`, `retrial-true.json`, `retrial-false.json`,
  `no-material.json`, `allocation-no-decision.json`) that must not change.
- **NFR-3 (Source-system parity)** — fix applies to LIBRA and XHIBIT alike; no
  `migrationSourceSystemName` gate is introduced.
- **NFR-4 (No schema change)** — zero descriptor/schema/version changes; both fields already
  exist on both schemas.
- **NFR-5 (Data protection)** — no new logging of case content; the date needs no log line.

No accessibility or performance NFR applies — this is a backend event-processor payload change
with no rendered UI in this repo.

### Out of scope for this story

- **Persistence in CP and display on CAAG.** Downstream of the `progression` boundary; confirmed
  (OQ-4) existing behaviour for non-migrated cases, applying unchanged since this story makes no
  schema change. No `progression`- or CAAG-side story is needed.
- **The allocation decision *value* mapping.** `motReasonId`/`motReasonCode`/`motReasonDescription`/
  `sequenceNumber` and the `retrieveModeOfTrialReason(...)`/`validateAllocationDecisionWhenExist(...)`/
  `validateAllocationDecisionWhenNotExist(...)` resolution are confirmed correct (FR-3, OQ-2) and
  must not be refactored as part of this change.
- **The plea/verdict half of R5** (`DD-43067-DD-43130-pcfdlrm-schema-enablement/01-requirements.md:647`).
  Confirmed out of scope (OQ-5) — a separate ticket will cover it.
- **Renaming `CaseType.REQUISITION` to `POSTAL_CHARGE`.** Same code (`Q`), different name — a
  separate tidy-up with its own blast radius across `CcProsecutionValidationRuleProvider` and its
  tests.
- **Adding a new `allocationDecisionValue` field to either schema.** Ruled out by the schema
  comparison (OQ-2) — "value" is the existing `motReason*` triple, not a new field.
- **`originatingHearingId`.** Optional on both schemas, unpopulated today, not named by any AC —
  left unset, same disposition as DD-34568.
- **Any change to validation rules or rule-set routing.** Allocation decision conversion is not
  gated by validation outcome, and FR-4 forbids introducing such a gate.

### Definition of done

- [ ] Code reviewed and approved — the diff is confined to
      `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter` (`buildAllocationDecision(...)`
      plus the new `getAllocationDecisionDate(...)` helper); no fallback/derivation logic, no
      date formatter beyond the existing `getDate(LocalDate)`, no case-type conditional
      introduced (per `02-design.md` § What this deliberately does not do).
- [ ] AC-001..AC-007 covered by automated tests:
  - 5 new unit tests in `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest`
    (AC-001/002/003 ideally as one `@ParameterizedTest` over `{C, S, Q}`; AC-005 as
    `shouldOmitAllocationDecisionDateWhenMigratedDateAbsent`; AC-006 as
    `shouldNotSetAllocationDecisionDateForSummaryOnlyFallback`, the NPE-regression test).
  - The 5 pre-existing allocation-decision unit tests in the same class stay green, unedited —
    that is itself part of the AC-005/NFR-2 evidence.
  - AC-004 is already covered by the existing
    `shouldNotSetAllocationDecisionWhenOffenceMoTIsOtherAndNoAllocationDecision` test and needs no
    new code.
  - AC-007: `allocation-with-decision.json` and `allocation-with-indictable-decision.json`
    updated with `"allocationDecisionDate": "2025-03-17"`; `allocation-no-decision.json` and the
    other five NFR-2 fixtures unchanged. Run via `./runIntegrationTests.sh` or
    `mvn -pl pcfdlrm-integration-test test -Dit.test=ReceiveMigratedCaseFileIT`.
- [ ] `mvn -pl pcfdlrm-event/pcfdlrm-event-processor test` green.
- [ ] No accessibility audit required (no rendered UI in this repo/story).
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on sandbox (via `context-validation` pipeline — the IT evidence for
      AC-007 comes from there, not the PR build, since ITs do not run under `mvn verify`).
- [ ] PR description explicitly names the `allocation-with-decision.json` /
      `allocation-with-indictable-decision.json` fixture diffs as the regression proof (FR-8) —
      a green run that doesn't touch them is treated as suspect at review.
- [ ] Jira ticket DD-34852 updated with test evidence (unit test results + the
      `context-validation` IT run reference).

### Notes / open questions

- No new Jira ticket is created by this stage — DD-34852 already exists under epic DD-34850 and
  is the linked story for this pipeline directory; this artefact is its stage-3 content, ready to
  attach to that existing ticket (labels `claude-generated`, `needs-review` to be applied there).
- All five open questions in `01-requirements.md` (OQ-1..OQ-5) are resolved; none remain open for
  this story.
- Per this repo's CLAUDE.md pipeline convention, stage 4 (test-engineer) should scaffold directly
  against AC-001..AC-007 and the test plan in `02-design.md` § Test plan — no further
  decomposition is expected there.

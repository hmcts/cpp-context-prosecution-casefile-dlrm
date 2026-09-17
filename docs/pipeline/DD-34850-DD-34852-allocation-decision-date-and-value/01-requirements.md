# 01 — Requirements

- **Story:** [DD-34852](https://tools.hmcts.net/jira/browse/DD-34852) (epic
  [DD-34850](https://tools.hmcts.net/jira/browse/DD-34850))
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Status:** **Draft — all open questions resolved (2026-09-17), ready for stage 2 (design).**
  OQ-1: "Postal Charge" is `CaseType.REQUISITION`. OQ-2: "allocation decision value" is the
  `motReasonId`/`motReasonCode`/`motReasonDescription` triple, already implemented correctly in
  `buildAllocationDecision(...)` — no defect, FR-3 discharged by evidence. OQ-3:
  `allocationDecisionDate` is a straight assignment, no fallback/derivation. OQ-4: this story stops
  at the `progression` payload — no `progression`/CAAG work needed, since no schema change is
  involved. OQ-5: the plea/verdict half of R5 is out of scope here and will get its own separate
  ticket.

## Context

`pcfdlrm` converts a migrated offence into the courts `Offence` model and emits it to `progression`
via `progression.initiate-court-proceedings` (behind the public event
`public.pcfdlrm.migrated-case-file-processed`). The migrated payload carries an
`allocationDecision` object with an `allocationDecisionDate`; the outgoing courts
`allocationDecision` object has the same field. The converter **reads neither and writes neither**:
the date is silently dropped on every migration, for both LIBRA and XHIBIT, and has been since this
repo's initial commit.

This is the same shape of defect as the indicated-plea gap closed in DD-34567/DD-34568 and the
`vehicleMake` drop fixed in DD-43067/DD-43130 — a migrated field that reaches `pcfdlrm` and dies
before `progression`. The fix is comparably small, and the scoping question (OQ-4) is resolved:
this story stops at the `progression` payload.

"Allocation decision value" has no literal counterpart in either schema and is confirmed (OQ-2,
resolved 2026-09-17) to be the already-implemented `motReasonId` / `motReasonCode` /
`motReasonDescription` triple, correctly and completely mapped today. This story therefore has one
code change (**add the missing date**, confirmed defect, confirmed fix site) plus new test coverage
tying value and date to each case type — the value mapping itself needs no change.

## Actors

| Actor | Relevance to this story |
|-------|-------------------------|
| Legacy migration (LIBRA / XHIBIT) | Source of the migrated `allocationDecision`, including the date. |
| `pcfdlrm` event processor | System under change — performs the migrated → courts offence conversion. |
| `progression` | Downstream consumer of `progression.initiate-court-proceedings`; persists the case in CP. Not changed by this story (confirmed, OQ-4). |
| CAAG user (caseworker / legal adviser) | End consumer who must see the allocation decision value and date on a migrated case. Reads from CP via `progression`, not from `pcfdlrm`. |

## Scope boundary (confirmed — OQ-4 resolved 2026-09-17)

The requester ACs say "persist this … in CP" and "display both on CAAG". Both name systems
**downstream of** the `pcfdlrm` → `progression` boundary. Confirmed boundary for this story,
matching how the sibling indicated-plea story DD-34567/DD-34568 was scoped in this repo:

- **In scope for `pcfdlrm`:** emit a courts `allocationDecision` on
  `progression.initiate-court-proceedings` that carries a correct `allocationDecisionDate` and a
  correct/complete allocation decision value, for every migrated case regardless of case type.
- **Out of scope for `pcfdlrm`:** the persistence of that payload in CP and its rendering on CAAG —
  confirmed `progression`'s and CAAG's own existing behaviour for non-migrated cases, requiring no
  extra work, since this story introduces no schema change (NFR-4).

This story stops at the `progression` payload. No `progression`- or CAAG-side story is needed.

## Functional requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1 | When a migrated offence has an `allocationDecision` with a non-null `allocationDecisionDate`, **and** an allocation decision is emitted for that offence, the outgoing courts `Offence.allocationDecision` MUST carry that date in `allocationDecisionDate`. This is the confirmed defect: `buildAllocationDecision(...)` never calls `.withAllocationDecisionDate(...)`. | Must |
| FR-2 | The emitted `allocationDecisionDate` MUST satisfy the courts schema's `datePattern` (`yyyy-MM-dd`, strict calendar-valid). The existing `getDate(LocalDate)` helper (converter line 303) already produces exactly this and is the expected mechanism — the requirement is on the output, not the implementation. | Must |
| FR-3 | The **allocation decision value** carried to `progression` — `motReasonId`, `motReasonCode`, `motReasonDescription` (plus `sequenceNumber`) — is already correctly and completely mapped by the existing `buildAllocationDecision(...)` implementation. Confirmed (OQ-2, resolved 2026-09-17): no defect exists in this mapping. FR-3 is discharged by evidence; no code change is required for the value half of this story. | Must |
| FR-4 | Behaviour MUST be identical for Charge, Summons and Postal Charge cases. `buildAllocationDecision(...)`, `ModeOfTrialRefDataEnricher` and `OffenceDataRefDataEnricher` contain **no case-type or channel conditional** today, so the correct implementation of FR-1 is one that stays case-type-agnostic. Per-case-type branching MUST NOT be introduced. | Must |
| FR-5 | Where no allocation decision is emitted for an offence (i.e. `retrieveModeOfTrialReason(...)` returns null and `buildAllocationDecision(...)` returns null today), the behaviour MUST remain unchanged — no `allocationDecision` object, and therefore no date. A bare `allocationDecision` carrying only a date MUST NOT be constructed: the courts schema requires `offenceId`, `motReasonId`, `motReasonDescription`, `motReasonCode` and `sequenceNumber`, so such an object would fail schema validation at dispatch. | Must |
| FR-6 | Where an allocation decision **is** emitted but the migrated `allocationDecisionDate` is absent, `allocationDecisionDate` MUST be omitted from the emitted object. It is optional on both the migrated and the courts schema, so omission validates. **No fallback or derivation** (of the kind `convictionDate` has at converter lines 148–153) is introduced — confirmed straight assignment (OQ-3, resolved 2026-09-17). | Must |
| FR-7 | All existing allocation-decision behaviour MUST be preserved: the summary-only fallback via `validateAllocationDecisionWhenNotExist(...)`, MOT-reason resolution against `ModeOfTrialReasonsReferenceData`, `sequenceNumber` from the reference-data `seqNum`, and `courtIndicatedSentence` mapping. No regression in the existing unit or integration coverage. | Must |
| FR-8 | The three existing integration-test expectation fixtures **currently encode the defect** and MUST be updated as part of this story, not left green. `allocation-with-decision.json` and `allocation-with-indictable-decision.json` both assert an `allocationDecision` with no `allocationDecisionDate`, while their input commands supply `"allocationDecisionDate": "2025-03-17"`. A fix that leaves these fixtures untouched will fail; a fix that leaves them untouched *and* passes means the fix did not take effect. | Must |
| FR-9 | New test coverage MUST exist tying allocation-decision emission (value **and** date) to each of the three named case types. There is **no existing coverage** of allocation decision combined with case type anywhere — this is new coverage, not an extension. Postal Charge is `CaseType.REQUISITION` (OQ-1, resolved). | Must |

## Non-functional requirements

| ID | Category | Requirement | Threshold |
|----|----------|-------------|-----------|
| NFR-1 | Schema integrity | The emitted `progression.initiate-court-proceedings` payload must validate against `criminal-court-public-model` 17.104.4 `json/schema/global/allocationDecision.json`. | Zero validation failures at dispatch |
| NFR-2 | Backwards compatibility | Existing migrated cases without an `allocationDecisionDate` must continue to convert and emit exactly as today. | No change in emitted payload for that case |
| NFR-3 | Source-system parity | The fix applies to LIBRA and XHIBIT alike. Unlike DD-34568, there is no `migrationSourceSystemName` gate here, and none is to be added. | Identical behaviour both sources |
| NFR-4 | No schema change | This story requires **no** change to the migrated schema, the courts model version, or any `subscriptions-descriptor.yaml` / `public-publications-descriptor.yaml`. Both fields already exist on both schemas. If design finds otherwise, that is a scope change to raise, not to absorb. | Zero descriptor/version changes |
| NFR-5 | Data protection | Allocation decision date is case data, not PII, but must not be logged in isolation of the existing logging policy. No new logging of case content. | No new PII/case-content log lines |

## Requester-supplied acceptance criteria (verbatim, authoritative)

- **AC1 — Charge case.** GIVEN a migrated **Charge** case is created on Common Platform, WHEN the
  case detail has an allocation decision value and allocation decision date against any offence on
  the case, THEN persist this allocation decision value and allocation decision date in CP AND
  display both on CAAG as is done now for any other case.
- **AC2 — Summons case.** As AC1, for a migrated **Summons** case.
- **AC3 — Postal Charge case.** As AC1, for a migrated **Postal Charge** case.

## Derived testable acceptance criteria

These restate AC1–AC3 at the `pcfdlrm` boundary defined in *Scope boundary*, plus the edge cases
the schemas force. They add no business behaviour beyond the requester's three ACs.

- **AC-1 — Charge case carries value and date** *(from AC1, FR-1/FR-3/FR-4)*
  - *Given* a migrated **Charge** case whose offence has an `allocationDecision` with a resolvable
    `motReasonId` and an `allocationDecisionDate` of `2025-03-17`
  - *When* the case is converted for `progression`
  - *Then* the emitted courts `Offence.allocationDecision` has `motReasonId`, `motReasonCode`,
    `motReasonDescription` and `sequenceNumber` populated from reference data **and**
    `allocationDecisionDate = "2025-03-17"`.

- **AC-2 — Summons case carries value and date** *(from AC2)*
  - As AC-1, for a migrated **Summons** case; same assertions, same values.

- **AC-3 — Postal Charge case carries value and date** *(from AC3)*
  - As AC-1, for a migrated case with `CaseType.REQUISITION` (initiation code `Q` — confirmed
    2026-09-17 as the "Postal Charge" case type, OQ-1 resolved); same assertions, same values.

- **AC-4 — No allocation decision, no date** *(from FR-5)*
  - *Given* a migrated offence with no `allocationDecision` and a `modeOfTrialDerived` that does not
    trigger the summary-only fallback
  - *Then* the emitted offence has `allocationDecision = null` — unchanged from today, and no bare
    date-only object is produced.

- **AC-5 — Allocation decision without a date** *(from FR-6)*
  - *Given* a migrated offence with a resolvable `motReasonId` but no `allocationDecisionDate`
  - *Then* the emitted `allocationDecision` is populated as today and `allocationDecisionDate` is
    absent; the payload still validates against the courts schema.

- **AC-6 — Summary-only fallback preserved** *(from FR-7)*
  - *Given* a migrated offence with **no** `allocationDecision` but `modeOfTrialDerived = SUMMARY`
  - *Then* the summary-only MOT reason is still emitted exactly as today, with no
    `allocationDecisionDate` (there is no migrated object to take one from).

- **AC-7 — Integration fixtures reflect the date** *(from FR-8)*
  - *Given* the existing IT `ReceiveMigratedCaseFileIT.shouldSetAllocationDecisionForDifferentScenario`
  - *Then* the `allocation-with-decision` and `allocation-with-indictable-decision` expectation
    fixtures assert `allocationDecisionDate = "2025-03-17"`, matching what their input commands
    already supply, and the IT passes.

## Evidence

Re-verified 2026-09-16 against the working tree at `team/libra1` (`e0dbc52`).

- **The defect.** `pcfdlrm-event/pcfdlrm-event-processor/src/main/java/uk/gov/moj/cpp/pcfdlrm/event/processor/convertor/ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.java`,
  `buildAllocationDecision(...)` lines 276–291, builds the courts object with `withOffenceId`,
  `withMotReasonId`, `withMotReasonCode`, `withMotReasonDescription`, `withSequenceNumber`,
  `withCourtIndicatedSentence` — and **no** `withAllocationDecisionDate`. Confirmed by direct read.
- **The field is never touched anywhere.** `grep -rn "allocationDecisionDate|AllocationDecisionDate"`
  across all `*.java` and `*.json` outside `target/` returns exactly three hits, none of them Java:
  the migrated schema declaration, and two integration-test **input** command fixtures. No
  production code reads or writes it.
- **The drop is baked into the expected fixtures.** Input
  `pcfdlrm-integration-test/src/test/resources/command-json/pcfdlrm.command.receive-migrated-case-file-with-allocation-decision.json:117`
  supplies `"allocationDecisionDate": "2025-03-17"`; the corresponding expectation
  `pcfdlrm-integration-test/src/test/resources/json/xhibit/initiate-court-proceedings/allocation-with-decision.json`
  asserts an `allocationDecision` of exactly `{motReasonCode, motReasonDescription, motReasonId,
  offenceId, sequenceNumber}` — no date. Same for
  `...-with-indictable-allocation-decision.json:117` → `allocation-with-indictable-decision.json`.
  This is what makes FR-8 non-optional.
- **Both schemas have the field; neither requires it.** Migrated:
  `pcfdlrm-domain/pcfdlrm-domain-value-schema/src/main/resources/json/schema/migrated/migrated-allocation-decision.json`
  — `allocationDecisionDate` is `definitions/date`, and the only `required` field is `motReasonId`.
  Courts: `criminal-court-public-model` 17.104.4 `json/schema/global/allocationDecision.json` —
  `allocationDecisionDate` is `courtsDefinitions.json#/definitions/datePattern` (a `string` with a
  strict calendar regex), and `required` is `offenceId, motReasonId, motReasonDescription,
  motReasonCode, sequenceNumber`. Both schemas are `additionalProperties: false`.
- **Type crossing.** The migrated field uses the same `definitions/date` ref as `chargeDate` and
  `offenceCommittedEndDate`, which the converter passes through `getDate(final LocalDate)` (line
  303, `date.toString()`) before handing to the courts builder — so the generated migrated accessor
  is a `LocalDate` and the courts setter takes a `String`. `LocalDate.toString()` emits ISO-8601,
  which satisfies `datePattern`. (Types inferred from the generation pattern — the value-schema jar
  ships schemas only; confirm at design time against generated sources.)
- **No `allocationDecisionValue` field exists.** Neither schema has any field named "value" for the
  allocation decision. The only candidate for what the requester means is the `motReason*` triple.
  This is the basis for OQ-2.
- **The code-vs-UUID resolver exists for MOT reason.** `retrieveModeOfTrialReason(...)` /
  `validateAllocationDecisionWhenExist(...)` (lines 255–274) match the migrated `motReasonId`
  against `ModeOfTrialReasonsReferenceData`. R5 in
  `docs/pipeline/DD-43067-DD-43130-pcfdlrm-schema-enablement/01-requirements.md:647` claims "no
  resolver exists anywhere in the pipeline" for plea/verdict/allocationDecision — **for allocation
  decision that claim is stale.** The gap, if any, is the plea/verdict half. This is the basis for
  OQ-5.
- **No case-type gating.** `buildAllocationDecision(...)`,
  `pcfdlrm-refdata/.../ModeOfTrialRefDataEnricher.java:31-52` and
  `pcfdlrm-refdata/.../OffenceDataRefDataEnricher.java:104` contain no case-type or channel
  conditional. Case type is used for **validation rule-set selection**
  (`CcProsecutionValidationRuleProvider`, `MaterialValidationRuleProvider`), not for conversion.
- **No existing case-type × allocation-decision coverage.** `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest`
  has five allocation-decision tests (lines 95, 124, 148, 177, 205, 233) — all keyed on mode of
  trial, none on case type, and **none asserting a date**.
  `ReceiveMigratedCaseFileIT.shouldSetAllocationDecisionForDifferentScenario` (line 184) is
  parameterised over three MOT scenarios, not case types.
- **"Postal Charge" is not a `CaseType` name.**
  `pcfdlrm-domain/pcfdlrm-domain-aggregate/src/main/java/uk/gov/moj/cpp/pcfdlrm/validation/CaseType.java`
  declares `CHARGE("C")`, `REQUISITION("Q")`, `SJP("J")`, `SUMMONS("S")`, `OTHER("O")`. (A second,
  unrelated `uk.gov.moj.cpp.pcfdlrm.CaseType` — `SJP, UNKNOWN, CC` — also exists and is not the one
  in play here.) **However**,
  `docs/pipeline/DD-43067-DD-43130-pcfdlrm-schema-enablement/01-requirements.md:270-271` records,
  *"DECIDED 2026-08-14, CONFIRMED 2026-08-17"* with the architect, that LIBRA's initiation-code set
  is `O, C, Q, J, R` where **`Q` is Postal Charge**. So "Postal Charge" is code `Q`, which this
  repo's enum happens to name `REQUISITION`. That is a naming mismatch, not a missing case type —
  **confirmed 2026-09-17, OQ-1 resolved.**

## Constraints

- **Courts contract.** `criminal-court-public-model` is pinned at `${coredomain.version}` = 17.104.4
  (`pom.xml:25,36,79-80`). The courts `allocationDecision` shape is fixed by that version and is
  `additionalProperties: false` — nothing can be added to the emitted object beyond its declared
  fields.
- **Three-layer rule (CLAUDE.md).** Any event/schema change must be reasoned across command side,
  listener and processor. NFR-4 asserts this story needs **none** of them; if design disagrees, the
  descriptor + JSON-schema pair must be updated together or dispatch 500s at runtime.
- **One story, one repo (CLAUDE.md).** N/A here — OQ-4 confirmed no `progression`- or CAAG-side
  work arises from this story, so no second repo/story is needed.
- **Integration tests are not run by `mvn verify`.** FR-8's fixture changes are only exercised via
  `./runIntegrationTests.sh` (needs `CPP_DOCKER_DIR`, Docker, registry auth), so CI evidence for
  AC-7 comes from the `context-validation` pipeline, not the PR build.

## Out of scope

- **Persistence in CP and display on CAAG.** Downstream of the `progression` boundary — confirmed
  (OQ-4, resolved 2026-09-17) existing behaviour for non-migrated cases, applying unchanged once
  the payload is correct, since this story makes no schema change.
- **The plea / verdict half of R5's code-vs-UUID gap**
  (`DD-43067-DD-43130-pcfdlrm-schema-enablement/01-requirements.md:647`). The slug and ACs name
  allocation decision only. Confirmed out of scope (OQ-5, resolved 2026-09-17) — a separate ticket
  will be created for it.
- **Adding a new `allocationDecisionValue` field to either schema.** Ruled out by the schema
  comparison; confirmed (OQ-2, resolved 2026-09-17) the requester means the existing
  `motReasonId`/`motReasonCode`/`motReasonDescription` triple, not a new field.
- **`originatingHearingId`.** Optional on both schemas, not populated today, not named by any AC —
  same disposition as in DD-34568 (left unset).
- **Renaming `CaseType.REQUISITION` to `POSTAL_CHARGE`.** Even if OQ-1 confirms they are the same
  code, a rename is a separate tidy-up with its own blast radius across
  `CcProsecutionValidationRuleProvider` and its tests.
- **Any change to validation rules or rule-set routing.** Allocation decision conversion is not
  gated by validation outcome and FR-4 forbids introducing such a gate.

## Open questions

All resolved (2026-09-17). Ready for stage 2 (design).

1. **OQ-1 — What is "Postal Charge" in this pipeline? RESOLVED (2026-09-17).**
   Confirmed: `CaseType.REQUISITION` (initiation code `Q`), per the architect-confirmed LIBRA code
   set recorded at `DD-43067-DD-43130-pcfdlrm-schema-enablement/01-requirements.md:270-271` (`O`
   Other, `C` Charge, `Q` Postal Charge, `J` Single Justice Notice, `R` Remittance). The code and
   the enum constant name disagree (`Q` = `REQUISITION`, not `POSTAL_CHARGE`), which is a naming
   mismatch, not a missing type — see *Out of scope* on renaming. AC-3 and FR-9 use this mapping.
2. **OQ-2 — What does "allocation decision value" mean, and is today's mapping already right?
   RESOLVED (2026-09-17).**
   Confirmed: "allocation decision value" is the `motReasonId`/`motReasonCode`/`motReasonDescription`
   triple, already populated correctly by `buildAllocationDecision(...)`. No defect identified. FR-3
   is discharged by evidence — the only code change required by this story is adding
   `allocationDecisionDate` (FR-1).
3. **OQ-3 — Straight-through or derived date? RESOLVED (2026-09-17).**
   Confirmed: `allocationDecisionDate` maps 1:1 from
   `offence.getAllocationDecision().getAllocationDecisionDate()`, with no fallback or derivation of
   the kind `convictionDate` has at converter lines 148–153. When absent, it is simply omitted from
   the emitted object. FR-6 and FR-1 reflect this.
4. **OQ-4 — Does this story stop at the `progression` payload? RESOLVED (2026-09-17).**
   Confirmed **yes**: this story stops at the `progression` payload, and no extra
   `progression`/CAAG work is needed, because this story introduces no schema change (NFR-4).
   `progression` and CAAG's existing handling of `allocationDecisionDate` for non-migrated cases
   applies unchanged. The *Scope boundary* section is updated to reflect this as confirmed, not
   proposed.
5. **OQ-5 — Is the plea/verdict half of R5 in scope? RESOLVED (2026-09-17).**
   Confirmed **out of scope**: a separate ticket will be created for the plea/verdict half of R5
   (`DD-43067-DD-43130-.../01-requirements.md:647`). This story's slug and ACs cover allocation
   decision only, and the evidence above shows allocation decision already **has** a resolver — so
   R5 is partially stale and should be corrected (split into its allocation-decision part, now
   covered by this story, and its plea/verdict part, covered by the new ticket) rather than
   silently closed by this story.

## Risks and notes

- **The fixture trap.** FR-8 is the one place this story can ship looking green and be wrong. The
  expectation fixtures currently encode the bug; a change that passes CI *without* touching them
  means the date is still being dropped. Any implementation PR that does not diff
  `allocation-with-decision.json` and `allocation-with-indictable-decision.json` should be treated
  as suspect at review.
- **All scoping questions are now resolved, not just the code.** FR-1 is plausibly one line, OQ-2
  confirmed "value" means the existing `motReason*` mapping (no defect, no schema change), and OQ-4
  confirmed this story stops at the `progression` payload with no cross-repo work required. There
  is no further scoping risk left to resolve before design.
- **The fix is not LIBRA-specific.** Unlike DD-34568, nothing here is gated on
  `migrationSourceSystemName`, so this also fixes the drop for XHIBIT. That is a side benefit, not
  a scope expansion — but it does mean XHIBIT regression evidence is required, which is why the
  existing IT fixtures live under `json/xhibit/`.
- **`CaseType` ambiguity is a live hazard.** Two enums named `CaseType` exist in this codebase
  (`uk.gov.moj.cpp.pcfdlrm.CaseType` = `SJP, UNKNOWN, CC`, and
  `uk.gov.moj.cpp.pcfdlrm.validation.CaseType` = the five initiation codes). Any design or test
  discussion of "case type" must say which.

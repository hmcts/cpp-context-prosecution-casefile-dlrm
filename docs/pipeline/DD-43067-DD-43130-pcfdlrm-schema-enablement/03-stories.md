# Stories — LIBRA enabler: PCFDLRM schema enablement

> Stage 3 artefact. Source: [`02-design.md`](./02-design.md).
>
> Design decisions that stage 3 must follow — the schema seams and their three failure modes, the
> ADR-002 dispatch shape, the FR9 all-or-nothing rule, the test layout — live in
> [`02-design.md`](./02-design.md). They are not repeated here; a second copy would drift.
>
> **Settled 2026-08-17 (architect) — there is no `X`.** It was a transcription error for `R`
> (Remittance). LIBRA's set is **`O, C, Q, J, R`**, `R` arrives as `R`, and no translation layer is
> needed. **FR12b stands but its answer is open** — PCF turns out to exercise `R` on the fallback
> rather than special-casing it, so T2 carries a decision (see AC-T2-2).

## Stories

Every story below is a task-level slice of the single Jira story
[DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) (epic
[DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler), one per task in
[`02-design.md` § Tasks](./02-design.md#tasks). As with DD-43099, no separate Jira *story* exists per
task — T1–T5 are sub-tasks under DD-43130, one per PR.

| Task | Story below | Owns | Depends on | Jira sub-task |
|---|---|---|---|---|
| T1 | Schemas — declare Group B, raise the close-out ticket | `pcfdlrm-domain-value-schema` | — (gates T4) | *to raise* |
| T2 | Validation dispatch — source-system map, `R`'s rule set, delete the dead selector | `pcfdlrm-domain-aggregate` | — | *to raise* |
| T3 | Aggregate guards — fix `:368`, guard `isXhibit` | `pcfdlrm-domain-aggregate` | T2 | *to raise* |
| T4 | Converters — officer block, `convictionDate`, previous convictions | `pcfdlrm-event-processor` | T1 | *to raise* |
| T5 | Tolerance tests + fixture reconciliation | `pcfdlrm-domain-aggregate`, fixtures | T3 | *to raise* |
| T6 | Integration — one representative LIBRA journey | `pcfdlrm-integration-test` | T1 to author, T1–T4 to pass | *to raise* |

```text
T1 ──────────────► T4 ──────┐
T2 ──► T3 ──► T5 ───────────┴──► T6
```

**T1 first** — it unblocks DD-43081 and produces the POJOs T4 needs. **T2, T3 and T5 share
`pcfdlrm-domain-aggregate` and cannot run in parallel.** T4 runs alongside the T2→T3→T5 chain. **T6
lands last**, because no earlier task can make the journey green on its own.

**Unit and component tests ship with their task** on the DD-43099 DSL, through
`pcfdlrm-test-support` (`FixtureLoader`, `WholePayloadMatcher`), with source system as a scenario
parameter via the existing `SourceSystem` record. No LIBRA-specific test class; no `if` on source
system inside a test. **The integration journey is T6** — see its background for why it cannot be a
line in someone else's definition of done.

---

### T1 — Schemas: declare the Group B field set

| | |
|---|---|
| Linked Jira story | [DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) (epic [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067)) |
| Owns | `pcfdlrm-domain/pcfdlrm-domain-value-schema` |
| Depends on | Nothing. **Gates T4** |
| PRs | 1 |

#### User story
As a **service owner migrating magistrates' court cases from LIBRA**,
I want **PCFDLRM's schemas to declare the Group B field set stagingDLRM will send**,
so that **a LIBRA submission carrying an officer is not rejected outright, and the four new field
values are not silently dropped on the way in**.

#### Background
Five edits across five files, with **three different failure modes** that need three different tests
([`02-design.md` § Schema changes](./02-design.md#schema-changes-file-by-file)):
`migrated/migrated-case-details.json` is `additionalProperties: false`, so an undeclared
`officerInCase` is a **terminal 4xx**; `migrated-offence.json` and `migrated-defendant.json` are
open, so undeclared fields are **silently dropped**; `contact-details.json` is closed **and shared**
(`$ref`'d from `personal-information.json`), so adding `fax` **widens every parent** that reaches it.

`pcf-policeOfficerInCase.json` is already authored and currently referenced by nothing — the work is
references and generated POJOs, not new schema design.

FR19 rides here because DD-43081's Group B converter mapping is compile-blocked until this releases,
and its impact CSV hard-fails on stale claims the moment DD-43130 merges — so the close-out ticket
must exist before then, not after.

#### Acceptance criteria
- [ ] AC-T1-1 (traces to AC1, FR1): Given a migrated case file carrying a complete `officerInCase` block, when PCFDLRM processes it, then the block is accepted, every field deserializes, and none is silently dropped.
- [ ] AC-T1-2 (traces to AC2, FR1): Given the same payload validated against `migrated-case-details.json` **without** the `officerInCase` declaration, then it is rejected — a test that pins *why* FR1 exists. Delete it only with the reason recorded.
- [ ] AC-T1-3 (traces to FR2): Given `pcf-policeOfficerInCase.json`, when reviewed, then it declares `policeWorkerReferenceNumber` and `policeWorkerLocationCode`. Both are `required` by core (FR9), so without them T4 could never take its send branch.
- [ ] AC-T1-4 (traces to FR3): Given `contact-details.json` gains `fax`, when every parent reaching it is validated, then all still accept their existing payloads — reviewed as a shared-schema change, not an officer-local one.
- [ ] AC-T1-5 (traces to FR4, FR5): Given a payload carrying `convictionDate` and `numPreviousConvictions`, when deserialized, then both survive to the domain event. `numPreviousConvictions` is declared **`integer`**, matching core's `Defendant.numberOfPreviousConvictionsCited`.
- [ ] AC-T1-6 (traces to FR7, NFR1): Given every XHIBIT scenario from DD-43099, when the suites run, then all pass unchanged and no XHIBIT fixture is edited. No `maxLength`, `pattern`, `minimum`, `maximum`, `type` or `required` on any existing field is touched.
- [ ] AC-T1-7 (traces to FR8): Given `pcfdlrm.command.receive-migrated-case-file.json` and `pcfdlrm.receive-migrated-case-file.json`, when inspected, then both still `$ref` `migrated-case-details.json` rather than restating it, so all five changes propagate to the inbound gate with no edit. **Re-confirm, do not assume** — DD-43081 F6 found the sibling repo's equivalent file does diverge.
- [ ] AC-T1-8 (traces to AC14, FR19): Given this task completes, then the stagingDLRM close-out ticket (Group B converter mapping + impact-CSV refresh) exists and is linked as blocked by DD-43130.

#### Out of scope for this story
- The officer→Progression mapping and the `numberOfPreviousConvictionsCited` rename — T4.
- Reconciling the 24 fixtures carrying `numPreviousConvictions` — T5 (FR17).
- The FR2a officer-shape decision. If canonical nests (recommended), this task's schema is already
  correct; if it stays flat, the reshaping cost lands in T4, not here.

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes with no hand-edits to generated sources.
- [ ] ITs pass via `./runIntegrationTests.sh`.
- [ ] The generated POJO tree for the officer block is inspected and recorded in the PR — T4's estimate depends on it.

---

### T2 — Validation dispatch: source-system map, Remittance, and the dead selector

| | |
|---|---|
| Linked Jira story | [DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) |
| Owns | `pcfdlrm-domain/pcfdlrm-domain-aggregate` |
| Depends on | Nothing. **Blocks T3, T5** (same module) |
| PRs | 1 |

#### User story
As a **service owner**,
I want **LIBRA cases routed to a deliberately chosen rule set rather than a fallback**,
so that **LIBRA is not validated by Crown Court rules nobody selected, and a future change cannot
silently move it back**.

#### Background
Rule sets key on the raw `initiationCode`, and an unmapped code **does not fail** — it falls through.
**LIBRA's five codes are `O, C, Q, J, R`** *(settled 2026-08-17 — the workbook's `X` is a
transcription error for `R`; there is no separate LIBRA code and no translation layer)*. `O`/`C`/`Q`
are map hits; `J` deliberately takes the SJP sets at both levels; **`R` has no defendant-level entry
and this task decides whether it needs one.**

**FR12b is a decision, not a given.** Two options, set out in full in
[`02-design.md` § Validation dispatch](./02-design.md#validation-dispatch):
**Option A (recommended)** — leave `R` on the `DEFAULT_DEFENDANT_RULE_SET` fall-through, matching
PCF, which has no `R` anywhere in its maps and *exercises* that fall-through on its primary
happy-path IT with both dates supplied. Zero code. **Option B** — map `R` alongside `O`, the original
recommendation; if chosen, `defendantValidationMapDlrm = defendantValidationMapSpi` (`:291`) is a
reference alias of an `ImmutableMap`, so it must be redeclared as a separate map or the **SPI channel
changes too**, and the value is the composed triple `COMMON + SPI + OTHER_DEFENDANT_RULE_SET`, not
the bare set. **Get the answer before writing the map** — see the open item below.

Three case rules — `ReceiptType`, `SendingCourt`, `ReceivingCourt` — exist in PCFDLRM but **not in
PCF**, and `ReceiptTypeValidationRule`'s allowed values are Crown Court concepts that fail on null.
They are scoped to XHIBIT rather than deleted, so NFR1 holds.

Dispatch is a **source-system-keyed map**, per ADR-002, which names this class as precedent — not a
conditional. Existing constants and `caseValidationMap` are untouched, so XHIBIT resolves to exactly
today's list. See [`02-design.md` § Validation dispatch](./02-design.md#validation-dispatch) for the
sketch and for why the map-miss fallback is a chosen default here rather than ADR-002's "programming
error".

#### Acceptance criteria
- [ ] AC-T2-1 (traces to AC6, FR12): Given each of LIBRA's **five** `initiationCode` values — `O`, `C`, `Q`, `J`, `R` — when a LIBRA case is validated, then the rule sets applied are the ones FR12(3) and FR12b chose, asserted explicitly per code.
- [ ] AC-T2-2 (traces to AC6, FR12b) *(revised 2026-08-17 — the assertion is now the deliverable, whichever option is chosen)*: Given a LIBRA case with `initiationCode: "R"`, when validated, then the rule set applied is the one FR12b chose, **asserted explicitly by name** — `DEFAULT_DEFENDANT_RULE_SET` under Option A, or `COMMON + SPI + OTHER_DEFENDANT_RULE_SET` under Option B. The point is that `R`'s routing is pinned and cannot change unnoticed, not which option wins.
- [ ] AC-T2-2a (traces to AC6a): Given this task's suites and fixtures, when reviewed, then **no fixture carries `initiationCode: "X"`**. `X` is not a CPP code and not a LIBRA one either; a fixture would encode the workbook's transcription error into the suite.
- [ ] AC-T2-3 (traces to AC6, FR12(3)): Given a LIBRA case with `initiationCode: "J"`, when validated, then `SJP_CASE_RULE_SET` and `SPI_DEFENDANT_RULE_SET_FOR_INITIATION_CODE` apply. **This assertion is the deliverable**, so a later change cannot quietly move `J` back onto the common path (FR13).
- [ ] AC-T2-4 (traces to FR12, NFR1): Given an XHIBIT case with any initiation code, when validated, then the rule set is byte-identical to `04c0b2d1` behaviour — including `ReceiptType`, `SendingCourt` and `ReceivingCourt`, which stay XHIBIT-only.
- [ ] AC-T2-5 (traces to FR12(5)): Given the provider, when reviewed, then source-system selection is a **map lookup**, there is no `if`/`switch` on source system in a shared path, and no rule class is source-system aware.
- [ ] AC-T2-6 (traces to FR12(5)): Given a payload whose `migrationSourceSystemName` is absent or unrecognised, when validated, then it resolves to the LIBRA/PCF-shaped set — the chosen neutral default — and does **not** throw. `migrationSourceSystemName` is a plain `string` and not `required`, so this is a real input case.
- [ ] AC-T2-7 (traces to AC8, FR14): Given `getDlrmDefendantValidationRules`, when this task completes, then it is **deleted**. It is private, returns `getOrDefault(code, null)`, and is called from nowhere.
- [ ] AC-T2-8 (traces to AC7): Given a case whose defendant-level `initiationCode` differs from the case-level one, when validated, then the documented precedence holds (`ProsecutionCaseFileHelper:93-94`).

#### Out of scope for this story
- The eight `isXhibit` guards — T3.
- `getDefendantValidationRules`' **signature**: unchanged either way. `defendantValidationMapDlrm =
  defendantValidationMapSpi` is already PCF's magistrates-court map and covers `O`, `C`, `Q` and `J`,
  so the only possible defendant-layer edit is FR12b's `R` entry — and under Option A there is none.
- Renaming `ARREST_DATE_IN_FUTURE`, which is raised for a *missing* arrest date (design F2).

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes; ITs pass via `./runIntegrationTests.sh`.
- [ ] Every XHIBIT scenario from DD-43099 passes unchanged, no XHIBIT fixture edited (NFR1).
- [ ] `getDlrmDefendantValidationRules` no longer exists.

---

### T3 — Aggregate guards: fix the lossy one, guard the NPE

| | |
|---|---|
| Linked Jira story | [DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) |
| Owns | `pcfdlrm-domain/pcfdlrm-domain-aggregate` |
| Depends on | T2 (same module). **Blocks T5** |
| PRs | 1 |

#### User story
As a **service owner**,
I want **a LIBRA case with no materials to reach Progression**,
so that **migrated LIBRA cases are not silently dropped with no event, no outcome and no error**.

#### Background
`isXhibit` has **eight** call sites. Seven degrade quietly — LIBRA gets no warning events and no
rejection branch. **`MigratedCaseFileAggregate:368` is lossy**: with no materials and a non-XHIBIT
source, no `MigratedCaseFileReceived` is emitted at all, so nothing reaches
`MigratedCaseReceivedProcessor` and the case produces no outcome. stagingDLRM's harness carries a
`fixeddatenomaterial` case, so the shape is real.

Only `:368` is in scope. The other seven are an **accepted, stated limitation** for this first stab —
a LIBRA case with case-, hearing- or offence-level problems will proceed to Progression carrying
them, silently — and get their own follow-up story.

`isXhibit` at `:525-526` also calls `.getMigrationSourceSystemName().equals(XHIBIT)` unguarded on a
field that is not `required`, so `migrationSourceSystem: {}` NPEs. Fixed while the method is open.

#### Acceptance criteria
- [ ] AC-T3-1 (traces to AC9a, FR12a): Given a **LIBRA case with no materials**, when PCFDLRM processes it, then `MigratedCaseFileReceived` is emitted and the case reaches Progression. *(Fails today.)*
- [ ] AC-T3-2 (traces to NFR1): Given an **XHIBIT** case with no materials, when processed, then behaviour is unchanged from `04c0b2d1`.
- [ ] AC-T3-3 (traces to FR12a): Given a payload carrying `migrationSourceSystem: {}` — valid per schema, since `migrationSourceSystemName` is not `required` — when processed, then it does not NPE.
- [ ] AC-T3-4 (traces to FR12a): Given the seven remaining `isXhibit` guards, when this task completes, then they are **unchanged**, and the limitation is recorded in the PR description and the follow-up ticket is raised.

#### Out of scope for this story
- The other seven guards (`:221`, `:282`, `:313`, `:423`, `:433`, `:554`, `ProsecutionCaseFileHelper:118`) — deliberately deferred; raise the follow-up story here.
- Any change to what the no-materials branch *emits* beyond removing the source-system condition.

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes; ITs pass via `./runIntegrationTests.sh`.
- [ ] A LIBRA no-materials fixture exists and would fail without the `:368` change — **prove it bites**.
- [ ] Follow-up story raised for the remaining seven guards, linked to DD-43130.

---

### T4 — Converters: carry the officer block and two fields to Progression

| | |
|---|---|
| Linked Jira story | [DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) |
| Owns | `pcfdlrm-event/pcfdlrm-event-processor` |
| Depends on | T1 (generated POJOs). Runs in parallel with T2→T3→T5 |
| PRs | 1 |

#### User story
As a **service owner migrating from LIBRA**,
I want **the officer block, `convictionDate` and the previous-conviction count carried into the
Progression payload**,
so that **the Group B fields are not write-only, and a migrated case arrives in the live pipeline
with the data LIBRA held**.

#### Background
The outbound seam is `MigratedCaseReceivedProcessor:51-57`; the reachability root is
**`courtReferral.json`**, not `apiProsecutionCase.json`. All three targets exist in core at
`17.104.4` — `ProsecutionCase.policeOfficerInCase`, `Defendant.numberOfPreviousConvictionsCited`
(`Integer`), `Offence.convictionDate` (`String`).

The officer is **all-or-nothing** because core marks four fields `required` on
`policeOfficerInCase.json` plus `lastName` and `gender` on `person.json`. LIBRA has no officer
gender, so `gender` is set to `NOT_KNOWN` — the enum member that means exactly that, already used for
defendants at `ProsecutionCaseFileHelper:270,280`. **It is the only synthesised value in this
story**; see [`01-requirements.md` FR9a](./01-requirements.md) for the rejected alternatives.

PCF declares the officer block and never reads it. That is PCF being silent, not opposed — the
"follow PCF" rule does not reach this decision, so it was made on the merits (design F5).

#### Acceptance criteria
- [ ] AC-T4-1 (traces to AC4, FR9): Given a LIBRA case whose officer block is missing any of `surname`, `policeOfficerRank`, `policeWorkerReferenceNumber` or `policeWorkerLocationCode`, when the Progression payload is built, then the block is **omitted entirely** — never sent partial.
- [ ] AC-T4-2 (traces to AC4a, FR9a): Given a complete officer block, when the payload is built, then `personDetails.gender` is `NOT_KNOWN`, and `personDetails.address` is present only when `address1` is.
- [ ] AC-T4-3 (traces to AC5, FR9): Given a complete officer block, when `progression.initiate-court-proceedings` is built, then the officer appears under its Progression names, asserted as a **whole payload**, not a field-presence spot check.
- [ ] AC-T4-4 (traces to AC3, AC5, FR10): Given a case carrying `convictionDate` and `numPreviousConvictions`, when the payload is built, then `Offence.convictionDate` and `Defendant.numberOfPreviousConvictionsCited` are populated — the rename applied at this seam and nowhere else.
- [ ] AC-T4-5 (traces to NFR1): Given every XHIBIT scenario from DD-43099, when the suites run, then all pass unchanged and no XHIBIT fixture is edited. The three additions are pure additions — no existing mapping changes.
- [ ] AC-T4-6 (traces to AC15): Given a LIBRA case with a complete officer block, when the payload is validated against core's schemas, then it satisfies `policeOfficerInCase.json`, `person.json` and `address.json` `required` lists.

#### Out of scope for this story
- Declaring the fields — T1. This task assumes the generated POJOs exist.
- `organisationTelephoneNumber` — FR6 is closed; LIBRA 0.13 does not carry it.
- Whether the officer converter lives in `MigratedCaseToProsecutionCaseConverter` (205 lines) or its
  own class — a build-time call, noted in the design.

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes; ITs pass via `./runIntegrationTests.sh`.
- [ ] A whole-payload assertion exists at the `receive-migrated-case-file` boundary — the only assertion that catches an ADR-003 name mismatch (FR18).
- [ ] Every XHIBIT scenario from DD-43099 passes unchanged (NFR1).

---

### T5 — Tolerance tests and fixture reconciliation

| | |
|---|---|
| Linked Jira story | [DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) |
| Owns | `pcfdlrm-domain/pcfdlrm-domain-aggregate`, fixtures across modules |
| Depends on | T3 (same module) |
| PRs | 1 |

#### User story
As a **service owner**,
I want **the five fields DD-43081 makes optional proven tolerable, and the fixtures reconciled
against the schemas**,
so that **a LIBRA case omitting them is handled deliberately rather than by luck, and no fixture
claims a field the contract does not have**.

#### Background

> **Scope corrected 2026-08-14 — this task shrank.** It was written around a `prosecutorOffenceId`
> defect, on the strength of DD-43081's `02-design.md` schema table. That table is **stale**. Their
> `01-requirements.md` FR1 relaxes **five** constraints, not ten, and states that `durationMinutes`
> and `prosecutorOffenceId` "are now required on **both** sides in LIBRA 0.13, so LIBRA satisfies them
> and no relaxation is needed" — corroborated by the impact CSV, which marks both `already_flowing`.
> **The unmatched-offence rule is no longer part of this task.** What remains is confirming
> tolerance and reconciling fixtures.

DD-43081 relaxes five constraints, all on `caseDetails`, covering six fields.
[`01-requirements.md` FR16](./01-requirements.md) audited them and **all five are already clean**:
`dateReceived` is never read in Java; `retrialIndicator` is a straight copy;
`dateOfCommittal`/`dateOfSending` are explicitly `Optional.ofNullable(…).orElse(null)`;
`receiptType` and `receivingCourt` are moot once T2 scopes their rules to XHIBIT.

**So this task ships no production change for FR16** — its deliverable is the confirming tests.
Absence becomes newly reachable for all five and no existing test exercises it, which is exactly the
gap DD-43099 exists to close.

FR17 is the other half: `numPreviousConvictions` appears in **24** fixtures while being declared in no
schema in either repo, so those fixtures cannot be used as evidence of what the contract allows. Once
T1 declares it for real, they are reconciled against the schema rather than assumed correct.

The `prosecutorOffenceId` matching gap is **real but unreachable** — canonical requires the field and
LIBRA supplies it, so nothing arriving at PCFDLRM can trigger it. It is recorded in FR16 as a
robustness gap for the deferred `isXhibit` guards story. **G3 is withdrawn.**

#### Acceptance criteria
- [ ] AC-T5-1 (traces to AC9, FR16): Given a LIBRA case omitting each of the five relaxed fields in turn, when processed, then it completes with no NPE and no silent default — confirming the audit rather than assuming it.
- [ ] AC-T5-2 (traces to AC9, FR16): Given those five scenarios, when reviewed, then each is a **new** test — none existed, because canonical required all five until DD-43081.
- [ ] AC-T5-3 (traces to AC11, FR17): Given this repo's fixtures and RAML examples, when validated against the post-T1 schemas, then they conform, and the `numPreviousConvictions` drift across 24 fixtures is resolved.
- [ ] AC-T5-4 (traces to NFR1): Given every XHIBIT scenario from DD-43099, when the suites run, then all pass unchanged and no XHIBIT fixture is edited.

#### Out of scope for this story
- **The unmatched-offence rule** — removed from scope; the condition is unreachable from LIBRA.
- The `:554` guard, and the other six deferred `isXhibit` guards.
- The workbook-correction list — DD-43081 T6's deliverable, not duplicated here.
- Renaming `ARREST_DATE_IN_FUTURE` (design F2).

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes; ITs pass via `./runIntegrationTests.sh`.
- [ ] No fixture claims a field the schema does not declare.

---

### T6 — Integration: one representative LIBRA journey

| | |
|---|---|
| Linked Jira story | [DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) |
| Owns | `pcfdlrm-integration-test` |
| Depends on | T1 to author the fixture; **T1–T4 to pass** |
| PRs | 1 |

#### User story
As a **service owner migrating from LIBRA**,
I want **one end-to-end LIBRA journey exercised against a deployed PCFDLRM**,
so that **the schema, the rule routing, the no-materials path and the Progression payload are proven
together rather than only in isolation**.

#### Background
FR18 asks for unit/component coverage that is exhaustive and **integration that is one representative
LIBRA journey**, at the depth DD-43099 established.

This is its own task because **no earlier task can make it green**. The journey crosses all four:
the schema declares the fields (T1), the dispatch selects the rule set (T2), `:368` lets a
no-materials case emit (T3), and the converters build the payload (T4). It is also real work rather
than a line in a checklist — `ReceiveMigratedCaseFileIT` carries 11 `@Test` methods plus an
eight-fixture parameterised source, so a LIBRA journey means a new `command-json` fixture and new
assertions.

The ITs are **not** run by `mvn verify`. They need WildFly, Postgres and ActiveMQ up via
`./runIntegrationTests.sh`, with `CPP_DOCKER_DIR` set — so this task's feedback loop is slower than
T1–T5's and should be scheduled with that in mind.

Two things already in place, and one trap:
- `referencedata.get-initiation-types.json` stubs `J, Q, S, C, R, O`, which covers all five of
  LIBRA's codes **including `R`**, so no stub change is needed for the Remittance route.
- Three existing fixtures already carry `migrationSourceSystemName: "LIBRA"` — but they predate this
  work, use `initiationCode: "Q"`, and are not a LIBRA journey in any meaningful sense. **Do not
  treat them as one**; they pass today only because every problem they raise is discarded by the
  `isXhibit` guards.
- Per design **F6**, police-rank reference data is stubbed (`referencedata.get-police-ranks.json`,
  one rank `AXE`) but consumed by nothing. T4 deliberately does not wire it, so the fixture's
  `policeOfficerRank` need not match `AXE` — if an assertion ever seems to demand it, the wiring
  decision has been reversed somewhere and that is the bug.

**T6 is not AC15.** AC15 is the cross-repo journey from a real LIBRA `case.json` through
stagingDLRM to Blob Storage; it needs DD-43086 and a sample that does not exist yet, and is owned by
whichever story lands last.

#### Acceptance criteria
- [ ] AC-T6-1 (traces to FR18): Given a LIBRA `receive-migrated-case-file` command carrying the Group B field set, when submitted against a deployed PCFDLRM, then it is accepted and `MigratedCaseFileReceived` is emitted.
- [ ] AC-T6-2 (traces to AC5, FR18): Given that journey, when the `progression.initiate-court-proceedings` payload is captured, then it is asserted as a **whole payload** — the only assertion that catches an ADR-003 name mismatch, which is invisible to both repos' unit suites.
- [ ] AC-T6-3 (traces to AC1, AC4a): Given the journey's payload carries a complete officer block, then the officer reaches Progression with `personDetails.gender` set to `NOT_KNOWN`.
- [ ] AC-T6-4 (traces to AC9a): Given a LIBRA case **with no materials**, when submitted, then it completes and reaches Progression — the `:368` fix proven end to end, not only in the aggregate suite.
- [ ] AC-T6-5 (traces to AC6, AC6a): Given the journey uses one of LIBRA's **five** codes — `O`, `C`, `Q`, `J`, `R` — then the fixture states it explicitly and the chosen rule set is observable in the outcome. **Never `X`** — it is not a real code.
- [ ] AC-T6-6 (traces to AC10, NFR1): Given the full IT suite, when run via `./runIntegrationTests.sh`, then every existing XHIBIT IT passes unchanged and no XHIBIT fixture is edited.

#### Out of scope for this story
- The cross-repo journey (AC15) — needs DD-43086 and a real LIBRA sample.
- `cpp-apitests`, as for DD-43099.
- Exhaustive LIBRA IT coverage. **One** representative journey; breadth lives in the unit and
  component suites per FR18.
- Re-purposing the three incidental `"LIBRA"` fixtures — leave them alone, or fold them into T5's
  FR17 reconciliation if they turn out to violate the post-T1 schemas.

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `./runIntegrationTests.sh` passes locally and in CI.
- [ ] No material increase in IT runtime.
- [ ] The whole-payload assertion is a real comparison, not a field-presence spot check.

---

## AC coverage map

Every acceptance criterion in [`01-requirements.md`](./01-requirements.md), and where it is
discharged. Nothing is dropped; three are deliberately not tasks.

| AC | Where |
|---|---|
| AC1, AC2 | T1; AC1 also proven end-to-end in T6 |
| AC3 | T1 (declared) + T4 (carried) |
| AC3a | **Not a task — G2.** Provable by inspection today; the fix may land in either repo |
| AC4, AC4a, AC5 | T4; AC4a and AC5 also proven end-to-end in T6 |
| AC6 | T2; observable in T6's journey |
| AC6a | T2 (AC-T2-2a), re-checked in T6 (AC-T6-5) — no `X` fixture anywhere |
| AC6b | **Not a task — cross-repo.** Correcting `X`→`R` in DD-43081's `libra-schema-impact.csv` and ADR-003 §5 is DD-43081's edit; carried by the handover note |
| AC7, AC8 | T2 |
| AC9 | T5 |
| AC9a | T3 (aggregate) + T6 (end-to-end) |
| AC10 | **Every task** — the NFR1 line in each task's ACs and definition of done, not a separate task. T6 covers the IT half |
| AC11 | T5 |
| AC12 | **Every task's definition of done** |
| AC13 | **Already delivered** — FR11's answer is recorded in `01-requirements.md` and communicated via the handover note |
| AC14 | T1 (AC-T1-8) |
| AC15 | **Not a task** — joint with DD-43081, owned by whichever lands last; also needs DD-43086 and a real LIBRA sample |

AC10 and AC12 are deliberately not given their own task. A regression phase that runs once at the end
is exactly the failure DD-43099 was built to prevent — both are gates on every PR instead.

## Open before build starts

| # | Item | Owner | Blocks |
|---|---|---|---|
| **G1** *(revised 2026-08-17)* | **Canonical `initiationCode` must widen from `["O"]`** to LIBRA's `O, C, Q, J, R`, with the XHIBIT allowed-values rule kept at `["O"]` and a LIBRA rule added. DD-43081 FR1 currently drops this thread. Plus ADR-003 §5 → `O, C, Q, J, R`, and **correct the `X`** in §5 and in `libra-schema-impact.csv` — it is a transcription error for `R`, so no translation is needed | DD-43081 | Nothing here; **blocks any non-`O` LIBRA case** and so T6's journey |
| **G4** *(new 2026-08-17)* | **FR12b: does `R` stay on the fallback (Option A, PCF parity) or get its own entry (Option B)?** Turns on whether remitted LIBRA cases reliably carry `arrestDate`/`chargeDate` — the contract permits absence, so the schema cannot answer it | Architect / LIBRA extract contact | **T2's map, but not T2's start.** Low urgency: the `isXhibit` guards discard defendant problems for LIBRA until the deferred guards story lands, so a wrong first answer is a one-line correction |
| **G2** | ADR-003 §3 → officer block nests | DD-43081 | Nothing here; sets T4's cost |
| ~~G3~~ | ~~`prosecutorOffenceId`: detect or prevent?~~ **Withdrawn 2026-08-14** — the field is not relaxed and the condition is unreachable from LIBRA (FR16) | — | — |

G1 and G2 are drafted at
[`docs/analysis/dd-43130-to-dd-43081-handover.md`](../../analysis/dd-43130-to-dd-43081-handover.md)
and not yet sent. Neither blocks T1, which can start immediately.

**Not tracked as a task:** AC15 — the joint end-to-end LIBRA journey — is shared with DD-43081 and
owned by whichever story lands last. It also needs DD-43086 and a real LIBRA sample, neither of which
exists yet.

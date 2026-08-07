# Design — LIBRA enabler: PCFDLRM test hardening

> Stage 2 artefact. Source: [`01-requirements.md`](./01-requirements.md).
> Split per the team workflow: **2a** cross-context impact, **2b** inside the service.
> The shared convention this repo builds against is
> [ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md)
> — linked, not restated.
>
> **Revised 2026-08-07 — test-strategy sync from DD-43078**, whose T1/T2 have landed and whose T3–T6
> were refined before implementation. Only test-coverage decisions were brought across, each
> re-verified against *this* repo's working tree rather than assumed from the sibling:
> **(a)** converter assertions move to the `MigratedCaseReceivedProcessor` seam with a real converter
> ([why](#where-the-converter-assertions-belong)), and T8 gains a three-tier scenario ratio;
> **(b)** `MigratedCaseToProsecutionCaseConverter` is `@Mock`ed in that seam test today — finding 5,
> a T8 prerequisite;
> **(c)** `Comparison` drops out of T1 — the sibling removed it before merge (finding 6);
> **(d)** fixture conventions, the fidelity round-trip test, and the dump-then-prove-it-bites
> discipline, in [Notes for stage 3](#notes-for-stage-3-story-writer).
> The handler row was **already right** and is confirmed rather than changed.
> Nothing about DD-43081 changed for this story: it depends on that story's FR13 and FR14a only, and
> the FR7 correction made on 2026-08-07 is stagingDLRM-side.

| | |
|---|---|
| Epic | [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler |
| Story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) — PCFDLRM test hardening |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Production changes | none (see [NFR1](#nfr1-the-one-build-change)) |

---

## 2a — Cross-context impact

**No cross-context impact.** Test, fixture and test-support code only. Nothing changes in any
schema, RAML, event contract, JMS subscription or Progression interaction. The
`public.pcfdlrm.migrated-case-file-processed` event and the `initiatecourtproceedings` payload sent
to Progression are asserted, not altered.

Two things are worth the lead's attention, and both are about how the work is *run*:

**This story is independently deliverable and shares no code with DD-43078.** Different repo,
different branch, separate CI run, no ordering constraint — either story can merge first. The two
were originally scoped as one story spanning both repos; splitting them restores the workflow's
one-story-one-repo rule, which their independence already satisfied.

**One shared contract, single-homed:**
[ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md).
It fixes the scenario-row shape, the whole-payload comparison semantics, the fixture layout and the
source-system parameter mechanism so the two repos do not diverge into two dialects. It lives in
the stagingDLRM repo and is **linked from this repo's PR description, never copied** — a second
copy would drift the moment the decision changed. It must be approved before this story starts
stage 5.

**`cpp-apitests` is out of scope.** The workflow asks for a third test scope when two stories
change a contract that spans them. Neither story changes a contract, and the requester scoped
`cpp-apitests` out explicitly. Recorded here so the omission is a decision, not a gap.

**MbD vs context service does not arise** — this is an existing CQRS/ES context service and no
pattern choice is being made.

---

## 2b — Design inside the service

### The shape of the problem

This repo's suite is not thin — 39 aggregate test methods on real POJOs, 63 domain test classes,
14 IT tests asserting real outbound requests. Three specific properties are missing, and each needs
a different remedy. Naming them separately matters, because "add whole-payload assertions
everywhere" is not the work.

| Failure mode | Where | Remedy |
|---|---|---|
| **Source system is hardcoded, so it cannot vary** | `builder/ObjectBuilder.java:43` → all 39 aggregate tests | Parameterise the builder; make the source system a required scenario input |
| **Asserts a spot check of a real payload** | `MigratedCaseFileHandlerTest` (2 fields), `ReceiveMigratedCaseFileHelper:180` (2 `withJsonPath`), the converters | Keep the structure, swap selective assertions for whole-payload comparison |
| **Asserts passthrough of a value the test itself set** | the ten `is(XHIBIT)` assertions | Delete or replace with assertions that distinguish source-system-dependent behaviour |

The third is the subtle one and the reason FR1 is worded as it is. Lines 373, 663, 701, 804, 1000,
1035, 1074, 1117, 1167 and 1217 each assert that the source system that came out equals the source
system `ObjectBuilder` put in. They would pass unchanged if every XHIBIT-gated branch in the
aggregate were deleted. They look like source-system coverage and are not.

### Shared foundations (per ADR-001)

New test-scoped module `pcfdlrm-test-support` carrying `FixtureLoader`, `WholePayloadMatcher` and
`Comparison`.

Consumers: `pcfdlrm-domain-aggregate`, `pcfdlrm-command-handler`, `pcfdlrm-event-processor`,
`pcfdlrm-integration-test`.

**All three classes are written out in full in ADR-001's appendix**, with the anchored-exclusion,
wildcard-rejection and unused-exclusion changes already applied. T1 is a copy-and-adjust-the-package
job — it needs no access to `cpp-context-results`, and must not substitute a Maven dependency on
`uk.gov.moj.cpp.results:test-utilities` (that artefact drags `results-domain-common`, an unrelated
context's domain module, onto the test classpath). The only new dependency is
`org.skyscreamer:jsonassert` at test scope, version-managed by `maven-common-bom` — this repo
already declares it in `pcfdlrm-integration-test`, so that module can inherit it from the new one.

**No step-sequencing layer** (ADR-001 §3). The aggregate suite looked like the strongest candidate
for one until it was counted:

| Invocation | Count | Kind |
|---|---|---|
| `receiveMigratedCaseFile` | 37 | command |
| `materialAddedPostProcessing` | 2 | command |
| `acceptMigratedCase` | 1 | command |
| `getReceiveMigratedCaseFile`, `getMaterialsAdded`, 4 others | 15 | getter |

**40 command invocations across 39 tests — essentially one command per test.** The suite is
multi-*variant*, not multi-*step*: 39 variations of a single call with different inputs. Eleven
tests make two or more aggregate calls, but for most of them the second is a getter.

So `Scenario`/`StepDef` would serve roughly three test methods at a cost of ~400 lines. Those three
issue their commands as sequential calls with a whole-payload assertion after each; everything else
becomes `@ParameterizedTest` + `@MethodSource` rows, which is what actually delivers FR3 and AC2.

Two things already here are reused rather than rebuilt:

- **`test-utils-core` is already a test dependency across the repo** and runs everit — the same
  validator the framework uses in production — should any schema-level assertion be needed.
- **`ObjectBuilder` / `TestConstants` stay.** They are not deleted; `ObjectBuilder` gains a source
  system parameter and `SOURCE_SYSTEM_XHIBIT` becomes one value a caller may pass rather than the
  baked-in default. This is the smallest change that satisfies FR3/AC3, and it keeps the 39
  existing call sites working through an overload during conversion.

### Component by component

| Component | Now | Design | Scenario rows? |
|---|---|---|---|
| `builder/ObjectBuilder` + `TestConstants` | `:43` hardcodes `SOURCE_SYSTEM_XHIBIT` | Add a source-system parameter; keep a deprecated no-arg overload only for the duration of the conversion, removed before the story closes. **Do this first** — everything else depends on it | — |
| `MigratedCaseFileAggregateTest` (1,659 lines, 39 methods) | real POJOs, asserts emitted-event content, all XHIBIT by construction | **Rows adopted** — 39 scenarios as `@MethodSource` data, which is what this multi-variant suite actually needs. Carries FR5.2 (hearing/defendant matching computed for every case, surfaced only for XHIBIT, `MigratedCaseFileAggregate:526`). Whole-payload assert on appended events. Delete the ten vacuous `is(XHIBIT)` assertions as each block converts | **yes** |
| `MigratedCaseFileHandlerTest` (4 tests, 353 lines) | captures `CaseProcessingArgs`, asserts 2 fields | Keep the captor; assert the captured payload **whole** against a fixture. **Confirmed correct** — DD-43078 arrived at the same answer the hard way (its story asked for the *appended event* instead, which a test with a mocked aggregate cannot observe; corrected as its F21). No source-system parameterisation: the handler is a pass-through | no |
| `ExhibitFiileTypeValidationRuleTest` (222 lines) | references both source systems | FR5.1 — rule fires for XHIBIT **and** explicitly no-ops for non-XHIBIT (`ExhibitFiileTypeValidationRule:66` gates on `XHIBIT.equals(...)`). Both problem codes pinned: `INVALID_FILE_TYPE_FOR_XHIBIT`, `INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION` | rows |
| `ProsecutionCaseFileHelperTest` (259 lines) | | FR5.3 — `applyRuleToDefendantFields()` normalises gender/language/ethnicity after a validation failure, gated at `ProsecutionCaseFileHelper:118` on `"XHIBIT".equals(...)`. Assert the XHIBIT normalisation **and** that a non-XHIBIT source leaves the fields untouched — the latter as a whole-payload equality against the input, per FR5a | rows |
| `CcProsecutionValidationRuleProviderTest` (127 lines) | asserts by `Channel`; every assertion is an `anyMatch` on one rule class | FR6 — assert **set equality** on the rule classes returned by `getCaseValidationRules(initiationCode)` for `SUMMONS`, `REQUISITION`, `SJP` and the default. A set comparison detects a rule leaving a set; `anyMatch` does not | rows |
| `MigratedCaseToProsecutionCaseConverterTest` (403 lines) + 5 sibling converters | field-level | Still the highest-value FR2 target — but **pin it at `MigratedCaseReceivedProcessor`, not on the converter classes**, and with a **real** converter rather than today's `@Mock`. See [Where the converter assertions belong](#where-the-converter-assertions-belong) | **yes** — three tiers, not one-per-converter |
| `MigratedCaseFileProcessedProcessorTest` (83 lines) | thin | Whole-payload assert on `public.pcfdlrm.migrated-case-file-processed` | no |
| `pcfdlrm-integration-test` | 14 tests, 24 fixtures | See below | no |

### Where the converter assertions belong

*Added 2026-08-07, transferred from DD-43078's stage-5 refinement. Verified against this repo's
working tree, not assumed from the sibling.*

**The converter tree is a single-entry hierarchy behind one seam:**

```
MigratedCaseReceivedProcessor                            <- the only production caller
  └─ MigratedCaseToProsecutionCaseConverter
       ├─ ProsecutionCaseFileMigratedDefendantToCCDefendantConverter
       │    ├─ ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter
       │    ├─ ProsecutionMigrationCaseToCCPersonDefendantConverter
       │    └─ ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter
       └─ ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
```

`MigratedCaseReceivedProcessor` is the sole production caller of the root converter, and every
sibling is reached only through it. So **all six converters are drivable from one seam**, and nothing
is lost by asserting there instead of on six separate test classes.

What is gained is that the pins survive DD-43081. A test asserting
`MigratedCaseToProsecutionCaseConverter` directly is coupled to a **class**; one asserting the
`InitiateCourtProceedings` payload the processor emits is coupled to a **contract**. Since the six
converters form a three-level hierarchy with no external callers, that hierarchy can be restructured
freely — and per-class tests would break on a restructure that changed no payload. This argument is
*stronger* here than in stagingDLRM, where a single converter sits behind a single processor; here it
is six classes deep.

**Blocker to fix first — the same defect as stagingDLRM's F3.**
`MigratedCaseReceivedProcessorTest:44` declares `@Mock private MigratedCaseToProsecutionCaseConverter`.
With the converter mocked, the payload under assertion is empty, so the seam assertion means nothing
until a real converter is wired in. This is not recorded anywhere in this design and is a
**prerequisite of T8**, not a consequence of it.

### Scenario ratio for T8 — three tiers, not one per converter

The metric is not tests-per-converter. It is: **for each way this mapping can break, does exactly one
test fail, naming the field?** Six converters × per-class tests gives coupling without composition
coverage; one maximal test gives a diff that could be anywhere.

| Tier | Scenarios | What it catches |
|---|---|---|
| **1 — maximal** | 1 | every optional field populated, every collection ≥2 elements. Omission and transposition across the whole tree |
| **2 — minimal** | 1 | only required fields. Every null-guard, and **whether an absent input yields an omitted field or an explicit `null`** |
| **3 — branch rows** | ~5–7 | what tiers 1 and 2 cannot reach: empty vs null collections, person vs legal-entity defendant (two distinct sibling converters), hearing present vs absent |

**Tier 2 is load-bearing here, more than in stagingDLRM.** `WholePayloadMatcher` is STRICT and STRICT
is symmetric — an *extra* field fails. DD-43081 FR12 adds 18 tier-1/2 fields **at PCFDLRM's nesting
level**, and FR13 adds 6 more plus the orphaned `pcf-policeOfficerInCase.json`. If the converters
**omit** absent fields, those additions never appear in an XHIBIT payload and every pin written here
survives DD-43081 untouched. If they emit explicit `null`s, every pin breaks on a change that altered
no XHIBIT behaviour — and the suite stops being trusted at exactly the moment it is supposed to prove
XHIBIT is unharmed. Tier 2 is the test that pins which of those happens.

**Two ACs that make T8 verifiable rather than reviewable:**

1. **Coverage is measured, not eyeballed.** With the six per-converter test classes excluded from the
   run, jacoco reports 100% method coverage across the converter package. Excluding them is what makes
   the number mean *"the seam tests reach the mapping"* rather than *"something reached it"*. A one-off
   measurement at the gate, not a build gate, since both sets contribute in a normal run.
2. **The dumped fixture is cross-checked against Progression's schema before it is committed.** The
   expected payload has to be dumped from the implementation — it is far too large to hand-write — but
   nothing has ever verified those mappings field by field, so an uncritical dump pins today's bugs as
   "correct" and asserts them forever. **Watch the two traps DD-43081's design notes record:** the root
   is **`courtReferral.json`**, *not* `apiProsecutionCase.json`, and matching a field by name alone
   picks whichever schema sorts first (this is how officer `forename`/`surname` once resolved to
   `judicialRole.json`). Read from the sibling checkout, not `.m2`.

**Do not delete the six per-converter test classes.** DD-43078 reached the same conclusion and then
reversed it at the story owner's call: they carry no FR2 weight once the seam is pinned, cost nothing
as smoke tests, and removing them is not this story's business. Leave them untouched.

### Sequencing the aggregate conversion

`MigratedCaseFileAggregateTest` is the largest single piece of work and the one most likely to
stall review (requirements *Risks*). It converts in **three PRs, not one**:

1. `ObjectBuilder` parameterised + test-support module in place, existing tests still passing unchanged.
2. The FR5.2 hearing/defendant-matching scenarios converted to rows, with the XHIBIT and
   non-XHIBIT paths both asserted — the highest-value slice, reviewable on its own.
3. The remaining scenario blocks, converted in whatever order suits, each removing its share of the
   ten vacuous assertions.

A reviewer can hold PR 2 in their head. They cannot hold a 1,659-line rewrite.

### Integration tests (FR9)

Three command fixtures carry `"migrationSourceSystemName": "LIBRA"`:
`pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json`,
`-multiple-hearing-wc-migrated-case-file.json`,
`pcfdlrm.command.receive-with-no-hearing-migrated-case-file.json`.

**Decision: convert, do not duplicate.** Re-point all three at XHIBIT. Duplicating would leave
LIBRA journeys running at IT level, which FR9 and AC7 forbid, and would add Docker runtime for
coverage that belongs at unit level.

Unlike stagingDLRM — where the base IT journey has never run as XHIBIT and re-pointing it is a
behavioural canary — the base journey here is **already XHIBIT**, so this is expected to be a
routine change. If any of the three changes result on re-pointing, that is a real behavioural
difference between the source systems and is raised immediately rather than absorbed.

`prosecutorOffenceId` values containing the string `LIBRA` are left alone.

Boundary payloads asserted whole per FR2: the `initiatecourtproceedings` request currently checked
with two `withJsonPath` values at `ReceiveMigratedCaseFileHelper:180` becomes a whole-payload
comparison. The `withRequestBody(containing(...))` WireMock filter that selects the request stays —
it is request *selection*, not assertion.

Journeys kept, per FR9 — case file received and processed through to the public event; material
addition. No new journeys.

### FR7 and FR8 — deferred within the story, not dropped

Both depend on DD-43081 decisions that have not landed: FR14a's resolution for `informant` /
`writtenChargePostingDate` / `prosecutorCosts`, and FR13 for the five `exists_mandatory` officer
fields. The scenario rows are authored as soon as those land. If they have not landed when this story
closes, the rows carry to DD-43081 and that is recorded at closure rather than the story waiting —
the requirements state DD-43099 has no external blocker, and waiting would create one.

---

## FR → design traceability

| Req | Where it is satisfied |
|---|---|
| FR1, FR3 | `ObjectBuilder` parameterised; ADR-001 §4 makes the source system a mandatory step input |
| FR2 | ADR-001 §1 — STRICT compare, anchored enumerated exclusions, unused exclusion fails |
| FR4 | Scenario rows throughout; step chaining deferred per ADR-001 §3 (~3 multi-command tests) |
| FR5, FR5a | 3 suites, each asserting XHIBIT and non-XHIBIT with a concrete positive |
| FR6 | `CcProsecutionValidationRuleProviderTest` — set equality per `initiationCode` |
| FR7, FR8 | Deferred within story pending DD-43081 FR14a / FR13 |
| FR9 | 3 fixtures converted; journeys unchanged; boundary payload asserted whole |
| NFR1 | No `src/main` change in any module — see below |
| NFR2 | Whole-payload comparison is in-memory JSONassert; ITs gain no journeys |

---

## NFR1: the one build change

One new Maven module, `pcfdlrm-test-support`, consumed only at `<scope>test</scope>`. No `src/main`
file in any deployable module changes, no WAR gains a dependency, no runtime artefact is affected.
AC8's "no production source file has changed" holds literally.

It is nonetheless a reactor change and the gate should approve it consciously. ADR-001's options
table records the fallback if rejected: duplicate the support classes into each module's
`src/test/java` and re-scope FR3/AC2 from per-repo to per-module.

---

## Findings raised during design

1. **Ten aggregate assertions are vacuous.** They assert passthrough of a value `ObjectBuilder`
   set, and would survive deletion of every XHIBIT-gated branch. They read as source-system
   coverage and provide none.
2. **`pcfdlrm-query` has no `src/test` directory at all**, and
   `pcfdlrm-viewstore-persistence/src/test` contains no Java sources. Both are real gaps. Neither
   is LIBRA-adjacent, so both are explicitly out of scope here — but they should be raised as
   separate tickets rather than left unrecorded.
3. **The Progression boundary assertion is better than reported but still a spot check.** The
   earlier draft brief described it as "already asserts the outbound payload". It captures the real
   request and asserts two `withJsonPath` values — better than a stub-hit check, well short of FR2.
4. **This repo is in better shape than stagingDLRM.** stagingDLRM's aggregate suite asserts
   essentially nothing (deep-stub mocks); this one asserts real emitted-event content across 39
   methods. The work here is narrower and the sizing should reflect that.

*Findings 5–7 added 2026-08-07 from DD-43078's stage-5 experience, each verified against this repo.*

5. **`MigratedCaseToProsecutionCaseConverter` is `@Mock`ed in its own seam test**
   (`MigratedCaseReceivedProcessorTest:44`) — the same defect stagingDLRM recorded as F3, and
   unrecorded here until now. Any whole-payload assertion on the emitted `InitiateCourtProceedings`
   is vacuous until a real converter replaces it. **Prerequisite of T8.**
6. **`Comparison` should be dropped from T1's scope.** T1 lists `FixtureLoader`,
   `WholePayloadMatcher` **and `Comparison`**. stagingDLRM built T1 first and **dropped `Comparison`
   before merge** — no call site needed it, exclusion lists read better as literals at the assertion,
   and the no-default-exclusions principle it existed to enforce is already structural (an exclusion
   matching no path fails the test). ADR-001's appendix was updated accordingly. Building it here
   would add a class the sibling repo removed for cause.
7. **Do not hand-write expected fixtures; do not trust a dump either.** Generate by dumping the real
   payload, save it, delete the generator — then **prove the assertion bites** by removing one field
   and watching it fail. stagingDLRM verified T1 and T2 this way. The dump-then-verify order matters
   most for the converters, where the mapping has never been checked field by field (see the
   Progression cross-check above).

## Open questions for the gate

1. **Does the new test-support module clear NFR1?** Design says yes with the reading above. The
   fallback is in ADR-001.
2. **FR7/FR8 timing.** If DD-43081 FR13/FR14a have not landed by stage 5, are those rows carried to
   DD-43081, or does this story wait? Design assumes carried.
3. **Three PRs for the aggregate conversion** — acceptable, or should it land as one? Design
   recommends three on reviewability grounds; it does affect the CI count.

## Notes for stage 3 (story-writer)

| # | Task | Depends on |
|---|---|---|
| T1 | `pcfdlrm-test-support` module — `FixtureLoader`, `WholePayloadMatcher` (**not `Comparison`** — finding 6) | ADR-001 approved |
| T2 | Parameterise `ObjectBuilder`; source system becomes a caller argument | T1 |
| T3 | Aggregate suite PR 1 — scenario rows in place, existing tests green | T2 |
| T4 | Aggregate suite PR 2 — FR5.2 hearing/defendant matching, both paths | T3 |
| T5 | Aggregate suite PR 3 — remaining blocks; vacuous assertions removed | T4 |
| T6 | FR5.1 + FR5.3 rule/helper pins | T2 |
| T7 | FR6 rule-set selection as set equality | T1 |
| T8 | Converters + processed-event: whole payloads **at the `MigratedCaseReceivedProcessor` seam**, three tiers; un-mock the converter first (finding 5) | T1 |
| T9 | Command handler: whole-payload assert on the captured `CaseProcessingArgs` | T1 |
| T10 | ITs: convert 3 LIBRA fixtures, whole boundary payload (FR9) | T1 |

T2 gates most of the story and is small — schedule it first and do not let it queue behind T1's
module scaffolding.

**Fixture conventions, settled by DD-43078's T1/T2 and not re-litigated here.** ADR-001 §5's original
one-directory-per-scenario form (`json/<scenario>/input.json`) was **superseded** during that story;
the shipped convention is one directory per test class with the scenario in the *filename* —
`json/<component-slug>/<document>-<scenario>.json`. Filenames like `input.json` grep and tab-title
badly once five are open. A source-system **sub-directory** is justified only where the two systems
produce genuinely different documents rather than one document with a substituted value; elsewhere
`{{SOURCE_SYSTEM}}` carries it (FR3). In this repo that likely means no source-system directories at
all — the divergence here is in *rules that fire*, not in payload shape.

**One assertion to add that DD-43078 added late.** A single fixture-fidelity test per fixture-driven
suite: `JSON → POJO → JSON` round-trips unchanged. Anything that does not survive that round trip is
invisible to every whole-payload assertion built on it — a field absent from the generated POJO is
silently dropped and the assertion still passes. Cheap, and it is what makes the rest of the pins
mean what they claim.

**Assert what the component reads *and* what it carries.** stagingDLRM's aggregate turned out never
to call `getMaterials()`, so two of its scenarios differed only in a stub nothing consumed and
collapsed into one. Worth a pass over the 39 methods here for the same shape: a scenario that varies
an input the aggregate never reads is one scenario, not two — unless the *emitted event* differs,
which is the thing actually worth a row.

**AC6 stays a review step, not a task.** Deliberately dropping a field from the Progression payload
and demonstrating a failing test is the only real proof FR2 landed; folding it into a task turns it
into a checkbox. It is demonstrated once at the stage 6 gate and not committed.

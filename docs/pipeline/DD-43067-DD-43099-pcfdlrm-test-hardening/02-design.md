# Design — LIBRA enabler: PCFDLRM test hardening

> Stage 2 artefact. Source: [`01-requirements.md`](./01-requirements.md).
> Split per the team workflow: **2a** cross-context impact, **2b** inside the service.
> The shared convention this repo builds against is
> [ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md)
> — linked, not restated.

| | |
|---|---|
| Epic | [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler |
| Story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) — PCFDLRM test hardening |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Production changes | none (see [R4](#r4-the-one-build-change)) |

---

## 2a — Cross-context impact

**None.** Test, fixture and test-support code only. Nothing changes in any schema, RAML, event
contract, JMS subscription or Progression interaction. The
`public.pcfdlrm.migrated-case-file-processed` event and the `initiatecourtproceedings` payload sent
to Progression are asserted, not altered.

Two things are worth the lead's attention, both about how the work is *run*:

**This story is independently deliverable and shares no code with DD-43078.** Different repo,
different branch, separate CI run, no ordering constraint — either can merge first.

**One shared contract, single-homed:**
[ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md)
fixes the scenario-row shape, the whole-payload comparison semantics, the fixture layout and the
source-system parameter mechanism, so the two repos do not diverge into two dialects. It lives in the
stagingDLRM repo and is **linked from this repo's PR description, never copied** — a second copy
would drift the moment the decision changed. It must be approved before stage 5.

`cpp-apitests` is out of scope by the requester's explicit decision, recorded so the omission is a
decision rather than a gap. MbD vs context service does not arise — this is an existing CQRS/ES
context service and no pattern choice is being made.

---

## 2b — Design inside the service

### The shape of the problem

The suite is not thin: 39 aggregate test methods, 63 test classes in the domain module, 12 IT methods
asserting real outbound requests. Six specific properties are missing, and each needs a different
remedy — "add whole-payload assertions everywhere" is not the work.

| Failure mode | Where | Remedy |
|---|---|---|
| **Inputs are deep-stub mocks, so no payload can be serialised** | `MigratedCaseFileAggregateTest:109-128` — `caseDetails`, `prosecution`, `prosecutionWithReferenceData` | Replace with real POJOs. **Prerequisite for everything else in T2** |
| Source system is hardcoded, so it cannot vary | `ObjectBuilder:42-43` → all 39 aggregate tests | Parameterise the builder; make the source system a required scenario input |
| Asserts a spot check of a real payload | `MigratedCaseFileHandlerTest` (2 fields), `ReceiveMigratedCaseFileHelper:194-195,219`, the converters | Keep the structure, swap selective assertions for whole-payload comparison |
| Asserts passthrough of a value the test itself set | the ten `is(XHIBIT)` assertions | Delete, or replace with assertions that distinguish source-system-dependent behaviour |
| Asserts aggregate state instead of emitted events | ~25 getter assertions; 13 invocations discard the returned `Stream<Object>` | Assert the returned stream: length, event type in order, whole payload. Never a getter |
| **Branches R3 requires have no scenario at all** | 4 of the 11 rejection reasons sit behind the `:221` / `:433` gates and are never tested | **Write them** — R3a is not satisfiable by conversion alone. See [Coverage](#coverage-what-r3-forces-what-defers) |

Three of these are worth expanding, because each changes what T2 costs.

**The deep-stub mocks are the blocker.** `ObjectBuilder` builds its case file with
`CaseDetails.caseDetails().withValuesFrom(caseDetails)`, copying getter-by-getter off a
`RETURNS_DEEP_STUBS` mock. Nested POJO getters therefore return *further mocks*, not data — so the
emitted events cannot be serialised to JSON, and no whole-payload assertion is possible until the
inputs are real. This is the single largest item in T2 and it is invisible from the test's surface,
which reads as though it already uses real objects.

**The expected value is an ordered event list, not a case-file payload.** No production code calls an
aggregate getter, and two of the four are package-private, reachable only because the test shares the
aggregate's package. Two further facts, verified against the framework and the aggregate, make
state-based assertion not merely weaker but incapable:

- `Aggregate.apply(Stream)` is `map(this::apply).collect(toList()).stream()` — **eager**. The 13
  stream-discarding tests do still apply their events; they simply cannot observe them. The argument
  rests on observability, not on events going unapplied.
- `MigratedCaseValidatedWithWarnings` and `MigratedCaseNotFoundInAutomation` are emitted by
  production but have **no arm in `apply()` and no field or getter on the aggregate**. No
  state-based assertion can see them under any circumstances.

**The ten `is(XHIBIT)` assertions would pass unchanged if every XHIBIT-gated branch were deleted.**
They look like source-system coverage and are not.

### Foundations

**New test-scoped module `pcfdlrm-test-support`** carrying `FixtureLoader` and `WholePayloadMatcher`.
Consumers: `pcfdlrm-domain-aggregate`, `pcfdlrm-command-handler`, `pcfdlrm-event-processor`,
`pcfdlrm-integration-test`.

Both classes are written out in full in ADR-001's appendix, with the anchored-exclusion,
wildcard-rejection and unused-exclusion changes applied — this is a copy-and-adjust-the-package job.
Two constraints:

- **Do not build `Comparison`.** ADR-001 originally listed it; stagingDLRM built the module first and
  dropped it before merge — no call site needed it, exclusion lists read better as literals at the
  assertion, and the no-default-exclusions principle it enforced is already structural.
- **Do not substitute a Maven dependency on `uk.gov.moj.cpp.results:test-utilities`.** It drags
  `results-domain-common`, an unrelated context's domain module, onto the test classpath.

The only new dependency is `org.skyscreamer:jsonassert` at test scope, version-managed by
`maven-common-bom`. `pcfdlrm-integration-test` already declares it and can inherit it from the new
module.

**No step-sequencing layer** (ADR-001 §3). The aggregate suite looked like the strongest candidate
for one until it was counted: **40 command invocations across 39 tests** — essentially one command
per test. The suite is multi-*variant*, not multi-*step*. `Scenario`/`StepDef` would serve roughly
three test methods at a cost of ~400 lines; everything else becomes `@ParameterizedTest` +
`@MethodSource` rows, which is what actually delivers R2 and AC5.

**`ObjectBuilder` and `TestConstants` stay.** `ObjectBuilder` gains source-system arguments and
`SOURCE_SYSTEM_XHIBIT` becomes one value a caller may pass rather than the baked-in default. Prefer a
small `SourceSystem` value type over a 9th positional `String` — the method already takes six
consecutive `String`s and a transposition would compile silently. **Skip the deprecated overload:**
37 call sites sit in one file, and a defaulting overload contradicts R2.

### Component 1 — Handler + aggregate

| Suite | Now | Design |
|---|---|---|
| `MigratedCaseFileAggregateTest` (1,659 lines, 39 methods) | deep-stub mock inputs; asserts emitted-event content via getters; all XHIBIT by construction | Real POJO inputs, then scenarios as `@MethodSource` rows — **43, not 39** (four R3a scenarios do not exist today). **Assert the stream returned by the aggregate method** — length, event type in order, each payload whole. Carries R3a (all seven `isXhibit()` gates). Delete the ten vacuous `is(XHIBIT)` and ~25 getter assertions as each block converts |
| `MigratedCaseFileHandlerTest` (4 tests, 353 lines) | captures `CaseProcessingArgs`, asserts 2 fields | Keep the captor; assert `captured.getReceiveMigratedCaseFile()` **whole** against a fixture. **`CaseProcessingArgs` is not wholly serialisable** — it also carries `ReferenceDataQueryService` and the enricher `Instance` lists, so `getSections()` and `getDocumentMetadataReferenceDataList()` stay the value assertions they already are. No source-system parameterisation — the handler is a pass-through. Note this test has its own non-determinism (`:77-79, 218, 229`), so R2 applies here too. DD-43078 arrived at the same answer the hard way: its story asked for the *appended event*, which a test with a mocked aggregate cannot observe |
| `ExhibitFiileTypeValidationRuleTest` (222 lines) | references both source systems | R3b — rule fires for XHIBIT, both problem codes pinned whole. **XHIBIT path only**; existing non-XHIBIT references left as they are |
| `ProsecutionCaseFileHelperTest` (259 lines) | | R3c — assert the normalised gender/language/ethnicity values whole for XHIBIT. **XHIBIT path only** |
| `CcProsecutionValidationRuleProviderTest` (127 lines) | asserts by `Channel`; every assertion is an `anyMatch` on one rule class | R3d — assert **set equality** on the rule classes returned by `getCaseValidationRules(initiationCode)` for `SUMMONS`, `REQUISITION`, `SJP` and the default. A set comparison detects a rule leaving a set; `anyMatch` does not |

The 54 simple `*RuleTest` classes stay as they are. The row DSL is a means to R1 and R2, not a target.

**The assertion shape.** Every aggregate scenario runs through **one shared assertion block**, so R1's
no-getters rule is enforced by the harness rather than by review — there is no per-test place to
reach for a getter. A scenario row carries the source system, the builder inputs, and its expected
value as an **ordered list of (event type, fixture) pairs**. The block asserts stream length first,
then type and whole payload per position: an aggregate emitting an extra event should fail on count,
which names the problem, rather than on a payload diff, which does not. Serialisation is the
framework `ObjectToJsonObjectConverter` into `WholePayloadMatcher`.

Worked code in [`03-stories.md`](./03-stories.md#aggregate-scenario-harness-t2).

**Sequencing.** `MigratedCaseFileAggregateTest` is the largest single piece of work and the one most
likely to stall review. Story owner's call (2026-08-07): **three PRs, not four.**

1. **De-mock the inputs.** Replace `caseDetails`, `prosecution` and `prosecutionWithReferenceData`
   with real POJOs; keep mocks only for the genuine collaborators (`ReferenceDataQueryService`, the
   three enrichers). Run it as a spike first — what the emitted events actually contain once the
   mocks are gone decides the fixture shape for everything after it. No assertions change.
2. **Row harness, proven on the `:368` gate.** The harness needs a first slice to prove itself, and
   `:368` — whether `MigratedCaseFileReceived` is emitted at all — is the single highest-value pin in
   the story. Making it the slice is what merges the two PRs without losing the reason `:368` was
   split out: it still gets a small, focused review, just not a separate CI run.
3. **The remaining six gates and the other scenario blocks**, each removing its share of the ten
   vacuous `is(XHIBIT)` and ~25 getter assertions, and writing the four missing R3a scenarios.

Take the fail-fast paths first within PR 3. Nine of the eleven `MigratedCaseFileProcessed` emissions
are early returns (`:192, 203, 214, 232, 244, 256, 271, 279`) producing a single-event stream
distinguished only by `description` — the cheapest rows in the suite, and the right place to settle
the fixture convention before the main path at `:378`.

A reviewer can hold any one of these in their head. They cannot hold a 1,659-line rewrite.

### Coverage — what R3 forces, what defers

The general coverage audit is a **follow-up ticket** (story owner, 2026-08-07). But R3 is not
satisfiable by conversion alone, and the split is not where it looks. Measured against the aggregate:

| Untested today | Gate | This story? |
|---|---|---|
| `Invalid Prosecuting Authority` | `isXhibit()` at `:221` | **Yes** — R3a |
| `INVALID_OFFENCE_CODE` | `isXhibit()` at `:433` | **Yes** — R3a |
| `MISSING_OR_INVALID_PLEA_DATE` | `isXhibit()` at `:433` | **Yes** — R3a |
| `MISSING_OR_INVALID_VERDICT_DATE` | `isXhibit()` at `:433` | **Yes** — R3a |
| `COURT_RECORD_SHEET_NOT_PDF` aggregate fail-fast | not gated (`:184`); the *rule* is | Rule only, via R3b — aggregate path defers |
| `COURT_RECORD_SHEET_FILE_TYPE_INVALID` aggregate fail-fast | not gated (`:195`); the *rule* is | Rule only, via R3b — aggregate path defers |
| `MaterialAddedPendingProcess` — never named anywhere | not gated | Defers |
| `acceptMigratedCase` ×1, `materialAddedPostProcessing` ×2 across 39 tests | not gated | Defers |

**So T2 writes four scenarios that do not exist today**, on top of converting the 39. All four are
fail-fast single-event paths and cheap once the harness exists — they are missing because nobody
wrote them, not because they are hard. Sizing T2 as "convert 39 tests" is wrong on two counts: this,
and the de-mock.

Everything in the *Defers* rows is ungated, which is why deferring it does not weaken any pin this
story places. Raise the follow-up when the story closes so these measurements are not lost.

### Component 2 — Event processor + converters

**Assert at the `MigratedCaseReceivedProcessor` seam, with a real converter.** The converter tree is
a single-entry hierarchy:

```
MigratedCaseReceivedProcessor                            <- the only production caller
  └─ MigratedCaseToProsecutionCaseConverter
       ├─ ProsecutionCaseFileMigratedDefendantToCCDefendantConverter
       │    ├─ ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter
       │    ├─ ProsecutionMigrationCaseToCCPersonDefendantConverter
       │    └─ ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter
       └─ ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter
```

All six converters are drivable from that one seam, so nothing is lost by asserting there instead of
on six separate test classes — and the pins survive DD-43081. A test asserting
`MigratedCaseToProsecutionCaseConverter` directly is coupled to a **class**; one asserting the
`InitiateCourtProceedings` payload the processor emits is coupled to a **contract**. The hierarchy has
no external callers and can be restructured freely; per-class tests would break on a restructure that
changed no payload.

**Prerequisite — wiring a real tree, which is more than un-mocking one field.**
`MigratedCaseReceivedProcessorTest` mocks the converter (`:44`), `objectToJsonObjectConverter`,
`envelopeHelper` **and** the `MigratedCaseFileReceived` input, then asserts
`verify(sender).sendAsAdmin(envelope)` against a `JsonValue.NULL` payload. Nothing real flows through
it today.

Building the real tree is a wiring problem: all six converters use **private `@Inject` fields**, and
`@InjectMocks` populates only one level of a three-level hierarchy. Two of them —
`MigratedCaseToProsecutionCaseConverter:50-55` and
`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter:75` — also need a
`ReferenceDataQueryService`.

So T3 needs a small test-side factory using `FieldUtils.writeField(target, name, value, true)`
(commons-lang3 is already on the classpath) that assembles the six converters and injects a stubbed
`ReferenceDataQueryService`. Put it in `pcfdlrm-test-support` beside `FixtureLoader` — the ITs will
not need it, but that is where the next person will look. A CDI container (Weld) would also work and
is not worth the runtime.

**Assertion point: capture at `envelopeHelper`, not at `sender`.** Capturing the envelope passed into
`withMetadataInPayloadForEnvelope` gets the converted payload *and* the renamed
`progression.initiate-court-proceedings` metadata, while leaving whatever `envelopeHelper` itself
does out of scope — it has its own test. Wire a real `ObjectToJsonObjectConverter`; keep `sender`,
`envelopeHelper` and `PcfMigratedCaseReceivedCounter` mocked.

Worked code in [`03-stories.md`](./03-stories.md#converter-seam-harness-t3).

The input `MigratedCaseFileReceived` is deserialised from a fixture rather than hand-built. That is
what makes the round-trip fidelity test load-bearing here specifically: a field absent from the
generated POJO is dropped silently on the way in, and the STRICT comparison on the way out still
passes.

**Scenario ratio — three tiers, not one per converter.** The metric is not tests-per-converter; it is
*for each way this mapping can break, does exactly one test fail, naming the field?*

| Tier | Scenarios | What it catches |
|---|---|---|
| 1 — maximal | 1 | every optional field populated, every collection ≥2 elements. Omission and transposition across the whole tree |
| 2 — minimal | 1 | only required fields. Every null-guard, and **whether an absent input yields an omitted field or an explicit `null`** |
| 3 — branch rows | ~5–7 | what tiers 1 and 2 cannot reach: empty vs null collections, person vs legal-entity defendant, hearing present vs absent |

**Tier 2 is load-bearing.** `WholePayloadMatcher` is STRICT, and STRICT is symmetric — an *extra*
field fails. DD-43081 adds 18 fields at PCFDLRM's nesting level, plus 6 more and the orphaned
`pcf-policeOfficerInCase.json`. If the converters **omit** absent fields, those additions never appear
in an XHIBIT payload and every pin written here survives untouched. If they emit explicit `null`s,
every pin breaks on a change that altered no XHIBIT behaviour — and the suite stops being trusted at
exactly the moment it is supposed to prove XHIBIT is unharmed. Tier 2 pins which of those happens.

`MigratedCaseFileProcessedProcessorTest` (83 lines, thin) gains a whole-payload assertion on
`public.pcfdlrm.migrated-case-file-processed`.

**Two checks that make this task verifiable rather than reviewable:**

1. **Coverage is measured.** With the six per-converter test classes excluded from the run, jacoco
   reports 100% method coverage across the converter package. Excluding them is what makes the number
   mean *"the seam tests reach the mapping"* rather than *"something reached it"*. A one-off
   measurement at the gate, not a build gate.
2. **The dumped fixture is cross-checked against Progression's schema before it is committed.** The
   expected payload must be dumped from the implementation — it is far too large to hand-write — but
   nothing has ever verified these mappings field by field, so an uncritical dump pins today's bugs as
   "correct" and asserts them forever. Two traps DD-43081's design notes record: the root is
   **`courtReferral.json`**, *not* `apiProsecutionCase.json`; and matching a field by name alone picks
   whichever schema sorts first — this is how officer `forename`/`surname` once resolved to
   `judicialRole.json`. Read from the sibling checkout, not `.m2`.

**Do not delete the six per-converter test classes.** They carry no R1 weight once the seam is pinned,
cost nothing as smoke tests, and removing them is not this story's business. DD-43078 reached the same
conclusion and reversed it at the story owner's call.

### Component 3 — Integration tests

Three command fixtures carry `"migrationSourceSystemName": "LIBRA"`, so those journeys have **no
XHIBIT baseline at all** — precisely the gap this story exists to close:

- `pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json`
- `pcfdlrm.command.receive-multiple-hearing-wc-migrated-case-file.json`
- `pcfdlrm.command.receive-with-no-hearing-migrated-case-file.json`

**Decision: convert, do not duplicate.** Re-point all three at XHIBIT. Duplicating would leave LIBRA
journeys running at IT level, which AC6 forbids, and would add Docker runtime for coverage that
belongs at unit level.

Unlike stagingDLRM — where the base IT journey has never run as XHIBIT and re-pointing it is a
behavioural canary — the base journey here is **already XHIBIT**, so this is expected to be routine.
If any of the three changes result on re-pointing, that is a real behavioural difference between the
source systems and is raised immediately rather than absorbed.

`prosecutorOffenceId` values containing the string `LIBRA` are left alone — renaming them is churn
with no coverage effect.

The `initiatecourtproceedings` request, checked today with three `withJsonPath` values across two
assertions, becomes a whole-payload comparison. The `withRequestBody(containing(...))` WireMock filter
that selects the request stays — it is request *selection*, not assertion.

Journeys kept: case file received and processed through to the public event; material addition. **No
new journeys**, and no unit scenarios ported down. Once the ITs are open the temptation is to port
scenarios into them — little gain, Docker runtime cost on every build.

---

## Tasks

| # | Task | Depends on |
|---|---|---|
| **T1** | **Foundation** — `pcfdlrm-test-support` module (`FixtureLoader`, `WholePayloadMatcher`); `ObjectBuilder` de-randomised and both source-system fields parameterised. 3 files, 37 call sites | ADR-001 approved |
| **T2** | **Handler + aggregate** — de-mock the aggregate test inputs, then rows asserting the returned event stream (**3 PRs**, see *Sequencing*), **plus the four R3a scenarios that do not exist today**; handler whole-payload assert on the captured `ReceiveMigratedCaseFile`; R3b/R3c/R3d pins | T1 |
| **T3** | **Event processor + converters** — build a real converter tree via a `FieldUtils` factory, capture at `envelopeHelper`, whole payloads in three tiers; processed-event assert | T1 |
| **T4** | **ITs** — convert the 3 LIBRA fixtures, whole boundary payload | T1 |

T1 gates everything and is small — schedule it first. T2, T3 and T4 are independent of each other and
can run in parallel.

**AC7 (the deliberate-break check) stays a review step, not a task.** Dropping a field from the
Progression payload and demonstrating a failing test is the only real proof R1 landed; folding it into
a task turns it into a checkbox. Demonstrated once at the stage 6 gate, not committed.

### Conventions for stage 3

**Fixture layout**, settled by DD-43078 and not re-litigated: one directory per test class with the
scenario in the *filename* — `json/<component-slug>/<document>-<scenario>.json`. This **supersedes**
ADR-001 §5's original `json/<scenario>/input.json` form; filenames like `input.json` grep and
tab-title badly once five are open. A source-system sub-directory is justified only where the two
systems produce genuinely different documents rather than one document with a substituted value —
in this repo that likely means none at all, since the divergence here is in *which rules fire*, not
in payload shape.

**Do not hand-write expected fixtures; do not trust a dump either.** Generate by dumping the real
payload, save it, delete the generator — then **prove the assertion bites** by removing one field and
watching it fail. This matters most for the converters, where the mapping has never been checked
field by field.

**Add one fixture-fidelity test per fixture-driven suite:** `JSON → POJO → JSON` round-trips
unchanged. Anything that does not survive that round trip is invisible to every whole-payload
assertion built on it — a field absent from the generated POJO is silently dropped and the assertion
still passes. Cheap, and it is what makes the rest of the pins mean what they claim.

**Assert what the component reads *and* what it carries.** stagingDLRM's aggregate turned out never
to call `getMaterials()`, so two of its scenarios differed only in a stub nothing consumed and
collapsed into one. Worth a pass over the 39 methods here: a scenario that varies an input the
aggregate never reads is one scenario, not two — unless the *emitted event* differs, which is the
thing actually worth a row.

---

## R4: the one build change

One new Maven module, `pcfdlrm-test-support`, consumed only at `<scope>test</scope>`. No `src/main`
file in any deployable module changes, no WAR gains a dependency, no runtime artefact is affected.
AC8's "no production source file has changed" holds literally.

It is nonetheless a reactor change and the gate should approve it consciously. ADR-001's options table
records the fallback if rejected: duplicate the support classes into each module's `src/test/java` and
re-scope R2/AC5 from per-repo to per-module.

## Requirement → design traceability

| Req | Where it is satisfied |
|---|---|
| R1 | ADR-001 §1 semantics via `WholePayloadMatcher` (T1); emitted-stream expected value fixed by T2's harness; AC2 greppable at review |
| R2 | `ObjectBuilder` de-randomised and parameterised (T1); AC3's repeat-build check verifies determinism |
| R3a | T2 — all seven `isXhibit()` gates, XHIBIT path |
| R3b, R3c | T2 — rule and helper pins |
| R3d | T2 — `CcProsecutionValidationRuleProviderTest` set equality per `initiationCode` |
| R4 | Test-scoped module only; whole-payload comparison is in-memory JSONassert; ITs gain no journeys |

## Gate decisions

All three questions settled by the story owner on 2026-08-07. No open questions remain; stage 3 can
proceed.

1. **Does the new test-support module clear R4 (no production change)?** **Yes.** `pcfdlrm-test-support`
   is approved as a reactor addition at `<scope>test</scope>`. ADR-001's duplicate-per-module fallback
   is not needed.
2. **How many PRs for the aggregate conversion?** **Three, not four.** See *Sequencing* — the `:368`
   pin becomes the slice that proves the row harness rather than a PR of its own, so it keeps its
   focused review without a separate CI run.
3. **Is the general coverage audit in scope?** **Follow-up ticket.** This story writes only the
   scenarios R3 requires. Note the consequence recorded in
   [Coverage](#coverage-what-r3-forces-what-defers): that is still **four new scenarios**, because
   `Invalid Prosecuting Authority` and the three offence/plea/verdict reasons sit behind the `:221`
   and `:433` gates. Raise the follow-up at story closure so the measurements are not lost.

---

## Appendix — evidence

Verified against the working tree on 2026-08-07. Cited here once so the prose above stays readable
and these can be re-checked with a grep rather than trusted.

| Claim | Site |
|---|---|
| Seven `isXhibit()` call sites | `MigratedCaseFileAggregate:221, 282, 368, 423, 433, 554, 562` (defined at `:525`) |
| `:368` gates event emission | `MigratedCaseFileAggregate:368` — inside the block adding `MigratedCaseFileReceived` to the stream |
| Exhibit rule XHIBIT gate | `ExhibitFiileTypeValidationRule:66`; problem codes raised at `:45`, `:51` |
| Helper XHIBIT gate | `ProsecutionCaseFileHelper:118` |
| Source system hardcoded | `ObjectBuilder:42` (`SOURCE_SYSTEM_XHIBIT_IDENDIFIER`), `:43` (`SOURCE_SYSTEM_XHIBIT`) — note the spelling |
| Five non-deterministic values | `ObjectBuilder:39` (`dateOfSending`), `:56` (plea id), `:78` (`dateOfBirth`), `:89` (offence id), `:112` (`submissionId`) |
| Ten vacuous `is(XHIBIT)` assertions | `MigratedCaseFileAggregateTest:373, 663, 701, 804, 1000, 1035, 1074, 1117, 1167, 1217` |
| ~25 getter assertions | `MigratedCaseFileAggregateTest` — grep the four getter names |
| 13 invocations discard the stream | `MigratedCaseFileAggregateTest:173, 202, 207, 211, 219, 247, 266, 1236, 1414, 1471, 1521, 1594, 1637` |
| 39 test methods, 40 command invocations | `MigratedCaseFileAggregateTest` — 33 `@Test` + 6 `@ParameterizedTest` |
| Aggregate test inputs are deep-stub mocks | `MigratedCaseFileAggregateTest:109-128` — `@Mock(answer = RETURNS_DEEP_STUBS)` on `caseDetails`, `prosecution`, `prosecutionWithReferenceData` and four others; `ObjectBuilder:36` copies off them via `withValuesFrom` |
| `apply(Stream)` is eager | `framework-api-domain` `Aggregate.apply(Stream)` = `map(this::apply).collect(toList()).stream()` |
| Two emitted types have no state representation | `MigratedCaseValidatedWithWarnings`, `MigratedCaseNotFoundInAutomation` — emitted via `builder.add(...)`, absent from the `apply()` match arms at `MigratedCaseFileAggregate:133-143`, no field or getter |
| 6 of 11 rejection reasons unnamed in the test | production reasons from `withDescription(...)`; grep each against `MigratedCaseFileAggregateTest` |
| `MaterialAddedPendingProcess` never named in the test | 0 occurrences in `MigratedCaseFileAggregateTest` |
| Entry-point invocation counts | `receiveMigratedCaseFile` 37, `materialAddedPostProcessing` 2, `acceptMigratedCase` 1 |
| Converter mocked in its own seam test | `MigratedCaseReceivedProcessorTest:44`; the tree is field-injected at `MigratedCaseToProsecutionCaseConverter:50-55`, `ProsecutionCaseFileMigratedDefendantToCCDefendantConverter:44-51`, `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter:75` |
| IT boundary spot checks | `ReceiveMigratedCaseFileHelper:194-195, 219` |
| Three LIBRA IT fixtures | `command-json/…receive-multiple-hearing-migrated-case-file.json:143`, `…-multiple-hearing-wc-…json:145`, `…receive-with-no-hearing-…json:107` |
| Two further unpinned source-system branches | `PleaDataRefDataEnricher:67`, `VerdictDataRefDataEnricher:66` — `XHIBIT ? CROWN : MAGISTRATES`. `migrationSourceSystemName` is declared `"type": "string"` with **no enum** in `pcfdlrm-domain-value-schema/…/migrated/migrated-migrationSourceSystem.json`, so any unrecognised value silently takes the `MAGISTRATES` branch rather than failing schema validation |
| Suite sizes | 63 test classes in `pcfdlrm-domain-aggregate` (54 are `*RuleTest`); 6 converter test classes; 12 IT methods across 2 IT classes; 24 IT command fixtures |

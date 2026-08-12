# Stories — LIBRA enabler: PCFDLRM test hardening

> Stage 3 artefact. Source: [`02-design.md`](./02-design.md).
>
> Conventions that stage 3 must follow — fixture layout, the dump-then-prove-it-bites discipline, the
> round-trip fidelity test — are in
> [`02-design.md` § Conventions for stage 3](./02-design.md#conventions-for-stage-3). They are not
> repeated here; a second copy would drift.

## Stories

Every story below is a task-level slice of the single Jira story
[DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) (epic
[DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler), one per task in
[`02-design.md` § Tasks](./02-design.md#tasks). No separate Jira *story* exists per task — T1–T4 are
sub-tasks/PRs under DD-43099. Sub-task tickets are created under DD-43099, one per PR:

| Task | Story below | PRs | Depends on | Jira sub-task |
|---|---|---|---|---|
| T1 | Foundation — `pcfdlrm-test-support` + deterministic `ObjectBuilder` | 1 | ADR-001 approved | [DD-43121](https://tools.hmcts.net/jira/browse/DD-43121) |
| T2 | Handler + aggregate — de-mock, row harness, R3a–d pins | **3** (fixed, not negotiable — see T2) | T1 | PR1 [DD-43122](https://tools.hmcts.net/jira/browse/DD-43122), PR2 [DD-43123](https://tools.hmcts.net/jira/browse/DD-43123), PR3 [DD-43124](https://tools.hmcts.net/jira/browse/DD-43124) |
| T3 | Event processor + converters — real converter tree, seam assertions | 1 | T1 | [DD-43125](https://tools.hmcts.net/jira/browse/DD-43125) |
| T4 | ITs — XHIBIT-only fixtures, whole-payload boundary | 1 | T1 | [DD-43126](https://tools.hmcts.net/jira/browse/DD-43126) |

T1 gates T2/T3/T4 and should be scheduled first; T2, T3 and T4 are independent of each other and can
run in parallel once T1 lands.

---

### T1 — Foundation: `pcfdlrm-test-support` module and a deterministic `ObjectBuilder`

| | |
|---|---|
| Linked Jira story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) (epic [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067)) |
| Jira sub-task | [DD-43121](https://tools.hmcts.net/jira/browse/DD-43121) |
| Depends on | ADR-001 approved. Gates T2, T3, T4 |
| PRs | 1 |

#### User story
As a **PCFDLRM test engineer**,
I want **a shared, deterministic test-support module (`FixtureLoader`, `WholePayloadMatcher`) and an
`ObjectBuilder` that takes both source-system fields from the caller and never mints a
non-deterministic value**,
so that **every other hardening task in this story can build whole-payload fixtures against stable,
XHIBIT-explicit test data, and adding a source system later is a data change, not a code change**.

#### Background
`ObjectBuilder` currently hardcodes `SOURCE_SYSTEM_XHIBIT_IDENDIFIER`/`SOURCE_SYSTEM_XHIBIT`
(`ObjectBuilder:42-43`) and mints five non-deterministic values per call — two relative dates and
three random UUIDs (`:39, 56, 78, 89, 112`) — two of which (`dateOfSending`, `dateOfBirth`) are read
by validation rules. `FixtureLoader` and `WholePayloadMatcher` are a copy-and-adjust of ADR-001's
appendix classes (anchored-exclusion, wildcard-rejection and unused-exclusion changes applied), placed
in a new test-scoped Maven module so `pcfdlrm-domain-aggregate`, `pcfdlrm-command-handler`,
`pcfdlrm-event-processor` and `pcfdlrm-integration-test` share one implementation rather than four
copies drifting. Per the story owner's 2026-08-07 gate decision, the module is approved as a reactor
addition; ADR-001's duplicate-per-module fallback is not needed.

#### Acceptance criteria
- [ ] AC-T1-1 (traces to R4, AC8): Given the reactor pom(s), when `pcfdlrm-test-support` is added, then it is declared at `<scope>test</scope>` in every consuming module and no `src/main` file in any module changes.
- [ ] AC-T1-2 (traces to ADR-001 §2, R4): Given `pcfdlrm-test-support`'s contents, when reviewed, then it contains `FixtureLoader` and `WholePayloadMatcher` only — no `Comparison` class, and no Maven dependency on `uk.gov.moj.cpp.results:test-utilities`.
- [ ] AC-T1-3 (traces to AC3): Given two calls to `ObjectBuilder` with identical arguments, when the outputs are compared, then they are byte-identical — no relative date, no random UUID in either.
- [ ] AC-T1-4 (traces to R2, AC3): Given a call to `ObjectBuilder`, when the source system is supplied, then **both** `migrationSourceSystemName` and `migrationSourceSystemCaseIdentifier` are taken from the caller (via a small `SourceSystem` value type, not two more positional `String`s), with **no** defaulting overload.
- [ ] AC-T1-5 (traces to R2, AC3): Given the 37 existing `ObjectBuilder` call sites, when migrated to the new signature, then every one states `migrationSourceSystemName` explicitly (baseline `XHIBIT`) — none relies on `TestConstants.SOURCE_SYSTEM_XHIBIT` as an implicit default, and none calls a deprecated overload.
- [ ] AC-T1-6 (traces to AC8): Given the new module and the changed call sites, when `mvn clean install` runs, then it passes with no `src/main` file changed outside the reactor pom edits that add the new module.

#### Out of scope for this story
- Building a `Comparison` helper class (listed in ADR-001, dropped per the design decision at [`02-design.md` § Foundations](./02-design.md#foundations)).
- A step-sequencing (`Scenario`/`StepDef`) layer — the aggregate suite is multi-variant, not multi-step (design decision, not revisited here).
- Rewriting any *assertion* at the 37 call sites — T1 only makes them compile against the new signature with explicit XHIBIT values; converting the surrounding test's assertions is T2/T3/T4's job.

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes.
- [ ] ITs pass via `./runIntegrationTests.sh` with no material runtime increase (this task does not touch IT fixtures, but the IT baseline must stay green before T2–T4 build on it).
- [ ] No `src/main` file changed in any module, except the reactor pom(s) gaining the new `pcfdlrm-test-support` module.
- [ ] `FixtureLoader`/`WholePayloadMatcher` implement ADR-001 §1's JSONassert STRICT / anchored-enumerated-exclusion / wildcard-rejection semantics — confirmed against the ADR, not re-derived.
- [ ] Jira sub-task updated with test evidence once created.

#### NFR links
- None. Test-scoped code only; no user-facing surface, no accessibility, performance or security implications.

#### Notes / open questions
- ADR-001 lives in `cpp-context-stagingdlrm` and must be linked from the PR description, never copied.
- Jira sub-task: [DD-43121](https://tools.hmcts.net/jira/browse/DD-43121).

---

### T2 — Handler + aggregate: real inputs, event-stream assertions, R3 pins

| | |
|---|---|
| Linked Jira story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) (epic [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067)) |
| Jira sub-task | PR1 [DD-43122](https://tools.hmcts.net/jira/browse/DD-43122), PR2 [DD-43123](https://tools.hmcts.net/jira/browse/DD-43123), PR3 [DD-43124](https://tools.hmcts.net/jira/browse/DD-43124) |
| Depends on | T1 |
| PRs | **3 — fixed by the story owner's 2026-08-07 gate decision.** Do not collapse to fewer or split to more |

#### User story
As a **PCFDLRM test engineer/reviewer**,
I want **`MigratedCaseFileAggregateTest` and `MigratedCaseFileHandlerTest` to assert the real events an
aggregate method returns — and the real captured command — as whole payloads, instead of reading
deep-stub getters or aggregate state**,
so that **every XHIBIT-gated behaviour (R3a–R3d) is provably pinned, and deleting a gated branch fails
a test rather than passing silently**.

#### Background
Three things make this bigger than "convert 39 tests to rows": the test's `caseDetails` /
`prosecution` / `prosecutionWithReferenceData` inputs are `RETURNS_DEEP_STUBS` mocks
(`MigratedCaseFileAggregateTest:109-128`), so nothing can be serialised until they are replaced with
real POJOs; ~25 assertions read aggregate getters, two of which (`MigratedCaseValidatedWithWarnings`,
`MigratedCaseNotFoundInAutomation`) have **no** getter or `apply()` arm at all, so no state-based
assertion can ever observe them; and four R3a scenarios — `Invalid Prosecuting Authority` (gate
`:221`), `INVALID_OFFENCE_CODE`, `MISSING_OR_INVALID_PLEA_DATE`, `MISSING_OR_INVALID_VERDICT_DATE`
(all gate `:433`) — do not exist today and must be written, not converted. The shared
row-and-assertion-block shape is worked out in
[Implementation sketches § Aggregate scenario harness (T2)](#aggregate-scenario-harness-t2).

Given the size (1,659 lines, 39 methods today, 43 scenarios after conversion), the story owner's
2026-08-07 gate decision fixes this at **exactly three PRs** — a reviewer must be able to hold each in
their head; none may be merged as a single monolithic rewrite.

#### PR1 — De-mock the inputs (spike, no assertion changes)
Replace `caseDetails`, `prosecution` and `prosecutionWithReferenceData` with real POJOs; keep mocks
only for genuine collaborators (`ReferenceDataQueryService`, the three enrichers). No assertion
changes in this PR — its output (what the emitted events actually contain once the mocks are gone)
decides the fixture shape for PR2 and PR3.

- [ ] AC-PR1-1: Given the existing 39 test methods, when the deep-stub mocks are replaced with real POJOs, then every test still compiles, still passes, and asserts exactly what it asserted before this PR (no assertion rewritten).
- [ ] AC-PR1-2: Given the de-mocked inputs, when an aggregate method's returned events are serialised via the framework `ObjectToJsonObjectConverter`, then serialisation succeeds — proving the inputs are real data, not further mocks.

#### PR2 — Row harness, proven on the `:368` gate only
Introduce the `@ParameterizedTest`/`@MethodSource` row shape and the one shared assertion block
(stream length → type per position → whole payload per position, never a getter), and prove it on
exactly one gate: whether `MigratedCaseFileReceived` reaches the stream at all (`:368`) — R3's
highest-value pin, invisible from state.

- [ ] AC-PR2-1 (traces to AC1, AC2): Given the `:368` gate exercised both ways (source system that satisfies `isXhibit()` vs. one that does not), when the harness runs, then it asserts the returned `Stream<Object>`'s length, the type of each event in order, and each event's payload whole — with no aggregate getter (`getReceiveMigratedCaseFile`, `getMaterialsAdded`, `getMaterialsAddedPostProcessing`, `getMaterailsReadyForCourtDocuments`) appearing in the new assertion.
- [ ] AC-PR2-2 (traces to AC4): Given the `:368` scenario, when the gate is deleted from production locally as a one-off check (not committed), then the new assertion fails — demonstrated in review, guarding against the vacuous-pin failure mode the design calls out.

#### PR3 — Remaining six gates, R3a's four new scenarios, R3b/R3c/R3d, cleanup
Convert the rest of the suite to rows through the shared harness. Fail-fast paths first — nine of the
eleven `MigratedCaseFileProcessed` emissions are single-event early returns
(`:192, 203, 214, 232, 244, 256, 271, 279`) distinguished only by `description`, the cheapest rows and
the right place to settle the fixture convention before the main path at `:378`. Then the remaining
scenario blocks, deleting the ten vacuous `is(XHIBIT)` assertions and the ~25 getter assertions as each
converts; write the four missing R3a scenarios; add the handler's whole-payload assert; land R3b/R3c/R3d.

- [ ] AC-PR3-1 (traces to AC2): Given the completed conversion, when `MigratedCaseFileAggregateTest` is grepped for `getMaterialsAdded`, `getMaterialsAddedPostProcessing`, `getMaterailsReadyForCourtDocuments` and `getReceiveMigratedCaseFile`, then the grep returns **no assertion sites**.
- [ ] AC-PR3-2 (traces to R3a, AC4): Given all seven `isXhibit()` gates (`:221, 282, 368, 423, 433, 554, 562`), when each is exercised on the XHIBIT path, then each has a scenario asserting the concrete effect — the exact problem raised or the exact emitted event — including the four new scenarios (`Invalid Prosecuting Authority` at `:221`; `INVALID_OFFENCE_CODE`, `MISSING_OR_INVALID_PLEA_DATE`, `MISSING_OR_INVALID_VERDICT_DATE` at `:433`), bringing the suite to **43** scenarios.
- [ ] AC-PR3-3 (traces to R1, AC1): Given the ten `assertThat(…getMigrationSourceSystemName(), is(XHIBIT))` assertions (`MigratedCaseFileAggregateTest:373, 663, 701, 804, 1000, 1035, 1074, 1117, 1167, 1217`), when the conversion completes, then all ten are **deleted**, not left alongside the new assertions.
- [ ] AC-PR3-4 (traces to AC1): Given `MigratedCaseFileHandlerTest`'s captured `CaseProcessingArgs`, when the assertion runs, then `captured.getReceiveMigratedCaseFile()` is asserted **whole** against a fixture, while `getSections()` and `getDocumentMetadataReferenceDataList()` remain the value assertions they already are (not wholly serialisable — they carry a `ReferenceDataQueryService` and enricher `Instance` lists).
- [ ] AC-PR3-5 (traces to R3b, AC4): Given `ExhibitFiileTypeValidationRuleTest`, when the XHIBIT path is exercised, then both problem codes (`INVALID_FILE_TYPE_FOR_XHIBIT`, `INVALID_FILE_TYPE_FOR_XHIBIT_MIGRATION`) are pinned whole; existing non-XHIBIT references are left as they are.
- [ ] AC-PR3-6 (traces to R3c, AC4): Given `ProsecutionCaseFileHelperTest`, when `applyRuleToDefendantFields()` runs on the XHIBIT path, then the normalised gender, language and ethnicity values are asserted whole.
- [ ] AC-PR3-7 (traces to R3d, AC4): Given `CcProsecutionValidationRuleProviderTest`, when `getCaseValidationRules(initiationCode)` is called for `SUMMONS`, `REQUISITION`, `SJP` and the default, then each is asserted by **set equality** on the returned rule classes, not `anyMatch`.
- [ ] AC-PR3-8 (traces to AC5): Given a hypothetical new source system added after this story, when a scenario for it is written, then it is confined to scenario data plus a fixture — no new test class, no change to a test method body.

#### Out of scope for this story
- The non-XHIBIT path of every R3 gate, and the two `RefDataEnricher` branches (`PleaDataRefDataEnricher`, `VerdictDataRefDataEnricher`) — deferred to the LIBRA story; this story gives them no signal.
- `COURT_RECORD_SHEET_NOT_PDF` / `COURT_RECORD_SHEET_FILE_TYPE_INVALID` aggregate fail-fast scenarios, the `MaterialAddedPendingProcess` event type, and broadening `acceptMigratedCase`/`materialAddedPostProcessing` coverage — all ungated; carried to the follow-up general-coverage ticket (raise at story closure so these measurements are not lost).
- Adding a source-system axis to `CcProsecutionValidationRuleProvider` — this pins today's selection, it does not change it.

#### Definition of done (applies to each of the 3 PRs individually)
- [ ] Code reviewed and approved — each PR is small enough for a reviewer to hold in their head; a 1,659-line single diff is a review failure, not a deliverable.
- [ ] `mvn clean install` passes.
- [ ] ITs pass via `./runIntegrationTests.sh` with no material runtime increase.
- [ ] No `src/main` file changed.
- [ ] For PR3 specifically: all ACs above satisfied, and the AC-PR3-1 grep result recorded as test evidence on the Jira sub-task.
- [ ] Jira sub-task updated with test evidence per PR.

#### NFR links
- None. Test-scoped code only.

#### Notes / open questions
- Jira sub-tasks: PR1 [DD-43122](https://tools.hmcts.net/jira/browse/DD-43122), PR2 [DD-43123](https://tools.hmcts.net/jira/browse/DD-43123), PR3 [DD-43124](https://tools.hmcts.net/jira/browse/DD-43124) — one per PR so CI/review history maps 1:1 to this sequencing.
- AC7 (deliberately dropping a field from a payload to prove an assertion fails) is a **stage 6 review-step demonstration**, not an AC owned by this story — noted here only so it is not mistaken for a missing scenario.

---

### T3 — Event processor + converters: real converter tree, whole-payload seam assertions

| | |
|---|---|
| Linked Jira story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) (epic [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067)) |
| Jira sub-task | [DD-43125](https://tools.hmcts.net/jira/browse/DD-43125) |
| Depends on | T1 |
| PRs | 1 — if review load is heavy, the natural split (not a design requirement) is *wire the real tree* vs. *the three scenario tiers + processed-event assert*; call this out at planning if taken |

#### User story
As a **PCFDLRM test engineer**,
I want **the `MigratedCaseReceivedProcessor` seam exercised through a real, wired converter tree, with
whole-payload assertions on the `InitiateCourtProceedings` payload sent to Progression and on the
public `migrated-case-file-processed` event**,
so that **Progression's contract is pinned as a complete payload rather than as a mocked `verify()`
call against `JsonValue.NULL`**.

#### Background
`MigratedCaseReceivedProcessorTest` today mocks the converter (`:44`), `objectToJsonObjectConverter`,
`envelopeHelper` **and** the `MigratedCaseFileReceived` input, then verifies
`sender.sendAsAdmin(envelope)` against a `JsonValue.NULL` payload — nothing real flows through it.
All six converters are drivable from one seam
(`MigratedCaseToProsecutionCaseConverter` → its two defendant converters → their two grandchildren →
the hearing converter), so asserting there is coupling to a **contract**, not to six classes coupled
to structure — and the pins survive DD-43081's restructuring. Wiring needs a test-side
`FieldUtils.writeField(target, name, value, true)` factory (commons-lang3, already on the classpath)
because `@InjectMocks` only reaches one level of a three-level, private-`@Inject`-field hierarchy, and
two converters (`MigratedCaseToProsecutionCaseConverter`, `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter`)
need a stubbed `ReferenceDataQueryService`. Capture at
`envelopeHelper.withMetadataInPayloadForEnvelope(...)`, not at `sender` — that isolates the converted
payload and the renamed `progression.initiate-court-proceedings` metadata from whatever
`envelopeHelper` itself does (it has its own test). Worked code:
[Implementation sketches § Converter seam harness (T3)](#converter-seam-harness-t3).

#### Acceptance criteria
- [x] AC-T3-1 (traces to R1, AC1): Given a real converter tree wired via the `FieldUtils` factory in `pcfdlrm-test-support`, with a stubbed `ReferenceDataQueryService` injected into the two converters that need one, when `MigratedCaseReceivedProcessor` runs on a fixture-deserialised `MigratedCaseFileReceived`, then the `Envelope<JsonValue>` captured at `envelopeHelper.withMetadataInPayloadForEnvelope(...)` has metadata name `progression.initiate-court-proceedings` and a payload asserted **whole** against a fixture. Satisfied via `ReflectionFieldInjector.writeField` (generic wrapper, `pcfdlrm-test-support`) — the six-concrete-class tree topology stays in `MigratedCaseReceivedProcessorTest` itself, since `pcfdlrm-test-support` cannot depend back on `pcfdlrm-event-processor` (already a test-scope consumer) without a Maven reactor cycle.
- [~] AC-T3-2 (traces to the design's tier model): Given the three scenario tiers, when they run, then tier 1 (maximal, 1 scenario) has every optional field populated and every collection ≥2 elements; tier 2 (minimal, 1 scenario) has only required fields populated; tier 3 (branch rows, ~5–7 scenarios) covers empty vs. null collections, person vs. legal-entity defendant, and hearing present vs. absent. **Tier 3 fully met**: 6 rows total — 4 in `converterScenarios()` (person/legal-entity defendant isolated, hearing present, past hearing excluded) + 2 standalone `assertThrows` tests (`shouldThrowNpeWhenDefendantsListIsNull`, `shouldThrowNpeWhenHearingListIsNull`) pinning the null-collections branch, which can't fit the fixture-comparison harness since there's no successful payload to compare. **Tier 1 ("every optional field populated") NOT met** — measured, not assumed: `maximalInput()` leaves ~25 optional fields the converters do map unpopulated (e.g. nationality, ethnicity, bail status, parent/guardian info, individual/corporate aliases, receiving/sending court, offence legislation/title, vehicle/alcohol offence facts, `initiationCode`, `committalDate`). A code review on 2026-08-12 caught this — an earlier self-report of "100%" was a stale-`jacoco.exec` artifact (`append=true` carried over coverage from the six per-converter test classes' own runs), not a real measurement. Closing this gap fully would need many small, scattered `maximalInput()`/`referenceDataVO` additions across all six converters, several requiring per-scenario `ReferenceDataQueryService` stubs the current shared-mock harness doesn't support (a real design change, not a quick fixture edit) — and two methods (week-commencing hearing mapping, convicting-court-from-hearing lookup) are structurally unreachable without a hearing present, which `maximalInput()` deliberately has none of. Decision: recorded as a known, honest limitation rather than force it — the underlying converter *logic* for all of these fields remains fully covered by the six pre-existing per-converter unit classes (AC-T3-7, untouched); what's missing is specifically *whole-payload seam* protection for the less-common fields, not logic-level protection.
- [x] AC-T3-3 (traces to the design's "tier 2 is load-bearing" note): Given tier 2's minimal input, when the payload is compared STRICT, then the comparison explicitly pins whether an absent input field yields an **omitted** output field or an explicit **`null`** — named as its own assertion, not an accidental side effect of another scenario. Satisfied by `shouldOmitRatherThanEmitNullForFieldsAbsentFromMinimalInput`.
- [x] AC-T3-4 (traces to AC1): Given `MigratedCaseFileProcessedProcessorTest`, when the public event is asserted, then `public.pcfdlrm.migrated-case-file-processed` is asserted **whole** against a fixture.
- [x] AC-T3-5 (traces to the round-trip convention): Given the `MigratedCaseFileReceived` fixture used as processor input, when it round-trips JSON → POJO → JSON, then the round trip is byte-for-byte unchanged — proving no field is silently dropped by the generated POJO before the downstream STRICT comparison.
- [~] AC-T3-6 (traces to the design's verification-only checks): Given the converter package with the six per-converter test classes **excluded** from the coverage run, when jacoco runs, then it reports 100% method coverage; and given the dumped expected fixture, when it is cross-checked field-by-field against Progression's schema (root `courtReferral.json`, **not** `apiProsecutionCase.json`, read from the sibling checkout not `.m2`) before it is committed, then no field is found mismapped to the wrong same-named schema. **Schema cross-check: met** (unaffected by the coverage finding below). **Coverage target NOT met — real measured numbers, from a genuinely clean `jacoco.exec` (deleted before the run, not reused) with only `MigratedCaseReceivedProcessorTest` + `MigratedCaseFileProcessedProcessorTest` executing:**

  | Converter | Methods | Branches |
  |---|---|---|
  | `MigratedCaseToProsecutionCaseConverter` | 16/18 | 14/20 |
  | `ProsecutionCaseFileMigratedDefendantToCCDefendantConverter` | 4/18 | 7/42 |
  | `ProsecutionMigrationCaseToCCPersonDefendantConverter` | 14/23 | 16/66 |
  | `ProsecutionMigrationCaseFileToCCLegalEntityDefendantConverter` | 4/5 | 6/10 |
  | `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter` | 51/58 | 78/144 |
  | `ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter` | 19/20 | 45/90 |
  | **Total** | **108/142 (76%)** | **166/372 (45%)** |

  **Why recorded instead of closed (2026-08-12 decision):** the "100%" previously claimed here was a measurement artifact — jacoco's `append` defaults to `true`, so a prior full-module test run (which includes the six per-converter test classes) had left their coverage baked into `target/jacoco.exec`; re-running only the two seam test classes without deleting that file first reused their coverage and produced a false 100%. Deleting the exec file first and re-measuring gave the numbers above. Closing the real gap would require many `maximalInput()`/`referenceDataVO` additions across all six converters, several needing per-scenario `ReferenceDataQueryService` stubbing the shared-mock harness doesn't currently support, plus two methods that are structurally unreachable without a hearing present in a scenario deliberately designed to have none (`maximalInput()`). Given the scope, the choice made was to record the true numbers rather than force them to 100% — the converter *logic* itself stays fully protected by the six pre-existing per-converter unit test classes (AC-T3-7, untouched, 138 tests green); only *whole-payload seam* protection for these less-common fields is the acknowledged gap.
- [x] AC-T3-7 (traces to the design's explicit decision): Given the six existing per-converter test classes, when this story completes, then all six still exist and still pass — none is deleted.

#### Out of scope for this story
- Deleting the six per-converter test classes (explicit design decision — they cost nothing as smoke tests once the seam is pinned).
- Anything DD-43081 adds at PCFDLRM's nesting level (18 fields, +6 more, the orphaned `pcf-policeOfficerInCase.json`).
- AC7's deliberate-break demonstration (stage 6 review step, not owned by this story).

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes.
- [ ] ITs pass via `./runIntegrationTests.sh` with no material runtime increase.
- [ ] No `src/main` file changed.
- [ ] Jacoco 100%-method-coverage measurement (converter package, per-converter tests excluded) and the fixture-vs-Progression-schema cross-check both recorded as evidence on the Jira sub-task — one-off measurements at the gate, not build gates.
- [ ] Jira sub-task updated with test evidence.

#### NFR links
- None. Test-scoped code only; the payload contract asserted is unchanged, not altered.

#### Notes / open questions
- Jira sub-task: [DD-43125](https://tools.hmcts.net/jira/browse/DD-43125).

---

### T4 — Integration tests: XHIBIT-only fixtures, whole-payload boundary check

| | |
|---|---|
| Linked Jira story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) (epic [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067)) |
| Jira sub-task | [DD-43126](https://tools.hmcts.net/jira/browse/DD-43126) |
| Depends on | T1 |
| PRs | 1 |

#### User story
As a **PCFDLRM test engineer**,
I want **the three LIBRA-fixture IT command JSONs re-pointed to XHIBIT, and the
`initiatecourtproceedings` WireMock check upgraded from field spot-checks to a whole-payload
comparison**,
so that **every IT journey runs on the XHIBIT baseline this story pins, and the wire-level Progression
contract is checked to the same depth as the unit layer**.

#### Background
`pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json`,
`pcfdlrm.command.receive-multiple-hearing-wc-migrated-case-file.json` and
`pcfdlrm.command.receive-with-no-hearing-migrated-case-file.json` all carry
`"migrationSourceSystemName": "LIBRA"` today, so those three journeys have **no XHIBIT baseline at
all** — precisely the gap this story exists to close, and the one AC6 forbids continuing. Per the
design's explicit decision: **convert, do not duplicate** — duplicating would leave LIBRA journeys
running at IT level (which AC6 forbids) and add Docker runtime for coverage that belongs at unit
level. `prosecutorOffenceId` values containing the literal string `LIBRA` are left alone — they are
not the source-system field, and renaming them is churn with no coverage effect. Unlike the sibling
story in `cpp-context-stagingdlrm` (where the base IT journey has never run as XHIBIT), the base
journey here is **already** XHIBIT, so re-pointing is expected to be routine; any assertion result
that changes on re-pointing is a real behavioural difference and is raised immediately, not absorbed.
The `initiatecourtproceedings` request assertion moves from three `withJsonPath` checks to a
whole-payload comparison; the `withRequestBody(containing(...))` WireMock filter that selects the
request stays — that is request *selection*, not assertion.

#### Acceptance criteria
- [ ] AC-T4-1 (traces to AC6): Given the three command fixtures listed above, when they are inspected, then none sets `migrationSourceSystemName` to `LIBRA` — all three resolve to `XHIBIT`.
- [ ] AC-T4-2 (traces to the design's explicit decision): Given the same three fixtures, when `prosecutorOffenceId` values are inspected, then any value containing the literal string `LIBRA` is left unchanged.
- [ ] AC-T4-3 (traces to R1, AC1): Given the `initiatecourtproceedings` WireMock request captured by an IT, when the assertion runs, then it compares the **whole** captured request body against a fixture (STRICT, individually enumerated exclusions), replacing the three `withJsonPath` assertions; the `withRequestBody(containing(...))` selector is retained unchanged for request selection.
- [ ] AC-T4-4 (traces to the design's canary note): Given the three re-pointed ITs, when they are run, then no existing assertion result changes as a consequence of re-pointing alone; if one does, it is raised as a real behavioural difference between source systems, not absorbed into a fixture.
- [ ] AC-T4-5 (traces to R4, AC8): Given the full IT suite, when run via `./runIntegrationTests.sh`, then it passes with **no new journey** added, **no unit-level scenario ported down**, and no material runtime increase.

#### Out of scope for this story
- Any new IT journey.
- Porting aggregate/converter-level scenario variants down into ITs — the design explicitly warns against this once the ITs are open, since the gain is small and the Docker runtime cost recurs on every build.
- `cpp-apitests`, and any LIBRA scenario at either test layer.

#### Definition of done
- [ ] Code reviewed and approved.
- [ ] `mvn clean install` passes.
- [ ] ITs pass via `./runIntegrationTests.sh` with no material runtime increase (this task touches ITs directly — the "no increase" check is load-bearing here, not a formality).
- [ ] No `src/main` file changed.
- [ ] Jira sub-task updated with test evidence.

#### NFR links
- None. Test-scoped code only; no change to the wire contract itself, only to what is asserted about it.

#### Notes / open questions
- Jira sub-task: [DD-43126](https://tools.hmcts.net/jira/browse/DD-43126).

---

## Implementation sketches

Illustrative, not prescriptive. They fix the *shape* the design argues for — an ordered event list as
the expected value, and a capture point that sees the converted payload. Names, packaging and
matcher choice are the implementer's.

### Aggregate scenario harness (T2)

The row and the one shared assertion block. Every aggregate scenario goes through this, which is what
makes R1's no-getters rule structural rather than a review checklist.

```java
record ExpectedEvent(Class<?> type, String fixture) {}

record AggregateScenario(String name,
                         String sourceSystem,
                         CaseFileInput input,
                         List<ExpectedEvent> expected) {}
```

```java
final List<Object> actual = aggregate.receiveMigratedCaseFile(argsFor(scenario)).toList();

// Count first: an extra or missing event should fail here, naming the problem,
// rather than surfacing as a confusing payload diff at position 0.
assertThat(actual, hasSize(scenario.expected().size()));

for (int i = 0; i < actual.size(); i++) {
    final ExpectedEvent e = scenario.expected().get(i);
    assertThat("event " + i, actual.get(i), instanceOf(e.type()));
    assertWholePayload(actual.get(i), FixtureLoader.load(e.fixture()), EXCLUSIONS);
}
```

`assertWholePayload` serialises via the framework `ObjectToJsonObjectConverter` and compares with
`WholePayloadMatcher` (JSONassert STRICT, anchored enumerated exclusions).

Two notes for whoever builds this:

- The same block serves `materialAddedPostProcessing` and `acceptMigratedCase` — all three entry
  points return `Stream<Object>`. Broadening coverage of those two is a **follow-up ticket**, not this
  story, but the harness should not assume `receiveMigratedCaseFile`.
- Nine of the eleven `MigratedCaseFileProcessed` emissions are fail-fast early returns producing a
  single-event stream distinguished only by `description`. Those rows are near-trivial once this
  block exists, and are the right place to settle the fixture convention before the main path.
- **Four of those rows do not exist today** and must be written: `Invalid Prosecuting Authority`
  (gate `:221`), and `INVALID_OFFENCE_CODE` / `MISSING_OR_INVALID_PLEA_DATE` /
  `MISSING_OR_INVALID_VERDICT_DATE` (gate `:433`). R3a is not satisfiable by converting the existing
  39 — see [`02-design.md` § Coverage](./02-design.md#coverage-what-r3-forces-what-defers).

### Converter seam harness (T3)

Capture where the converted payload is, not where the envelope is finally sent.

```java
@Captor
private ArgumentCaptor<Envelope<JsonValue>> converted;

// ...

verify(envelopeHelper).withMetadataInPayloadForEnvelope(converted.capture());

assertThat(converted.getValue().metadata().name(), is("progression.initiate-court-proceedings"));
assertWholePayload(converted.getValue().payload(), FixtureLoader.load(fixture), EXCLUSIONS);
```

The real converter tree has to be assembled first — all six converters use private `@Inject` fields
and `@InjectMocks` populates only one level of three. A test-side factory using
`FieldUtils.writeField(target, name, value, true)` (commons-lang3, already on the classpath) builds
the tree and injects a stubbed `ReferenceDataQueryService` into the two converters that need one.

The input `MigratedCaseFileReceived` is deserialised from a fixture rather than hand-built, which is
why the round-trip fidelity test matters here specifically: a field absent from the generated POJO is
dropped silently on the way in, and the STRICT comparison on the way out still passes.

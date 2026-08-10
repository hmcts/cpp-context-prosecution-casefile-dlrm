# Input brief — LIBRA enabler: PCFDLRM test hardening

> Stage 0 artefact. Feeds [`01-requirements.md`](./01-requirements.md).
> **Self-contained** — everything an SDLC stage needs to run against this story is in this
> directory or reachable by the links below.

| | |
|---|---|
| Epic | [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler |
| Story | [DD-43099](https://tools.hmcts.net/jira/browse/DD-43099) — PCFDLRM test hardening |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Sibling story | [DD-43078](https://github.com/hmcts/cpp-context-stagingdlrm/tree/main/docs/pipeline/DD-43067-DD-43078-test-hardening) — the same hardening in `cpp-context-stagingdlrm` |

## The epic this story belongs to

**DD-43067 — LIBRA enabler.** Ingest magistrates' court case files from legacy system LIBRA
through the existing DLRM pipeline (Azure Blob → Function App → stagingDLRM → **PCFDLRM** →
Progression), reusing the XHIBIT path rather than forking it.

**Design decision already taken for the epic** (analysis §2): XHIBIT and LIBRA share **one**
stagingDLRM endpoint and **one** schema family. Source-system-specific behaviour is pluggable
strategies inside the shared path, not duplicated schemas, endpoints, or command/event types. The
rejected separate-schema alternative and the reasoning are in
[`libra-ingestion-analysis.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-ingestion-analysis.md) §7.

PCFDLRM already works this way: one shared ingestion path, a map-based rule-set provider
(`CcProsecutionValidationRuleProvider`) keyed by `CaseType`/`Channel`, and a small number of
scoped `XHIBIT`-guarded branches.

**The accepted cost of the shared-path design is coupled blast radius** — a change made to
accommodate LIBRA touches code XHIBIT already depends on in production. The agreed mitigation is a
test suite that treats source system as a variable. This story makes that mitigation real on the
PCFDLRM side.

## Why this is a separate story from DD-43078

The two halves of the hardening are **fully independent**: no shared code, no shared fixtures, no
ordering constraint, different repos, separate CI runs. Either can merge first and either can slip
a sprint without stranding the other. Under the team workflow's slice test that makes them two
stories, not one story spanning two repos — so DD-43078 keeps the `cpp-context-stagingdlrm` work
and DD-43099 (this story) carries PCFDLRM.

**One thing is genuinely shared**: the scenario-DSL and whole-payload assertion convention, so that
two developers working in parallel do not invent two dialects. It is recorded once, as
[ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md)
in the stagingDLRM repo, and linked — never copied. It must be approved before this story starts
stage 5.

## This story's request

Harden the existing tests in the **PCFDLRM context** (command handler, aggregate, event processor,
validation rules) so that:

1. Tests are written with **XHIBIT** as the source system, and **assertions cover whole
   payloads**, because the shared schema is being relaxed.
2. Tests can be **cleanly extended for LIBRA later** to add new scenarios — following a DSL
   framework like `HearingFinancialResultsAggregateNCESTest` in `cpp-context-results`, where
   needed.
3. **Integration tests cover XHIBIT exclusively, but not to the same extent as the unit tests** —
   unit tests need to cover every possible scenario.

**No production behaviour changes.** Test, fixture and test-support code only.

## Depth model — the two test layers are not held to the same bar

| Layer | Depth expected |
|---|---|
| Unit / component | **Exhaustive.** Every scenario that matters — each rule path, each source-system variant, each behaviour that currently branches on XHIBIT. |
| Integration | **Representative.** Enough journeys to prove the wiring and that the payload crossing each service boundary is whole. No scenario matrix. |

The asymmetry is about cost, not confidence: ITs need Docker and a running environment
(`mvn verify -P pcfdlrm-integration-test`), so they are the wrong place to enumerate variants.

## Why now — the driver

The schema relaxation LIBRA needs (DD-43081) removes constraints from the shared schema family
that **XHIBIT payloads currently rely on**. Across the whole payload the impact matrix counts
**17 `relax-*` rows** — every one a check the shared schema stops performing for XHIBIT.

Most of those constraints are enforced upstream, in stagingDLRM's canonical schema, and pinning
them is DD-43078's job. What lands **here** is the consequence: once the enum on `initiationCode`
is dropped, real codes (`C`, `J`, `Q`, `S`) reach PCFDLRM for the first time and route into
rule sets that migrated cases have never entered. Today every migrated case lands in the generic
default set because stagingDLRM forces `"O"`.

Whatever the schema no longer guarantees, the tests must. Tests asserting a handful of fields will
keep passing while the payload silently changes shape — the regression this story exists to
prevent.

## Current state — verified by direct inspection

Verified on branch `team/dlrm8`, not assumed. **This repo's test suite is already in good shape**;
the story is *close the source-system gap and fill named holes in a working suite*, not *build a
behavioural suite*.

| Artefact | Verified state |
|---|---|
| `pcfdlrm-domain-aggregate/.../aggregate/MigratedCaseFileAggregateTest.java` | 1,659 lines, **39** `@Test`/`@ParameterizedTest` methods on real generated POJOs via `ObjectBuilder`/`TestConstants`; asserts emitted-event content |
| `pcfdlrm-domain` validation rules | **63** test classes |
| `pcfdlrm-integration-test/.../ReceiveMigratedCaseFileIT.java` | 14 tests (several parameterised), 24 command fixtures, 64 test resource files, WireMock stubs for Progression, Material and reference data |
| `pcfdlrm-command-handler/.../MigratedCaseFileHandlerTest.java` | 4 tests, 353 lines; captures `CaseProcessingArgs` via `ArgumentCaptor` and asserts two of its fields |
| `pcfdlrm-refdata` / `pcfdlrm-event-processor` | 19 / 16 test classes |
| `pcfdlrm-query` | **no `src/test` directory at all** |
| `pcfdlrm-viewstore-persistence` | `src/test` exists, contains **no Java sources** |

**The gap is not coverage breadth — it is that source system is hardcoded.**
`pcfdlrm-domain-aggregate/src/test/java/uk/gov/moj/cpp/pcfdlrm/builder/ObjectBuilder.java:43` sets
`migrationSourceSystemName` from the constant `TestConstants.SOURCE_SYSTEM_XHIBIT` (`"XHIBIT"`,
`TestConstants.java:11`), so all 39 aggregate tests are XHIBIT **by construction**. Ten
`assertThat(…getMigrationSourceSystemName(), is(XHIBIT))` assertions — lines 373, 663, 701, 804,
1000, 1035, 1074, 1117, 1167, 1217 — confirm passthrough of a value the builder itself just set.
They do not distinguish source-system-**dependent** behaviour from source-system-**independent**
behaviour, which is precisely the distinction LIBRA will need.

The Progression boundary is **better than a stub-hit check but still a spot check**:
`ReceiveMigratedCaseFileHelper.java:180` captures the outbound `initiatecourtproceedings` request
and asserts exactly two `withJsonPath` values (`retrialIndicator`, offence `count`).

## What the current IT layer actually looks like

Checked, not assumed. Of the 24 IT command fixtures, exactly **three** carry
`"migrationSourceSystemName": "LIBRA"`:

- `pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json`
- `pcfdlrm.command.receive-multiple-hearing-wc-migrated-case-file.json`
- `pcfdlrm.command.receive-with-no-hearing-migrated-case-file.json`

Every other fixture — including the base `pcfdlrm.command.receive-migrated-case-file.json` — is
already XHIBIT. Other `LIBRA` hits in those files are `prosecutorOffenceId` string values
(`"LIBRA-offence-id-1"`), not the source system.

So unlike stagingDLRM — where the base IT journey has never run as XHIBIT — the base journey here
is already correct, and FR6 below is three fixtures re-pointed rather than a sweep.

## Decisions taken with the requester

| Question | Decision |
|---|---|
| Repo representation | PCFDLRM is its **own story** under the epic (DD-43099), not a task on DD-43078 — the two halves are independently deliverable. |
| Test scope | Unit/component **and** in-repo integration tests, at different depths (above). `cpp-apitests` out of scope. |
| Artefact layout | One pipeline directory per story, named `<epicKey>-<storyKey>-<slug>`, each self-contained. |
| Story independence | Stories under this epic are **independent** — this one carries no cross-story dependency and can be picked up on its own. |
| Shared convention | ADR-001 in the stagingDLRM repo, linked not copied; approved before either story starts stage 5. |

## Reference pattern

`cpp-context-results`:

- `results-domain/results-domain-aggregate/src/test/java/uk/gov/moj/cpp/results/domain/aggregate/HearingFinancialResultsAggregateNCESTest.java`
- `.../HearingFinancialResultAggregateTestSteps.java` (the DSL itself, ~425 lines)

Shape of it, for design reference:

- Scenarios are `static Stream<Arguments>` methods — each row a human-readable label plus a
  fluent scenario object — consumed by `@ParameterizedTest(name = "{index} => {0}")` +
  `@MethodSource`. The test body is one line: `scenario.run(name, new Aggregate())`.
- A scenario is a sequence of named steps; each supplies an input event from a JSON fixture and
  declares expected outcomes.
- Assertions are **whole-payload JSON comparisons** against an expected fixture, with explicitly
  listed exclusions for non-deterministic values and named parameter substitution — not
  field-by-field getters.

Adding a case is adding a row and two fixtures. That is the extensibility property requirement 2
asks for. ADR-001 records how this repo applies it.

## Supporting analysis

Both live in the stagingDLRM repo and are regenerable from the data-schema workbook via
`tools/schema-gen/`:

- [`libra-ingestion-analysis.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-ingestion-analysis.md)
  — pipeline trace, per-system change plan, open questions, and the rejected alternative (§7).
  §3.4 and §5 Q6 cover the PCFDLRM XHIBIT-only behaviours.
- [`libra-schema-impact.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-schema-impact.md)
  — field-level impact across the func-app gate, the canonical schema, pcfdlrm and Progression
  (§2, with the matrix in `libra-schema-impact.csv`), and the downstream triage of the 44
  LIBRA-added fields (§5).

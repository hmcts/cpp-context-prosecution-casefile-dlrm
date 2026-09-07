# Design — DD-43194: J17→J25 behavioural-parity tests for PCFDLRM

> Stage 2 artefact (architecture & design). Source: [`01-requirements.md`](./01-requirements.md),
> [`00-input-brief.md`](./00-input-brief.md).
>
> This document resolves the six "Notes for the design stage" questions in `01-requirements.md`
> with concrete file/class/method decisions — each verified against the current code on
> `team/25.104.x` (still J17), not assumed. **No production code, ADR, pom, or CI config is touched
> by this stage** — every decision below is "which existing test class gets a new method" or "where
> does one new small test class go," per FR16.

## How to read this doc

For each Bucket A item: **what exists today** (verified by reading the code), **the design
decision** (what gets added, and where), and **why** (which requirement/AC it satisfies, and what
alternative was rejected). Sequencing follows FR/note 6: BC-08 first.

---

## A. BC-08 — the primary item

### A1. Carrier C1 — `ListHearingRequest.listedStartDateTime`

**What exists:** `ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter` (main,
`pcfdlrm-event-processor`) has a private `getDateAndTimeOfHearing()` (line 175) feeding
`.withListedStartDateTime(...)` at line 244 — matches FR5/C1 exactly. Its existing test class,
`ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverterTest`, already calls the
public `convert()` with realistic fixtures (`getMigratedHearingWithReferenceData(...)`), but only
asserts `assertNotNull`/`assertNull` on the field (lines 377, 403, 430) — **never the zone identity
or serialized form**.

**Decision:** add **one new `@Test`** to that existing class — e.g.
`shouldPinZoneIdentityOnListedStartDateTime()` — reusing the existing fixture builder. It calls
`convert()` (the public entry point; `getDateAndTimeOfHearing()` cannot be called directly), then
asserts:
- `getListedStartDateTime().getZone()` equals the region `ZoneId.of("UTC")` (FR5's asymmetry, pinned
  as-observed per the story's risk note — not as a claim it's correct).
- The serialized form via `new ObjectMapperProducer().objectMapper().writeValueAsString(...)`
  matches the current J17 rendering.

**Why this mapper approach, not reflection (resolves design note #2):** the converter's
`OBJECT_MAPPER` field is `private static final`, no accessor. But it's constructed with
`new ObjectMapperProducer().objectMapper()` — a plain, dependency-free, no-arg call. Constructing a
second instance the same way *is* "a mapper configured the way the product's is," because it's the
same producer class and method, not a hand-rolled substitute. Reflection would only be justified if
the producer took runtime-only config the test couldn't reproduce; it doesn't. **Decision: construct,
don't reflect.**

### A2. Carrier C3 — `Defendant.courtProceedingsInitiated`

**What exists:** `ProsecutionCaseFileMigratedDefendantToCCDefendantConverter.convert()` line 71:
`.withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))`. Its existing test class,
`ProsecutionCaseFileMigratedDefendantToCCDefendantConverterTest`, calls `convert()` already but has
**zero** assertions on this field today.

**Decision:** add **one new `@Test`** — e.g. `shouldPinZoneIdentityOnCourtProceedingsInitiated()` —
asserting `getCourtProceedingsInitiated().getZone()` equals `ZoneId.of("UTC")` and that
`.toString()` ends in the region form, not `"Z"`. Per FR6, the pin is on **zone identity and
rendering**, never the instant (`now()` is non-deterministic by design) — assert on `.getZone()` and
the zone suffix of the serialized string, not on equality of the whole value.

### A3. FR7 — `toDefaultUtcTime` (Europe/London DST arithmetic, not BC-08)

**What exists — this is the one item where nothing needs writing.**
`MigratedCaseFileAggregateTest` + `AggregateScenarios.fixedHearingTimeDefaultingScenarios()`
(`AggregateScenarios.java` lines 141–143) **already** run this exact scenario through the aggregate's
public `receiveMigratedCaseFile()`:
- `FUTURE_HEARING_DATE_GMT` → asserts `timeOfHearing = "10:00:00"`.
- `FUTURE_HEARING_DATE_BST` → asserts `timeOfHearing = "09:00:00"`.

That is AC4 in full (one GMT date, one BST date, an hour apart), already executed on J17 today.

**Decision:** **no new test.** Add a one-line comment on the `fixedHearingTimeDefaultingScenarios()`
method and the two `Arguments.of(...)` rows pointing at FR7, labelling this as the `java.time` pin —
not a BC-08 carrier (per FR7's own instruction) — so `docs/j25-parity-checklist.md` can cite it as
🟢 with these exact test names rather than commissioning a duplicate. This is the same
annotate-don't-duplicate treatment FR11 gives BC-03.

### A4. AC5 — the payload-boundary assertion

**What exists:** `MigratedCaseReceivedProcessorTest.shouldConvertAndSendInitiateCourtProceedings`
already builds and compares the **whole** outbound `initiateCourtProceedings` payload — the actual
boundary to `cpp-context-prosecution-casefile` — but currently **excludes**
`courtProceedingsInitiated` (and `listedStartDateTime` reaches it too) from the comparison (lines
90–94), because `ZonedDateTime.now()` isn't deterministic.

**Decision:** don't widen the exclusion into a new test class. Narrow it instead: replace the blanket
exclusion for `courtProceedingsInitiated`/`listedStartDateTime` with a targeted assertion on the
*pattern* of the rendered value (e.g. a regex/suffix check that it renders as `...+00:00` /
`"UTC"`-suffixed rather than `Z`-suffixed — whatever the real J17 run shows), leaving the rest of the
whole-payload comparison untouched. This satisfies AC5 (an assertion *at the boundary*, not just on
the converter's return value) with a small edit to an existing test rather than a new one.

### BC-08 tally

| Where | Change |
|---|---|
| `ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverterTest` | +1 test method |
| `ProsecutionCaseFileMigratedDefendantToCCDefendantConverterTest` | +1 test method |
| `AggregateScenarios.java` / `MigratedCaseFileAggregateTest` | 0 new tests, +annotation only |
| `MigratedCaseReceivedProcessorTest` | narrow an existing exclusion into an assertion |

Two new test methods, one narrowed assertion, one annotation. Not ten.

---

## B. BC-11 — corrected: `JsonObjects` null-guard parity

**What exists:** confirmed by grep — the only `JsonObjects.createObjectBuilder()...add(...)` call
sites in this repo's main code are in `pcfdlrm-event-processor` (`MetadataHelper`, `EnvelopeHelper`,
`MaterialEventProcessor`). No existing test class covers this directly (`MetadataHelperTest` /
`EnvelopeHelperTest` don't exist).

**Decision:** one new, small, standalone test class —
`pcfdlrm-event/pcfdlrm-event-processor/src/test/java/.../utils/JsonObjectsNullGuardParityTest.java`
— exercising `uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder()` **directly**
(the framework helper itself), not through `MetadataHelper`/`EnvelopeHelper`'s business logic. This
mirrors exactly how the corrected guide's own reference test worked on
`cpp-context-notification-notify`, and avoids having to reverse-engineer which of the three callers'
code paths can actually produce a null value. One `@Test` asserting
`add(key, (String) null)` throws `NullPointerException` on J17 is sufficient — this is a parity pin
on shared framework behaviour, not a per-caller test.

**Rejected:** the requirements' original framing (assert exactly one resolvable JSON-P provider
across five modules) — superseded per the `01-requirements.md` FR9 revision.

---

## C. BC-03 / BC-20 — access control

**What exists:** `pcfdlrm-command-api/src/main/resources/META-INF/kmodule.xml` declares **exactly
one** kbase (`COMMAND_API`) and one ksession (`COMMAND_API_SESSION`, stateless).
`ReceiveMigratedCaseRuleTest extends BaseDroolsAccessControlTest` already has both
`shouldAllowAuthorisedUserToReceiveMigratedCase` and
`shouldNotAllowedUnAuthorisedUserToReceiveMigratedCase` — allow and deny both covered.

**Decision — BC-03 (FR11 annotation):** add a one-line comment on `ReceiveMigratedCaseRuleTest`
citing BC-03 and noting it's refuted-but-pinned (no new test).

**Decision — BC-20 (FR11 new gap):** one new test — e.g. `CommandApiKnowledgeBaseRuleCountTest` in
`pcfdlrm-command-api` — asserting a non-zero total rule count for kbase `COMMAND_API`. Because there
is only **one** kbase here (unlike the reference guide's `system-doc-generator` trap, where a second,
legitimately-empty `QUERY_API` kbase caused a false failure), there's no risk of the guard firing on
a kbase that's supposed to be empty.

**Open item carried to stage 4 (not resolved here):** `BaseDroolsAccessControlTest` is not in this
repo's source — it's the external `access-control-test-utils` dependency. Per design note #4, do
**not** copy the investigation report's rule-count snippet; whoever writes the test in stage 4/5 must
confirm the real accessor against that dependency's actual API (the reference guide's lesson —
`KieServices.get().getKieClasspathContainer().getKieBase(name).getKiePackages()`, summed
`getRules().size()` per kbase, because `StatelessKieSession` doesn't expose the `KieBase` — is a
starting point to verify, not to assume).

---

## D. BC-07 — `liquibase.properties` key inventory

**What exists:** `pcfdlrm-viewstore-liquibase/src/main/resources/liquibase.properties` has exactly
three keys today: `changelogFile`, `liquibase.hub.mode: off`, `liquibase.headless: true`.
`liquibase.hub.mode` is precisely the property the corrected guide flags as rejected by Liquibase 5
— this is a live, present exposure, not a hypothetical.

**Decision:** one small JUnit test in `pcfdlrm-viewstore-liquibase` (no `maven-enforcer-plugin`
exists anywhere in this repo, confirming design note #3's fallback to plain JUnit) that loads
`liquibase.properties` from the classpath and asserts its key set equals exactly
`{changelogFile, liquibase.hub.mode, liquibase.headless}` today. This pins the current inventory so
the **upgrade** story's removal of `liquibase.hub.mode` is a deliberate, visible diff against this
test rather than a silent deploy-time surprise — the removal itself is out of scope here (FR16).

---

## E. BC-13 — `WholePayloadMatcher` as test infrastructure

**What exists:** `pcfdlrm-test-support/.../WholePayloadMatcher.java` wraps `org.json`/JSONassert
(`JSONCompare`, `JSONCompareMode.STRICT`, a custom `ExactPathExclusionComparator`). Its own source
already references ticket DD-43078 for its wildcard-exclusion rule, confirming it's actively-shared
infrastructure per design note #5 (ADR-001 governs its DSL).

**Decision:** one new test class in `pcfdlrm-test-support` — e.g. `WholePayloadMatcherParityTest` —
asserting the matcher's verdict for a representative set of value shapes (numeric renderings, an
excluded-path match/mismatch, a STRICT-mode type mismatch). This tests **over** the public
`matchesWholePayload(...)` API only; the class itself is not touched, consistent with design note #5.

---

## F. BC-12 — `resteasy-multipart-provider` scope

**What exists:** confirmed present only in `pcfdlrm-integration-test/pom.xml` (line 67).

**Decision:** this is IT-tier and, per the depth model, **authored, not executed** (only 2 IT classes
exist, no confirmed WildFly 40 image). A single JUnit test in `pcfdlrm-integration-test`'s test
sources that resolves the multipart-provider class from the classpath records the expected scope in
code (so a fleet-wide RESTEasy repackaging that silently drops or relocates it is visible in a diff),
but it is marked 🟡 in the checklist — authored, not run — like the rest of the IT tier, per FR1.

---

## G. BC-21 — codegen type inventory

**What exists:** `uk.gov.justice.generators:pojo-generation-plugin` (and sibling generator plugins)
run in exactly **8 modules**: `domain-aggregate`, `domain-event`, `domain-event-processor`,
`domain-value-schema`, `event`, `event-processor`, `command-handler`, `command-api` — matching FR12's
claim precisely. `pcfdlrm-domain-event` alone has 8 event JSON schemas under
`src/main/resources/json/schema/`.

**Important scoping finding:** each module's `target/generated-sources` tree is **not** limited to
that module's own schemas — RAML-referenced external/vendored schemas generate classes under
unrelated packages too (e.g. `uk.gov.justice.hearing.courts.*`,
`uk.gov.justice.unified_search_query.courts.*` show up in `domain-event`'s generated sources,
alongside this repo's own `uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event.*`). A codegen test
that naively asserts "the generated-sources tree contains exactly N classes" would break on any
upstream schema change unrelated to this repo.

**Decision:** scope each module's BC-21 check to **its own package prefix only**
(e.g. `uk.gov.moj.cps.prosecution.casefile.dlrm.domain.event` for `domain-event`), deriving the
expected class name set from that module's own `src/main/resources/json/schema/*.json` filenames via
the generator's existing naming convention (schema id → PascalCase type name — already observable
from the generated sources, e.g. `pcfdlrm.events.material-added.json` → `MaterialAdded`), and
asserting via classpath reflection that exactly those classes exist post-generation. One small JUnit
test per module — plain JUnit again, per design note #3. **Open item for stage 4:** confirm the exact
naming-convention mapping against the plugin's own contract before hard-coding it, rather than
inferring it from one example.

---

## Sequencing (per FR/note 6)

1. BC-08 (A1–A4) — primary item, gets the review attention.
2. BC-11 (B) — corrected item, small and now well-understood.
3. BC-20 (C) — real gap, but small once the kbase accessor is confirmed.
4. BC-07, BC-13, BC-21, BC-12, BC-03-annotation — small, independent, can be cut from the back if the
   story needs trimming, per the input brief's own risk note.

## What this design doc does not do

- It does not write any test code — that's stage 4 (test specs) / stage 5 (implementation).
- It does not touch `01-requirements.md`'s FRs/ACs — every decision above traces to an existing
  FR/AC without changing its scope.
- It does not amend ADR-005 (single-homed in stagingDLRM) — nothing here changes the Bucket A
  applicability matrix, only how each item is implemented in this repo.

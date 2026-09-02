# Requirements — DD-43194: J17→J25 behavioural-parity tests for PCFDLRM

> Stage 1 artefact (requirements). Source: [`00-input-brief.md`](./00-input-brief.md).
> Requirements altitude — nothing here prescribes a class layout or a test-class name. Implementation
> **tasks** come from the design / story-writer stage.
>
> **Scoped to `cpp-context-prosecution-casefile-dlrm`, branch `team/25.104.x` while it is still J17.** The stagingDLRM half is
> [DD-43192](https://github.com/hmcts/cpp-context-stagingdlrm/blob/team/25.104.x/docs/pipeline/DD-43191-DD-43192-j25-parity/01-requirements.md).
> Method and scope are fixed by [the parity-method ADR](../adrs/DD-43191-j25-parity-method.md),
> single-homed in the stagingDLRM repo; this document does not restate it, it makes it testable.

## Story

**[DD-43194](https://tools.hmcts.net/jira/browse/DD-43194) — Pin PCFDLRM's J17 behaviour at the seams
the Java 25 upgrade will move**

| | |
|---|---|
| Epic | [DD-43191](https://tools.hmcts.net/jira/browse/DD-43191) — Java 25 upgrade, DLRM contexts |
| Size | **S–M** — smaller than DD-43192: no Function App, and two of its eight items need no new test |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Target branch | **`team/25.104.x`**, cut from `main` before this story starts and not yet upgraded — so still `service-parent-pom 17.104.1` on JDK 17 |
| Depends on | [the parity-method ADR](../adrs/DD-43191-j25-parity-method.md) accepted before stage 5. No other blocker — can start immediately |
| Blocks | [DD-43194 upgrade stage](../DD-43191-DD-43194-j25-upgrade/00-input-brief.md) — same branch; that story may not start until this one merges |
| Sibling story | [DD-43192](https://github.com/hmcts/cpp-context-stagingdlrm/blob/team/25.104.x/docs/pipeline/DD-43191-DD-43192-j25-parity/00-input-brief.md) — same stage in stagingDLRM, independently deliverable |
| Production changes | **none expected** — test, fixture and documentation only. See FR13 |
| Platform tickets | PEG-3296 (upgrade), **PEG-3377** (parity testing) |

### Summary (JIRA summary line)

`[Java 25] Pin PCFDLRM J17 behaviour: ZonedDateTime zone identity on the outbound CC payload, JSON-P provider resolution, codegen and deploy-time guards`

### User story

As a **developer who will shortly move PCFDLRM to Java 25, WildFly 40 and Jakarta EE 11**,
I want **the behaviours that the upgrade's library bumps are known to move to be asserted against the
current Java 17 stack, executed green on Java 17, and merged to `team/25.104.x` before anything on that
branch is upgraded**,
so that **the upgrade branch inherits a regression gate — in particular on the date-time values this
context sends downstream to `cpp-context-prosecution-casefile` and Progression, where a silent zone
identity change would corrupt migrated hearing data rather than fail a build**.

## Why this repo's shape differs from DD-43192

Both repos share the method and the BC list. They do **not** share where the risk sits, and levelling
them would put effort in the wrong place:

| | stagingDLRM (DD-43192) | PCFDLRM (this story) |
|---|---|---|
| Primary item | BC-13 + DLRM-01 — two schema-validation stacks | **BC-08** — `ZonedDateTime` zone identity |
| Where the primary risk lives | the ingestion gate | **main code on the outbound payload path** |
| `ZonedDateTime` in main code | none (one test helper) | **2 carriers**, both on the outbound payload |
| everit / `org.json` product seam | yes (`domain-value-schema` catalogue) | **none** — no everit anywhere |
| Function App | yes (DLRM-01) | **none** |
| Access-control coverage | 2 rules, 1 covered — **real gap** | 1 rule, both paths covered — **complete** |

The consequence: **BC-08 is where this story earns its keep.** A wrong zone identity does not fail a
build or return a 4xx; it writes a plausible-looking wrong timestamp into a migrated case.

## Depth model

| Tier | Depth | Rationale |
|---|---|---|
| **Unit / component** | **Exhaustive** for BC-08: every `ZonedDateTime` carrier in main code, and both directions of the Jackson round trip. Sufficient-branch for the rest. | Fast, in `mvn test`, no environment. |
| **Build-time assertion** | **Single decisive check** per item (BC-11, BC-21, BC-07, BC-12). | Packaging and code-generation facts, not runtime behaviour. |
| **Integration** | **Authored, not executed.** | Only 2 IT classes exist here against 105 unit tests, and no WildFly 40 image is confirmed available. |

## Scope

- `pcfdlrm-event/pcfdlrm-event-processor` — the two outbound CC converters (BC-08)
- `pcfdlrm-domain/pcfdlrm-domain-aggregate` — `MigratedCaseFileAggregate` (BC-08)
- `pcfdlrm-test-support` — `WholePayloadMatcher` (BC-13, test fidelity)
- `pcfdlrm-command/pcfdlrm-command-api` — access-control DRL harness (BC-03 annotate, BC-20)
- `pcfdlrm-domain/pcfdlrm-domain-event` — generated-type inventory (BC-21)
- `pcfdlrm-viewstore/pcfdlrm-viewstore-liquibase` — `liquibase.properties` (BC-07)
- Module POMs — `javax.json` coordinate inventory (BC-11)
- `docs/j25-parity-checklist.md` — new

Out of scope entirely: `pcfdlrm-viewstore-persistence` (no Java), `pcfdlrm-domain-value-schema`
(no Java, no tests), `pcfdlrm-refdata` and `pcfdlrm-service` (no Bucket A item).

## Requirements

### A. Method — binding on every item below

- **FR1 — Authored on J17 *and executed* on J17.** Every parity test is written against the
  **pre-upgrade `team/25.104.x`** stack — which is `main`'s, byte for byte: `service-parent-pom
  17.104.1`, JDK 17, `centos8-j17` — and **run**. A test not executed on J17 is marked 🟡 and does not count toward
  completion. Where a J17 run contradicts the investigation report, the test pins the **observed**
  behaviour and the contradiction is recorded — the run outranks the report.
- **FR2 — Every parity test names its item.** A one-line reference to its BC identifier and what is
  expected to move, on the test itself.
- **FR3 — Scope is closed.** The items in play are exactly the parity-method ADR's Bucket A for this repo: **BC-03,
  BC-07, BC-08, BC-11, BC-12, BC-13, BC-20, BC-21**. `DLRM-01` is stagingDLRM-only (no Function App
  here). Adding an item requires the parity-method ADR amended. Writing a test for an N/A item — in particular
  anything in the persistence cluster, which has no code to bind to — is a defect in this story.
- **FR4 — Tests use J17 idiom.** `javax` imports, no `jakarta`, no J25-conditional branches.

### B. BC-08 — the primary item

- **FR5 — Pin the zone identity of every `ZonedDateTime` carrier in main code.** BC-08 is that a
  Jackson round trip resolves `'Z'` to `ZoneOffset.UTC` (an offset) where it previously produced a
  region-based `ZoneId`; the two are `equals`-unequal and render differently (`"Z"` vs `"UTC"`).
  *(Revised 2026-09-01. A grep found four `ZonedDateTime` sites; reading the code reduced them to
  **two** true carriers. The other two are not BC-08 carriers: the hearing converter's static
  `OBJECT_MAPPER` only ever serializes a type whose date-like fields are all declared
  `"type": "string"`, so no `ZonedDateTime` passes through it; and
  `MigratedCaseFileAggregate.toDefaultUtcTime` returns a formatted `String`, so nothing crosses a
  Jackson boundary there — that one is a `java.time`/tzdata concern, covered by FR7.)*
  1. **C1 — `ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter`**, which parses
     `dateOfHearing + timeOfHearing` with a formatter carrying `ZoneId.of("UTC")` — a **region** id —
     and assigns the result to `ListHearingRequest.listedStartDateTime`, an **outbound** field.
  2. **C3 — `ProsecutionCaseFileMigratedDefendantToCCDefendantConverter`**, which sets
     `Defendant.courtProceedingsInitiated` from `ZonedDateTime.now(ZoneId.of("UTC"))` — again a region
     id, again outbound. Because `now()` is non-deterministic, the pin is on the **zone identity and
     rendering**, never the instant.
- **FR6 — Pin the round trip, not just the value.** For each carrier, assert the **serialized form**
  and the **deserialized zone identity** through the same `ObjectMapper` the product uses. Asserting
  only the instant would pass on both JDKs while the identity moved underneath — which is precisely
  BC-08's failure mode.
- **FR7 — Pin the `Europe/London` DST arithmetic separately, as a `java.time` concern.**
  `MigratedCaseFileAggregate.toDefaultUtcTime` converts a 10:00 `Europe/London` wall time to UTC and
  **returns a formatted `String`** — no `ZonedDateTime` crosses a Jackson boundary, so this is **not**
  BC-08. Its exposure is JDK `java.time` and tzdata between 17 and 25. Pin at least one GMT date and
  one BST date (the same input yields `10:00:00` in winter and `09:00:00` in summer), so a tzdata or
  resolution change cannot hide inside a single-season fixture. Record it in the checklist as a
  `java.time` pin, not as a BC-08 carrier.
- **FR8 — Pin what reaches the boundary, not only what the converter returns.** BC-08's consequence is
  a wrong timestamp in a migrated case, so the assertion must be on the payload actually handed to
  `cpp-context-prosecution-casefile` and to the `public.pcfdlrm.migrated-case-file-processed` event.

### C. The remaining items

- **FR9 — BC-11: pin JSON-P provider resolution.** Seven `javax.json` coordinates exist across
  domain-aggregate, domain-event, query-view, event-listener and command-handler. Assert that
  **exactly one** JSON-P provider is resolvable on the affected paths, and **which** one. The J25
  failure mode is a `ServiceLoader` collision between glassfish and Parsson.
- **FR10 — BC-13: pin `WholePayloadMatcher`'s comparison behaviour as test infrastructure.** This
  repo has **no everit and no product-side schema-validation seam**; its only `org.json` exposure is
  *through* JSONassert inside `WholePayloadMatcher`. The risk is a whole-payload comparison silently
  changing verdict — a **test-fidelity** risk, not a product-500 risk. Pin the matcher's verdict for a
  representative set of value shapes, including numeric renderings, so the assertion engine DD-43099's
  suites depend on cannot drift unnoticed. Classify it as such in the checklist; do not write it up as
  a product risk.
- **FR11 — BC-03 is annotated; BC-20 is added.** `ReceiveMigratedCaseRuleTest` already covers the
  single DRL rule's allow **and** deny paths — coverage-complete, so BC-03 needs annotation (📝), not
  new tests. BC-20 is a real gap: assert a **non-zero loaded rule count** for the command-API knowledge
  base, so a J25 zero-rule load cannot present as a passing deny test.
- **FR12 — BC-21, BC-07, BC-12: build-time assertions.**
  - **BC-21** — this repo's codegen surface is **larger than stagingDLRM's**: 8 event schemas and
    generator plugins in 8 modules (`pojo` ×4, `catalog` ×3, `messaging-client` ×3,
    `rest-client` ×3). Assert the expected **set of generated types**, derived from the schema
    resources rather than hard-coded.
  - **BC-07** — assert the key set in `liquibase.properties`, so an unsupported key fails in
    `mvn test` rather than in a K8s pre-install job.
  - **BC-12** — `resteasy-multipart-provider` appears only in `pcfdlrm-integration-test`. Record its
    expected scope so the fleet-wide RESTEasy repackaging cannot silently change the IT classpath.

### D. Recording, and boundaries

- **FR13 — A live J17 defect is raised, not fixed here.** If a parity test reveals a defect on the
  current stack, the test pins the **observed** behaviour, the defect is recorded and raised as its own
  ticket, and this PR does not fix it. The parity PR's value is that it is reviewable as "pins existing
  behaviour".
- **FR14 — Bucket B items produce a recorded check, not a test.** BC-14 (`beans.xml`
  `bean-discovery-mode`), BC-15 (core-domain field availability), BC-16 (`/internal/metrics`),
  BC-17 (`stream_error` identity) are framework-owned. Each gets a checklist row stating what was
  checked, the result, and why no context-level test follows.
- **FR15 — `docs/j25-parity-checklist.md` is a deliverable.** One row per Bucket A and Bucket B item,
  carrying the parity-method ADR's legend (🟡 · 🟢 · 🔴 · ⬜ · 📝), the J17 run evidence for every 🟢, and an explicit
  gaps section. It is what the upgrade story reads to know what its gate covers.
- **FR16 — Nothing in this story touches the build's Java target, the parent pom or the CI agent.**
  No version bump, no `jakarta` rename, no change to the `centos8-j17` demand. Those belong to the
  upgrade story. **On this branch layout that is a correctness requirement, not tidiness:** the branch
  is only J17 evidence for as long as nothing has upgraded it, so a stray pom bump here would silently
  invalidate every run this story produces.

## Acceptance criteria

- **AC1** — Each of the eight Bucket A items has either a test executed green on J17 (🟢), or a
  checklist row explaining why it is 📝 or 🔴, with a named reason.
- **AC2** — `mvn clean install -DskipITs` passes on `main` with JDK 17, with every new test executing
  (not skipped, not disabled).
- **AC3** — Both BC-08 carriers (C1, C3) are pinned, each asserting the serialized form **and** the
  deserialized zone identity through a mapper configured by the product's own `ObjectMapperProducer`
  (FR5, FR6).
- **AC4** — `toDefaultUtcTime` is pinned on at least one GMT and one BST date, with the two expected
  outputs differing by an hour, and is labelled a `java.time` pin rather than a BC-08 carrier (FR7).
- **AC5** — At least one assertion is on the payload as it crosses the boundary to
  `cpp-context-prosecution-casefile` or onto `public.pcfdlrm.migrated-case-file-processed` (FR8).
- **AC6** — Each affected module asserts exactly one resolvable JSON-P provider, and names it (FR9).
- **AC7** — The command-API knowledge base asserts a non-zero rule count, and
  `ReceiveMigratedCaseRuleTest` is annotated as an existing BC-03 pin (FR11).
- **AC8** — `docs/j25-parity-checklist.md` exists, covers every BC-01..BC-24 with a legend mark, notes
  `DLRM-01` as ⬜ not-applicable-here, and records the exact command and result for every 🟢.
- **AC9** — Every new test names its BC item (FR2).
- **AC10** — `git diff main` for this PR contains no change under `src/main` except generated-code or
  documentation, and no pom version change (FR13, FR16).
- **AC11** — Any J17 run that contradicts the investigation report is recorded in the checklist with
  both the report's claim and the observed behaviour.

## Out of scope

- Any Java 25, WildFly 40 or Jakarta EE 11 change — the upgrade story, DD-43194-j25-upgrade.
- Cutting `team/25.104.x` — done before this story starts.
- The persistence cluster (BC-01, BC-02, BC-04, BC-05, BC-06, BC-24) — `pcfdlrm-viewstore-persistence`
  contains no Java, so there is nothing to pin.
- `DLRM-01` — stagingDLRM-only; no Function App in this repo.
- BC-09/BC-10 (no Activiti), BC-18 (no `ActiveMQConnectionFactory`), BC-19 (SJP-specific),
  BC-22 (no Tika), BC-23 (no Quartz).
- `cpp-context-stagingdlrm` — DD-43192, its own pipeline.
- Framework and platform repositories — PEG-3296 owns those.
- Fixing any live J17 defect this story surfaces (FR13).
- Executing IT-tier items to green.

## Risks and notes

- **BC-08's failure mode is silent and downstream.** Unlike a validation change, a wrong zone identity
  produces a plausible timestamp on a migrated hearing. FR8 exists because a converter-level assertion
  alone would not catch a payload-level regression.
- **Both BC-08 carriers already use a *region* zone id (`ZoneId.of("UTC")`, whose `toString()` is
  `"UTC"`) rather than `ZoneOffset.UTC` (whose `toString()` is `"Z"`).** That is the exact asymmetry
  BC-08 describes, already present on J17. Whether the current behaviour is itself correct is **not**
  this story's question — FR13 applies: pin what it does, raise anything that looks wrong separately.
- **The investigation report is a hypothesis catalogue, not a specification.** 3 of 24 entries are
  Refuted, 2 Mixed, 2 Inconclusive, and its authors flag fleet-wide counts as directional. FR1 exists
  because the reference context already found one load-bearing claim wrong under a real J17 run.
- **BC-13 here is easy to mis-scope.** It looks like DD-43192's primary item and is not: no everit, no
  product seam, and `pcfdlrm-domain-value-schema` has no tests at all. Scoping it as a product risk
  would spend the story's budget in the wrong module.
- **BC-21's blast radius is larger than stagingDLRM's** — 8 modules run generators here. A hard-coded
  type manifest would be a standing maintenance cost; FR12 says derive it.
- **The `centos8-j17` agent is a dependency of this story, not an incidental.** If it is retired before
  this merges, FR1's execution evidence becomes unobtainable.
- **These tests will not exist on `main`.** Both stages live on `team/25.104.x`, so the shipping J17
  line does not get them. Unlike stagingDLRM there is no coverage gap here worth cherry-picking back —
  BC-03 is already complete on this side (FR11). See the upgrade-mechanics ADR decision 1.
- **Owner unassigned.** `prosecution-casefile-dlrm` shows owner "?" on the PEG-3296 tracker.

## Notes for the design stage

1. **Decide the observation point for each of the two carriers.** C1 returns a parsed value from a
   converter; C3 sets a field from `ZonedDateTime.now(...)` inside one. They may not be observable the
   same way, and FR6's round-trip assertion may need a different seam for each. FR8 also requires at
   least one assertion at the payload boundary rather than on the converter's return value — establish
   whether one seam can serve both.
2. **Establish how to reach a mapper configured the way the product's is.** The hearing converter holds
   a `private static final ObjectMapper` initialised from `new ObjectMapperProducer()`. Decide between
   constructing an equivalent mapper in the test (simpler, but pins a copy of the configuration rather
   than the instance) and reaching the field itself (stronger, but couples the test to the class's
   internals). Check what the class already exposes before assuming reflection is required.
3. **Choose one instrument for the three build-fact items** (FR12) rather than three. Check first
   whether this repo has `maven-enforcer` configured; if not, prefer plain JUnit for the same reasons
   DD-43192 did.
4. **Confirm the `BaseDroolsAccessControlTest` rule-count accessor** before writing FR11's assertion,
   and do **not** copy the investigation report's snippet — it is broken for the kbase≠ksession
   convention, which this repo's `kmodule.xml` uses (`COMMAND_API` / `COMMAND_API_SESSION`).
5. **`WholePayloadMatcher` is not on this branch.** BC-13 is therefore N/A here — see
   [the parity-method ADR](../adrs/DD-43191-j25-parity-method.md) decision 7.
6. **Sequence BC-08 first and give it the review attention.** The remaining seven items are small and
   well understood; if the story is cut, it should be cut from the back.

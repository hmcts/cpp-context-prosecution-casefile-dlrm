# Input brief — J17→J25 behavioural-parity tests for PCFDLRM

> Stage 0 artefact. Feeds [`01-requirements.md`](./01-requirements.md).
> **Self-contained** — everything an SDLC stage needs to run against this story is in this directory.

| | |
|---|---|
| Epic | [DD-43191](https://tools.hmcts.net/jira/browse/DD-43191) — Java 25 upgrade, DLRM contexts |
| Story | [DD-43194](https://tools.hmcts.net/jira/browse/DD-43194) — PCFDLRM, **parity stage** |
| Repo | `cpp-context-prosecution-casefile-dlrm` — **this repo only** |
| Target branch | **`team/25.104.x`**, while it is still J17 — cut from `main` before this story starts, and not yet upgraded |
| Sibling story | [DD-43194 upgrade stage](../DD-43191-DD-43194-j25-upgrade/00-input-brief.md) — same branch, runs **after** this story merges; consumes these tests as its regression gate |
| Platform tickets | PEG-3296 (J25 upgrade), **PEG-3377** (parity testing) |

## The epic this story belongs to

**DD-43191 — Java 25 upgrade, DLRM contexts.** Move `cpp-context-stagingdlrm` and
`cpp-context-prosecution-casefile-dlrm` from Java 17 to Java 25 (WildFly 26.1→40, Jakarta EE 8→10/11,
~30 transitive library bumps), de-risked by behavioural-parity tests written and executed on J17 first.

Four pipelines: parity and upgrade, for each of the two repos. Cross-cutting decisions live in two ADRs,
**single-homed in the stagingDLRM repo and linked not copied** — the precedent ADR-001 set for these two
repos. They are not restated here:

- [**ADR-005 — J25 parity method and BC scope**](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/005-j25-parity-test-method-and-bc-scope.md)
  — parity method and the BC applicability matrix for both repos. **Read this before stage 4.**
- [**ADR-006 — J25 upgrade mechanics**](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/006-j25-branch-milestone-and-funcapp-jdk.md)
  — relevant here only for the branch ordering (its decision 1).

Sibling story in the other repo: [DD-43192](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/DD-43191-DD-43192-j25-parity/00-input-brief.md).
The two are independently deliverable — no shared code, no ordering constraint.

## This story's request

Add parity tests that pin the current J17 behaviour of the seams the J25 upgrade will move, so the
upgrade stage has a real regression gate rather than a suite that passes because nothing recompiled.

Source of candidates: the investigation report in the stagingDLRM repo,
[`j25-behavioural-change-investigation-report.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/j25-upgrade/j25-behavioural-change-investigation-report.md)
— 24 catalogued behavioural changes (17 Confirmed, 3 Refuted, 2 Mixed, 2 Inconclusive).
Reference implementation: `cpp-context-users-groups` PR
[#217](https://github.com/hmcts/cpp-context-users-groups/pull/217) and its `doc/j25-parity-checklist.md`.

**Bucket A for this repo — 7 items** (full matrix and evidence in ADR-005):

| BC | Seam | Weight |
|---|---|---|
| **BC-08** | Jackson `'Z'` → `ZoneOffset.UTC` identity drift — `ZonedDateTime` in **main** code: `MigratedCaseFileAggregate`, and both `…ToCC…Converter` classes building the outbound CC hearing/defendant payloads | **primary** |
| **BC-11** | JSON-P provider collision (glassfish→Parsson) — 7 `javax.json` coordinates across domain-aggregate, domain-event, query-view, event-listener, command-handler | high |
| BC-03 | Drools 7→10 allow/deny — `command-receive-migrated-case-file-api.drl` (1 rule), `ReceiveMigratedCaseRuleTest` | high |
| BC-20 | Drools harness rule-count gate | low (cheap) |
| BC-21 | Codegen (reflections 0.9.10→0.10.2) — all 4 generator plugins + RAML | medium |
| BC-12 | RESTEasy engine swap — `resteasy-multipart-provider` in `pcfdlrm-integration-test` | low (IT tier) |
| BC-13 | org.json/everit strictness — `WholePayloadMatcher` (test support) only | thin |
| BC-07 | Liquibase 4→5 removed properties — `liquibase.properties` | low (deploy blocker) |

**N/A — do not write tests for these.** The whole persistence cluster (BC-01, 02, 04, 05, 06, 24) is
absent: `pcfdlrm-viewstore-persistence` contains **zero Java files** — no `@Entity`, no repository,
only `persistence.xml` and `beans.xml`. Also N/A: BC-09/BC-10 (no Activiti), BC-18 (no
`ActiveMQConnectionFactory`), BC-19 (SJP), BC-22 (no Tika), BC-23 (no Quartz). BC-14/15/16/17 are
framework-tier (Bucket B) — record a check, don't write a test. ADR-005's **DLRM-01** (Function App Jackson parse behaviour) is stagingDLRM-only and does not apply here.

This is why **the reference PR must not be copied file-for-file**: 7 of its 14 files are
viewstore-repository tests for entities this repo does not have.

**Where this repo differs from stagingDLRM.** Its primary target is BC-08, not BC-13. stagingDLRM's job
is JSON-schema validation, so strictness drift dominates there; here the exposure is `ZonedDateTime`
sitting in the aggregate and in the converters that assemble the payload sent on to
`cpp-context-prosecution-casefile`. Do not level the two — effort should follow this split.

## Decisions taken with the requester

| Question | Decision |
|---|---|
| Pipeline shape | Four pipelines — parity + upgrade, per repo. This is the PCFDLRM parity one |
| Target branch | `team/25.104.x` pre-upgrade. Tests must be authored **and executed green on J17**, which the branch still is until the upgrade PR lands — ADR-005 Method 1 |
| Story keys | One Jira key per repo; the two stages share it, distinguished by directory slug |
| Copy the reference PR? | No — its shape is persistence-led and this repo has no persistence layer |
| Checklist location | `docs/j25-parity-checklist.md` (reference used `doc/`; adjusted to this repo's convention) |
| Shared ADRs | Single-homed in stagingDLRM, linked from here — never copied. Precedent: ADR-001 |

## Scope boundaries

| In scope | Out of scope |
|---|---|
| Parity tests for the Bucket A BCs, J17-style `javax` imports | Any version bump, jakarta rename, or pom change — that is the upgrade story, and doing it here would destroy this story's J17 evidence |
| `docs/j25-parity-checklist.md` — scope, status, J17 run evidence | Cutting `team/25.104.x` — done before this story starts |
| Extending `ReceiveMigratedCaseRuleTest` for BC-03/BC-20 | `cpp-context-stagingdlrm` — DD-43192, its own pipeline |
| Recorded Bucket B checks (no test files) | Framework/platform repos — PEG-3296 owns those |
| Editing the shared ADRs (do it in stagingDLRM) | Copying the ADRs into this repo |

## Known blockers / open items

- **The `centos8-j17` agent must still be available.** This story deliberately does not change the
  pipeline — the branch must stay on the J17 agent for this story's runs to be J17 evidence. The
  upgrade story moves it to `ubuntu-j25`.
- **BC-08 needs the serialization seam pinned, not just the field.** The divergence is
  region-vs-offset `ZoneId` identity (`equals`/`toString`), which only shows through the shared
  `ObjectMapperProducer`. The reference's `UserRoleActivatedDateJacksonZoneParityTest` is the pattern
  to follow; choosing the carrier field here is a stage-2 call.
- **The report may be wrong in places, and a J17 run outranks it.** In the reference context three
  BC-04 tests were written to the report's claim and refuted by an actual J17 run. Expect at least one
  such correction here and record it.
- **Only 2 IT classes in this repo** against 105 unit tests. IT-tier parity items are authored-not-
  executed until Docker is available — mark them 🟡, not 🟢.
- Owner is unassigned for `prosecution-casefile-dlrm` on the PEG-3296 tracker. Confirm with Platform
  Engineering that DLRM is unowned rather than queued.

# J17 → J25 behavioural-parity checklist — pcfdlrm

> Deliverable of DD-43194 (T8), per `docs/pipeline/DD-43191-DD-43194-j25-parity/01-requirements.md`
> FR15/AC8. Legend per ADR `DD-43191-j25-parity-method`: 🟢 executed green on J17 · 🟡 authored, not
> executed · 🔴 not attempted, reason given · ⬜ not applicable to this repo · 📝 checked, recorded,
> no test needed.
>
> This is what the upgrade story (`DD-43191-DD-43194-j25-upgrade`) reads to know what its regression
> gate actually covers. Every 🟢 below was run on `team/25.104.x`-equivalent J17 (`service-parent-pom
> 17.104.1`-line, JDK 17) on 2026-09-07. Full task-by-task rationale lives in `03-stories.md`; this
> file records outcomes, not the reasoning behind them.

**Whole-suite gate (AC2)**: `mvn -o clean install -DskipITs` → BUILD SUCCESS, 51 modules, 557 tests,
0 failures, 0 skipped.

**Production-code footprint (AC10)**: `git status --porcelain | grep -E "src/main|pom.xml"` → empty.
Whole story's diff is docs plus 3 `src/test` files (T1's two converter tests, T2's comment-only
change to `MigratedCaseFileAggregateTest`).

## Bucket A — this repo's seams

| BC | Item | Status | Evidence |
|---|---|---|---|
| **BC-08** | `ZonedDateTime` zone-identity drift on the outbound CC payload (primary item) | 🟢 | `mvn -pl pcfdlrm-event/pcfdlrm-event-processor test`, 0 failures (`shouldPinZoneIdentityOnListedStartDateTime`, `shouldPinZoneIdentityOnCourtProceedingsInitiated` — each pins the round-trip read side; the write-side "bare Z" assertion was removed from both, verified non-discriminating on JDK17/Jackson 2.13.5 since both `ZoneId.of("UTC")` and `ZoneOffset.UTC` serialize identically with `WRITE_DATES_WITH_ZONE_ID` off — see contradictions). `mvn -pl pcfdlrm-domain/pcfdlrm-domain-aggregate test -Dtest=MigratedCaseFileAggregateTest`, 0 failures (FR7 `java.time`/DST pin, pre-existing, annotated — not a BC-08 carrier). **FR8/AC5's payload-boundary assertion is 🔴, cut**: the LIBRA-only `WholePayloadMatcher` seam it needs doesn't exist on `team/25.104.x`. **Two carriers pinned, not the whole set** — `MigratedCaseFileAggregate.java:340`'s `ZonedDateTime.now()` (JVM default zone) feeding `MaterialAdded.receivedDateTime` is untested by design (environment-dependent). See `03-stories.md` T1/T2 |
| BC-11 | JSON-P null-guard parity (corrected 2026-08-26 — was: provider collision) | 📝 | `JsonObjectsNullGuardParityTest` written, ran green, then removed on review: it exercises the JSON-P provider's own null-guard contract (no pcfdlrm class on the call stack), and BC-11 is already recorded as refuted — J17 and J25 throw identically, so this pinned a behaviour in a library this repo doesn't own, known not to drift. See `03-stories.md` T3 |
| BC-03 | Drools 7→10 rulebase recompile (refuted) | 📝 | Refuted per `java-25-parity.pdf`'s catalogue: rules unchanged and fail-closed, independently verified — one of only two catalogue-critical-rated entries, both refuted (the other is BC-14). `ReceiveMigratedCaseRuleTest` already covers this repo's single DRL rule's allow **and** deny paths (2/2 green, `mvn -pl pcfdlrm-command/pcfdlrm-command-api test`). Not annotated in-file — that file has CR-only line endings, so any edit renders as a whole-file diff. See `03-stories.md` T4 |
| BC-20 | Drools test harness 0-rule false confidence | 📝 | `CommandApiKnowledgeBaseRuleCountTest` written, ran green, then removed on review: redundant with `ReceiveMigratedCaseRuleTest`, which a throwaway zero-rule kbase experiment confirmed already fails three independent ways. See `03-stories.md` T4 |
| BC-21 | `reflections` scanning-contract change → codegen type set | 📝 | Verified per module rather than assumed: `02-design.md` §G's plan ("8 modules, 8 schemas, one package prefix, filename-derived class names") has four wrong premises — see contradictions. Net result: 8 of `domain-event`'s 9 event types and 2 of `command-handler`'s 3 command types are compile-covered via main-source imports. The one real gap: `command-api`/`event-processor`'s 23 framework-generated classes (JAX-RS/JMS adapters) are referenced from no main/test source, so a codegen regression there would build green — not tested, since that would pin a third-party generator's naming contract (same objection as BC-11); see gaps. `HearingValidationFailed` and one `command-handler` duplicate are dead code, nothing to protect. See `03-stories.md` T8 |
| BC-07 | Liquibase 4→5 removed properties | 📝 | `LiquibasePropertiesKeyInventoryTest` written, ran green, validated against Liquibase's own defaults-file validator rather than the raw properties file — correct polarity confirmed against real 4.10.0/5.0.4 jars. Still reverted, to match PR #26/T3/T4 precedent and avoid this module's first test-dependency footprint. Premise correction: an unsupported key only logs a warning, it doesn't crash the K8s pre-install job — see contradictions. See `03-stories.md` T5 |
| BC-13 | `org.json`/JSONassert strictness shift (`WholePayloadMatcher`) | 🔴 | Cut at story-planning stage (T6). Test-fidelity risk only — no product-side schema-validation seam in this repo (no everit anywhere). See `03-stories.md` T6 |
| BC-12 | `resteasy-multipart-provider` scope in `pcfdlrm-integration-test` | 🔴 | Cut at story-planning stage (T7). IT-tier; only 2 IT classes exist and no WildFly 40 image confirmed available. See `03-stories.md` T7 |

## Bucket B — framework-tier, recorded checks only (FR14)

All four rows below are **📝** (checked, recorded, no context-level test).

| BC | Item | What was checked | Result |
|---|---|---|---|
| BC-14 | CDI 4 discovery-mode vs. legacy `beans.xml` | All 12 `beans.xml` files in this repo | All at `version="1.1" bean-discovery-mode="all"` today. Refuted item; migrating to the 4.0 namespace is the upgrade story's job |
| BC-15 | core-domain forked from a stale base | `coredomain.version` in root `pom.xml` | Pinned at `17.104.4`. Whether this is the affected stale base is a Platform Engineering / PEG-3296 question, not determinable from this repo alone |
| BC-16 | `/internal/metrics/*` surface reduction | Repo-wide grep for `internal/metrics` | No reference found anywhere — no evident dependency to break |
| BC-17 | `stream_error` identity/dedup-hash change | Repo-wide grep for `stream_error` | No reference in this repo's code (only in this story's own requirements doc, quoting the generic guide) |

## N/A — no binding site in this repo

Every row below is **⬜** (not applicable to this repo).

| BC | Reason |
|---|---|
| BC-01, BC-02, BC-04, BC-05, BC-06, BC-24 | The Hibernate/DeltaSpike persistence cluster. `pcfdlrm-viewstore-persistence` has zero Java files — only `persistence.xml`/`beans.xml` |
| BC-09, BC-10 | No Activiti/Camunda in this repo |
| BC-18 | No `ActiveMQConnectionFactory` usage |
| BC-19 | SJP-specific; not applicable |
| BC-22 | No Apache Tika / file-upload MIME detection in this repo |
| BC-23 | No Quartz scheduler in this repo |
| **DLRM-01** | stagingDLRM-only (Function App Jackson parse behaviour) — no Function App in this repo |

## Contradictions vs. the investigation report / generic guide (AC11)

- **BC-11**: originally catalogued as a JSON-P `ServiceLoader` collision between glassfish and Parsson, weighted **high**. Refuted by a real J17/J25 run on `cpp-context-notification-notify` (corrected 2026-08-26); this repo sits on the same refuted code path. Re-weighted to **thin** and re-scoped — see `01-requirements.md`'s FR9 revision.
- **BC-08**: `02-design.md` §A1/§A2 assumed a region `ZoneId` would render distinguishably from an offset (e.g. bracketed `[UTC]`). Measured: both carriers serialize as a bare `Z`, indistinguishable from `ZoneOffset.UTC` — identity only diverges on the round-trip read side, where `…Z` resolves back to the region `ZoneId.of("UTC")`, not an offset. (`ZonedDateTime.toString()` does carry `[UTC]`; it's the Jackson rendering that doesn't.) Tests pin the observed behaviour, not the design's guess.
- **BC-20**: `02-design.md` §C's claim that `StatelessKieSession` doesn't expose `KieBase` doesn't hold on this repo's `kie-api 7.69.0.Final` (verified via `javap`) — not wrong in general, just not applicable to this pinned version.
- **BC-07**: `01-requirements.md` FR12/`02-design.md` §D describe an unsupported key as crashing the K8s pre-install job. Measured: Liquibase only throws under `GlobalConfiguration.STRICT`, which this repo never sets — non-strict just logs a warning, on both 4.10.0 and 5.0.4. Real cost is stale config nobody is forced to notice, not a broken deploy. (5.0.4 does genuinely drop `liquibase.hub.mode` from its defaults — that half of the claim held.)
- **BC-21**: `01-requirements.md` FR12/`02-design.md` §G's implementation plan has four wrong premises: schema count (9, not 8), module count/kind (9 poms, one an aggregator, two catalog-only with zero generated `.java`), "own package prefix" isn't one prefix (one `domain-event` schema's `id` lands in a different package than the other eight), and the class set isn't derivable from filenames (41 classes for 8 schemas — 33 are nested sub-schema types). See the BC-21 row and `03-stories.md` T8.

## Gaps

- **BC-13, BC-12** — not attempted this story (🔴, cut at story-planning). No coverage exists for these on `team/25.104.x`.
- **BC-08 / FR8, AC5** — the payload-boundary assertion is 🔴, cut: no whole-payload seam exists in `MigratedCaseReceivedProcessorTest` on this branch. BC-08 is pinned at converter level only.
- **BC-21** — the `command-api`/`event-processor` generated-adapter classes (23 total) are the one real, uncovered gap: nothing in `mvn install` would notice if the generators emitted none of them. Not tested by choice (would pin a third-party generator's naming contract). The upgrade story should check the built WAR for these classes by hand after the first J25 build.
- **BC-20** — the 0-rule net that exists today is incidental (rests on `verify()`/`eval()` and strict-stub behaviour, not on any rule-presence assertion), so it's fragile to an unrelated refactor of `ReceiveMigratedCaseRuleTest`.
- **BC-15, BC-16, BC-17** — recorded via a repo-wide grep only; not independently verified against a running system.
- **BC-08**'s third carrier (`MigratedCaseFileAggregate.java:340`, JVM default zone) is recorded but deliberately untested.
- **BC-07** — no test kept; `liquibase.properties`'s key set is unpinned, and the changelog itself is empty, so nothing here would catch a changelog-level Liquibase 5 change once read-model wiring lands. `liquibase.hub.mode`'s removal remains the upgrade story's job (FR16).

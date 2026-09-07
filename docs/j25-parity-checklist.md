# J17 → J25 behavioural-parity checklist — pcfdlrm

> Deliverable of DD-43194 (T8), per `docs/pipeline/DD-43191-DD-43194-j25-parity/01-requirements.md`
> FR15/AC8. Legend per ADR `DD-43191-j25-parity-method`: 🟢 executed green on J17 · 🟡 authored, not
> executed · 🔴 not attempted, reason given · ⬜ not applicable to this repo · 📝 checked, recorded,
> no test needed.
>
> This is what the upgrade story (`DD-43191-DD-43194-j25-upgrade`) reads to know what its regression
> gate actually covers. Every 🟢 below was run on `team/25.104.x`-equivalent J17 (`service-parent-pom
> 17.104.1`-line, JDK 17) on 2026-09-04.

## Bucket A — this repo's seams

| BC | Item | Status | Evidence |
|---|---|---|---|
| **BC-08** | `ZonedDateTime` zone-identity drift on the outbound CC payload (primary item) | 🟢 | `mvn -pl pcfdlrm-event/pcfdlrm-event-processor test` → 180 run, 0 failures (`shouldPinZoneIdentityOnListedStartDateTime`, `shouldPinZoneIdentityOnCourtProceedingsInitiated`, plus the narrowed `MigratedCaseReceivedProcessorTest` boundary assertion). `mvn -pl pcfdlrm-domain/pcfdlrm-domain-aggregate test` → 334 run, 0 failures (FR7 `java.time` pin, pre-existing, annotated) |
| BC-11 | JSON-P null-guard parity (corrected 2026-08-26 — was: provider collision) | 🟢 | `mvn -pl pcfdlrm-event/pcfdlrm-event-processor test` (`JsonObjectsNullGuardParityTest`, included in the 180 above) |
| BC-03 | Drools 7→10 rulebase recompile (refuted) | 📝 | Refuted per `java-25-parity.pdf`'s catalogue: rules unchanged and fail-closed, independently verified — one of only two catalogue-critical-rated entries, both refuted (the other is BC-14). `ReceiveMigratedCaseRuleTest` already covers this repo's single DRL rule's allow **and** deny paths (2/2 green, `mvn -pl pcfdlrm-command/pcfdlrm-command-api test`); annotated with a BC-03 reference rather than duplicated, per FR11 — paired with BC-20's rule-count guard so a zero-rule load can't masquerade as a passing deny test |
| BC-20 | Drools test harness 0-rule false confidence | 🟢 | `mvn -pl pcfdlrm-command/pcfdlrm-command-api test` → 4 run, 0 failures (`CommandApiKnowledgeBaseRuleCountTest`) |
| BC-21 | `reflections` scanning-contract change → codegen type set | 🟢 (narrowed) / 📝 (remainder) | `mvn -pl pcfdlrm-domain/pcfdlrm-domain-event test` → 1 run, 0 failures (`GeneratedEventTypeInventoryTest`, all 8 schemas across both real package roots). The other 7 generator-plugin modules (`domain-aggregate`, `domain-event-processor`, `domain-value-schema`, `event`, `event-processor`, `command-handler`, `command-api`) are 📝 — not given an equivalent test this story; see Gaps |
| BC-07 | Liquibase 4→5 removed properties | 🟢 | `mvn -pl pcfdlrm-viewstore/pcfdlrm-viewstore-liquibase test` → 1 run, 0 failures (`LiquibasePropertiesKeyInventoryTest`). Note: `pcfdlrm.xml`'s changelog is currently empty (no changesets, no tables) — this is a deploy-job config risk (Liquibase itself can refuse to start on an unsupported key), not a data-migration risk, since there is no real viewstore schema here yet |
| BC-13 | `org.json`/JSONassert strictness shift (`WholePayloadMatcher`) | 🔴 | Cut at story-planning stage (T6). Test-fidelity risk only — no product-side schema-validation seam in this repo (no everit anywhere) — versus BC-07/BC-20's confirmed-live exposure and BC-03/BC-11's near-zero cost. See `03-stories.md` T6 |
| BC-12 | `resteasy-multipart-provider` scope in `pcfdlrm-integration-test` | 🔴 | Cut at story-planning stage (T7). IT-tier; only 2 IT classes exist and no WildFly 40 image is confirmed available, so it would have landed 🟡 (authored-not-executed) even if written — lowest marginal value of the remaining items. See `03-stories.md` T7 |

## Bucket B — framework-tier, recorded checks only (FR14)

| BC | Item | What was checked (2026-09-04) | Result |
|---|---|---|---|
| BC-14 | CDI 4 discovery-mode vs. legacy `beans.xml` | All 12 `beans.xml` files in this repo (`grep -rl beans.xml`) | All at `version="1.1" bean-discovery-mode="all"` today. Refuted item (not reproduced on the migrated runtime per the corrected guide); migrating to the 4.0 namespace is mechanical work owned by the upgrade story, not this one |
| BC-15 | core-domain forked from a stale base | `coredomain.version` in root `pom.xml` | Pinned at `17.104.4`. Whether this is the affected stale base is a Platform Engineering / PEG-3296 tracking question — not independently determinable from this repo alone; not re-litigated here |
| BC-16 | `/internal/metrics/*` surface reduction | `grep -rl "internal/metrics"` across the whole repo | No reference found anywhere in this repo's code or config — no evident dependency on this endpoint to break |
| BC-17 | `stream_error` identity/dedup-hash change | `grep -rl "stream_error"` across the whole repo | No reference found in this repo's code (only in this story's own requirements doc, which quotes the generic guide) — framework-owned, no pcfdlrm-side binding site identified |

## N/A — no binding site in this repo

| BC | Reason |
|---|---|
| BC-01, BC-02, BC-04, BC-05, BC-06, BC-24 | The Hibernate/DeltaSpike persistence cluster. `pcfdlrm-viewstore-persistence` has **zero Java files** (confirmed 2026-09-04: `find ... -name "*.java" | wc -l` → 0) — only `persistence.xml`/`beans.xml`. Nothing to bind a test to |
| BC-09, BC-10 | No Activiti/Camunda in this repo |
| BC-18 | No `ActiveMQConnectionFactory` usage |
| BC-19 | SJP-specific; not applicable |
| BC-22 | No Apache Tika / file-upload MIME detection in this repo |
| BC-23 | No Quartz scheduler in this repo |
| **DLRM-01** | stagingDLRM-only (Function App Jackson parse behaviour) — no Function App in this repo |

## Contradictions vs. the investigation report / generic guide (AC11)

- **BC-11**: originally catalogued (and carried into this repo's `00-input-brief.md`) as a JSON-P `ServiceLoader` collision between glassfish and Parsson, weighted **high**. Refuted by a real J17/J25 run on `cpp-context-notification-notify` (`java-25-parity.pdf`, corrected 2026-08-26); this repo's own code was checked and found to sit on the same refuted code path (no raw `javax.json.Json` provider call anywhere in `src/main`, only the shared `JsonObjects` helper). Re-weighted to **thin** and re-scoped to a null-guard parity pin — see `01-requirements.md`'s FR9 revision.
- **BC-08**: the design stage (`02-design.md`) assumed a region `ZoneId` would render distinguishably from an offset at serialization time (e.g. bracketed as `[UTC]`). The actual J17 run (2026-09-04) showed **both carriers serialize as a bare `Z`**, indistinguishable from `ZoneOffset.UTC` on the wire — the identity only diverges on the **round-trip read side**, where "…Z" currently resolves back to the region `ZoneId.of("UTC")` (confirmed twice, independently, on both carriers), not an offset. The tests were corrected in place to pin the observed behaviour rather than the original guess.

## Gaps

- **BC-13, BC-12** — not attempted this story (🔴, cut at story-planning). No coverage exists for these on `team/25.104.x` as a result of this story.
- **BC-21** — only `pcfdlrm-domain-event` (8 schemas) has an inventory test. The other 7 generator-plugin modules are unguarded against a `reflections` scanning-contract change; 📝 recorded, not tested.
- **BC-15, BC-16, BC-17** — recorded via a repo-wide grep only; not independently verified against a running system (no WildFly 40 image, no live healthcheck).
- **BC-20**'s rule-count accessor (`KieServices.get().getKieClasspathContainer().getKieBase(...)`) was confirmed against the real `access-control-test-utils`/`kie-api` 7.69.0.Final jars via `javap` before writing the test — not copied unverified from the investigation report, per this story's own method.

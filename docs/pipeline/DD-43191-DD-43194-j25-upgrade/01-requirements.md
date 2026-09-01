# Requirements — DD-43194: Java 25 / WildFly 40 / Jakarta EE 11 upgrade of PCFDLRM

> Stage 1 artefact (requirements). Source: [`00-input-brief.md`](./00-input-brief.md).
> Requirements altitude — nothing here prescribes a class layout. Implementation **tasks** come from
> the design / story-writer stage.
>
> **Scoped to `cpp-context-prosecution-casefile-dlrm`, branch `team/25.104.x`.** The stagingDLRM half
> is [DD-43192](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/DD-43191-DD-43192-j25-upgrade/01-requirements.md).
> Mechanics are fixed by [ADR-006](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/006-j25-branch-milestone-and-funcapp-jdk.md),
> single-homed in the stagingDLRM repo; this document does not restate it, it makes it testable.

## Story

**[DD-43194](https://tools.hmcts.net/jira/browse/DD-43194) — Upgrade PCFDLRM to Java 25 / WildFly 40 /
Jakarta EE 11**

| | |
|---|---|
| Epic | [DD-43191](https://tools.hmcts.net/jira/browse/DD-43191) — Java 25 upgrade, DLRM contexts |
| Size | **M–L** — comparable Java churn to DD-43192 but no Azure Functions module |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Target branch | **`team/25.104.x`**, cut from `main` before the parity story; this story is the second PR onto it |
| Depends on | **[DD-43194 parity stage](../DD-43191-DD-43194-j25-parity/00-input-brief.md) merged to `team/25.104.x`** — hard, see FR1. Plus [ADR-006](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/006-j25-branch-milestone-and-funcapp-jdk.md) accepted |
| Sibling story | [DD-43192](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/DD-43191-DD-43192-j25-upgrade/00-input-brief.md) — same stage in stagingDLRM, independently deliverable |
| Production changes | **yes** — this is the upgrade |
| Platform ticket | PEG-3296 |

### Summary (JIRA summary line)

`[Java 25] Upgrade PCFDLRM to 25.104.0-M10 / WildFly 40 / Jakarta EE 11 and produce a QA Docker image`

### User story

As a **CPP platform engineer retiring the Java 17 estate**,
I want **PCFDLRM building, deploying and behaving identically on Java 25, WildFly 40 and Jakarta EE 11 —
with the DD-43194 parity tests still green and a QA Docker image published**,
so that **the DLRM migration pipeline is not the reason the platform has to keep a Java 17 runtime
alive, and the date-time values this context sends on to `cpp-context-prosecution-casefile` and
Progression are provably unchanged rather than plausibly wrong**.

## Reference and sizing

The reference upgrade is **`cpp-context-prosecution-casefile`** commit **`122a5a8fdc`** on
`team/25.104.x` (401 files, +5870/−5018) — which is the very context this one forwards to, so its shape
is the closest available match. Its **shape** is the template; its version pins are three milestones
stale and must not be copied (ADR-006 decision 2).

Measured surface in this repo, at `main`:

| Surface | This repo | stagingDLRM | Reference |
|---|---|---|---|
| `javax.*` import lines to migrate | **94**, across **58 files** | 92 / 40 files | 544 |
| `javax.json` | 44 | 53 | 220 |
| `javax.inject` | 35 | 11 | 155 |
| `javax.ws.*` | 7 | 22 | 15 |
| `javax.annotation.PostConstruct` | 5 | 4 | — |
| `javax.enterprise` | 3 | 1 | 32 |
| `javax.persistence` | **0** — no JPA code | 0 | 118 |
| `javaee-api` declarations | 8 modules + **6 generator-plugin dependency blocks** | 9 + 2 | — |
| `beans.xml` to migrate | 12, all legacy `xmlns.jcp.org` namespace | 10 | 12 |
| `.drl` | 1 | 1 | 2 |
| Generator-plugin modules | **8** | fewer | — |

Two asymmetries with DD-43192 worth planning around: this repo spreads its `javax` churn over **more
files** (58 vs 40) with more `javax.inject`, and it has **three times the plugin-internal `javaee-api`
surface** — 6 blocks across `event-processor`, `command-handler` and `command-api`. Against that, it
has **no Azure Functions module**, which is where DD-43192's uncertainty concentrates.

## Requirements

### A. Branch, versions, and the gate

- **FR1 — Do not start until the DD-43194 parity PR has merged into `team/25.104.x`.** Both stages
  share the branch, and the parity tests are only J17 evidence for as long as nothing has upgraded it.
  This story is what ends that state, so starting early would retroactively invalidate the gate it
  depends on (ADR-005 Method 1, ADR-006 decision 1). The branch itself is cut from `main` before the
  parity story, not by this one.
- **FR1a — Migrate the parity tests as part of this story's `javax`→`jakarta` sweep.** They are source
  files on the same branch and are swept like any other. This is the reason the single-branch layout was
  chosen: the tests are authored once. Their **assertions must not change** — only their imports.
- **FR2 — Target the latest platform milestones, verified at implementation time.**
  `cpp-platform-maven-service-parent-pom` `17.104.1` → **`25.104.0-M10`**; `cpp-platform-core-domain`
  → **`25.104.0-M11`**. These are the tracker's figures as at 06 Aug 2026. **Reconfirm before
  starting** — if they have advanced, take the newer.
- **FR3 — The DD-43194 parity tests must be green on Java 25 at the end of this story.** A parity test
  that goes red is a **finding, not a test to relax**. This matters more here than in DD-43192: the
  parity tests pin `ZonedDateTime` zone identity on payloads that cross into another context, so a red
  test means migrated hearing data would have been silently wrong. If one cannot be made green, the
  story stops and the divergence is raised.
- **FR4 — Interface pins move only as far as the enforcer requires.** The reference bumped
  `referencedata`, `progression` and `sjp` purely to satisfy the latest-interfaces enforcer. Bump this
  repo's pins to what the enforcer demands and no further.
- **FR5 — Decide the upstream `prosecution-casefile` pin deliberately.** This context forwards to
  `cpp-context-prosecution-casefile`, which is already merged on `team/25.104.x` with a QA image at
  `25.104.3-M4-SNAPSHOT`. Choose between pinning its **released** Java 25 version and staying on the
  J17 interface, and record the reasoning. The tracker shows `mi-reportdata` hitting exactly this
  problem — merged against a `-M1-SNAPSHOT` and needing a follow-up bump to the released version.

### B. The Jakarta EE migration

- **FR6 — Migrate only the Jakarta EE namespaces.** `javax.json` → `jakarta.json`, `javax.inject` →
  `jakarta.inject`, `javax.ws.*` → `jakarta.ws.*`, `javax.annotation.PostConstruct` →
  `jakarta.annotation.PostConstruct`, `javax.enterprise` → `jakarta.enterprise`.
- **FR7 — Do not rename JDK `javax.*` packages.** No JDK-namespace `javax` import was found in this
  repo at scan time, so a blanket replace would happen to work **today** — which is precisely why this
  requirement exists. `javax.net.ssl`, `javax.crypto`, `javax.xml.parsers` and `javax.naming` are JDK
  API with no `jakarta` equivalent, and one appearing between now and implementation would break the
  build. Migrate package-by-package against an explicit allowlist. *(DD-43192 has a live instance:
  `javax.net.ssl.SSLContext`.)*
- **FR8 — Swap `javaee-api` for the Jakarta equivalent everywhere it is declared, including inside
  plugin `<dependencies>`.** Eight modules declare it as a normal dependency, and **six further
  declarations sit inside generator-plugin dependency blocks** — `event-processor` (×2),
  `command-handler` (×4 across two plugins), `command-api` (×2). The plugin-internal ones are the ones
  that get missed, and they fail at code-generation time with a message that does not name them. **This
  repo has three times stagingDLRM's exposure here.**
- **FR9 — Migrate the CDI and persistence descriptors.** All 12 `beans.xml` are on the legacy
  `http://xmlns.jcp.org/xml/ns/javaee` namespace, and `persistence.xml` declares
  `http://java.sun.com/xml/ns/persistence` version `1.0`. Move to the Jakarta namespaces and current
  versions. **`bean-discovery-mode="all"` must be preserved explicitly on all 12** — it is already
  present, and it is the sole reason BC-14's *Refuted* verdict holds. Losing it would empty the
  interceptor chains silently.

### B2. Dependencies that will not resolve on the 25.104.x chain

*Added at stage 1 review, after a dependency scan. Numbered after the existing requirements rather than
renumbering them, so earlier references stay valid.*

- **FR21 — `pcfdlrm-viewstore-persistence` declares four coordinates that no longer exist on the Java 25 chain.**
  This is a **hard dependency-resolution failure**, not a behaviour change: `mvn clean install` will fail
  on this module before compiling anything. All four are in pcfdlrm-viewstore-persistence's pom (`pom.xml:21`, `:99`, `:104`, `:109`, `:119`, `:130`):
  1. `uk.gov.justice.services:persistence-deltaspike` — the module was **removed from the framework
     reactor and BOM**; its four non-DeltaSpike classes were relocated to a new `persistence-jpa` module
     in `cp-microservice-framework` 25.104.0-M3.
  2. four `org.apache.deltaspike.*` test artifacts (`deltaspike-test-control-module-api`/`-impl`, `deltaspike-cdictrl-openejb`,
     and `deltaspike-cdictrl-api` where present) — **DeltaSpike is removed entirely** from
     `cp-maven-common-bom` (8 artifacts), so these have no managed version.
  3. `org.hibernate:hibernate-entitymanager` — **removed in Hibernate 6**, merged into `hibernate-core`;
     the groupId also moved to `org.hibernate.orm`.
  4. `src/test/resources/META-INF/apache-deltaspike_test-container.properties` — the reference upgrade
     **deleted this exact file** and added a test `persistence.xml` in its place. This repo has no test
     `persistence.xml`.
- **FR22 — Prefer deletion over migration here, and justify whichever is chosen.**
  `pcfdlrm-viewstore-persistence` **contains no Java code at all** — no `@Entity`, no repository, no test. The DeltaSpike
  and Hibernate 5 test scaffolding exists to support tests that do not exist. So the reference's
  migration path (swap `persistence-deltaspike` → `persistence-jpa`, replace the properties file with a
  test `persistence.xml`) would faithfully reproduce scaffolding for nothing.
  **Establish first whether the module is needed at all** — it ships a `persistence.xml` and a
  `beans.xml` that the runtime may still require for the view-store datasource wiring, in which case the
  module stays and only its dependencies go. Record the reasoning either way; a module deleted by
  accident is worse than a dependency list trimmed too cautiously.

### C. Code generation

- **FR10 — Apply the two recorded generator fixes across all eight generator modules.**
  - `messaging-client-generator-plugin` needs **parsson** in its plugin dependencies (as
    `cpp-context-system-scheduling` found) — it runs in `pcfdlrm-event`, `event-processor` and
    `command-api`;
  - `rest-client-generator-plugin` needs the **jakartaee-api** swap (as
    `cpp-context-system-announcement` found) — it runs in `event-processor`, `command-handler` and
    `command-api`.
- **FR11 — The generated-artefact inventory must not shrink.** BC-21's `reflections` 0.9.10→0.10.2
  scanning-contract change alters what is discovered. The parity story's derived inventory assertion
  (its FR12) must still pass across all 8 event schemas; a silently smaller set of generated types is a
  defect in this story. **The blast radius here is larger than stagingDLRM's** — `pojo` ×4,
  `catalog` ×3, `messaging-client` ×3, `rest-client` ×3.

### D. Build, pipeline and packaging

- **FR12 — Move the build to the Java 25 track.** `azure-pipelines.yaml:29` demands
  `identifier -equals centos8-j17` → **`ubuntu-j25`**; repoint the template ref to the **`wildfly40`**
  track and set `aksDeployBranch` accordingly.
- **FR13 — `jacoco` must be at 0.8.14 or later.** The parent's 0.8.12 does not handle JDK 25 bytecode;
  every migrated context has needed a local override.
- **FR14 — Assess whether a `jboss-deployment-structure.xml` is needed.** This repo has **none**, and
  the reference *added* one at its root. Determine whether the WildFly 40 module set requires it — and
  if one is added, **check it against BC-12**, because a descriptor that disables the `jaxrs` subsystem,
  combined with the fleet-wide `packagingExcludes` stripping bundled RESTEasy, is the report's flagged
  possible deploy-breaker.
- **FR15 — Establish where this repo's container image comes from before assuming nothing is needed.**
  There is no `Dockerfile` at this repo's root, so the fleet's "Dockerfile base → Ubuntu 24.04, remove
  the RHEL `yum` lines" item has no obvious target here — but that must be **confirmed, not assumed**.
  Three facts to reconcile: `context-validation.yaml`'s image step is gated only by a repo-name
  exclusion list that **does not exclude this repo**, so the image step *will* run; its `dockerfilePath`
  parameter defaults to `'Dockerfile'`; and the reference context does ship one, at
  `docker/Dockerfile_prosecutioncasefile-service` — not at root. Meanwhile `support`,
  `system-id-mapper` and `notification` have no `Dockerfile` at all and all three have QA images.
  **Determine which of those patterns applies here**, then either fix the file the `wildfly40` track
  needs or record that none exists. Do not create a `Dockerfile` to satisfy a checklist, and do not
  assume the template covers it.
- **FR16 — `h2` test-scope bump — likely a no-op here, confirm and move on.** The fleet move is
  `1.4.196` → `2.3.232`, **dropping the MVCC/MV_STORE URL settings** (as `cpp-context-notification`
  found). `pcfdlrm-viewstore-persistence` declares `h2` at test scope with **no local version** (managed
  by the BOM), and its only JDBC URL is `jdbc:h2:mem:test` in
  `apache-deltaspike_test-container.properties` — **no MVCC or MV_STORE setting**, and that file is
  deleted by FR21 anyway. So there is probably nothing to do. Confirm rather than assume, and do not
  spend time on it.

### E. Known defects to fix in this story

- **FR17 — Delete `liquibase.hub.mode: off` from `liquibase.properties`.** Finding F1 from the parity
  story's stage 2. Liquibase Hub was sunset and its configuration removed; on Liquibase 5.0.3 this is an
  unknown-parameter failure in the **K8s pre-install migration job**, before any application code runs.
  It is a live deploy blocker on this branch. **Also verify `liquibase.headless`** the same way — lower
  confidence — by running Liquibase 5 against the file. *(The same line exists in stagingDLRM; both need
  it.)*
- **FR18 — Check the 8 missing core-domain fields *before* bumping `coredomain`.** Finding F3.
  **This is a sharper risk here than in stagingDLRM**: this context constructs
  `uk.gov.justice.core.courts.Defendant` and `ListHearingRequest` **directly**, so if any of the 8
  fields BC-15 records as absent from the J25 core-domain line sit on those types, the upgrade breaks at
  compile time. Recoverable, but far cheaper to check first than to discover.

### F. Definition of done

- **FR19 — Done is a QA Docker image, not a merged PR.** Nine contexts on the tracker are "Merged — no
  Docker image produced (build failed)", several still carrying an open pipeline-revert or Docker-fix PR.
  Budget for a follow-up pipeline/image PR as part of this story.
- **FR20 — Integration tests must pass on the Java 25 stack.** The repo has only 2 IT classes against
  105 unit tests, so IT coverage is thin and correspondingly cheap to run. If no WildFly 40 image is
  available, that is a blocker to record and escalate, not a reason to declare done on unit tests alone.

## Acceptance criteria

- **AC1** — `git log` on `team/25.104.x` shows the DD-43194 parity commits as **ancestors of** this
  story's first commit, and no commit of this story predates the parity merge (FR1).
- **AC2** — `mvn clean install` passes on JDK 25 for the full reactor.
- **AC3** — Every DD-43194 parity test passes on Java 25, with no assertion weakened — only imports
  changed. Any that cannot is recorded as a finding with its divergence described (FR3, FR1a).
- **AC4** — `grep -rE '^import (static )?javax\.' --include='*.java'` returns **only** genuinely-JDK
  packages, if any. No Jakarta EE `javax.*` import remains (FR6, FR7).
- **AC5** — No `javaee-api` coordinate remains anywhere, **including all six plugin-internal
  declarations** (FR8). Verified by grep across all poms, not by build success alone.
- **AC6** — All 12 `beans.xml` are on the Jakarta namespace and **all 12 still declare
  `bean-discovery-mode="all"`** (FR9).
- **AC7** — The build runs on the `ubuntu-j25` agent and the `wildfly40` template track (FR12).
- **AC8** — `liquibase.properties` contains no `liquibase.hub.mode`, and Liquibase 5 accepts the file
  (FR17).
- **AC9** — The upstream `prosecution-casefile` pin decision is recorded with its reasoning (FR5).
- **AC10** — The BC-15 field check is recorded before the `coredomain` bump, naming the 8 fields and
  whether any feeds this context (FR18).
- **AC11** — A QA Docker image is published to `crmdvrepo01.azurecr.io/hmcts/` and its tag recorded
  (FR19).
- **AC12** — The integration-test suite result on the Java 25 stack is recorded — passing, or blocked
  with the blocker named (FR20).
- **AC13** — No `deltaspike`, `persistence-deltaspike` or `hibernate-entitymanager` coordinate
  remains in any pom, and `apache-deltaspike_test-container.properties` is gone (FR21).
- **AC14** — The decision on `pcfdlrm-viewstore-persistence` — retained with trimmed dependencies, or removed — is
  recorded with its reasoning (FR22).

## Out of scope

- Writing parity tests — DD-43194 parity stage, already merged to `team/25.104.x`. Migrating their
  imports is in scope (FR1a); changing their assertions is not.
- `cpp-context-stagingdlrm` — DD-43192, its own pipeline.
- Framework and platform repository changes — PEG-3296 owns those. In particular BC-15's core-domain
  cherry-pick is **not** this story's to make; FR18 only *checks*.
- A production release. The tracker shows only `support` has gone that far.
- The "material-client decoupling" PR the fleet standard includes — **verified not applicable**, this
  repo has no such dependency.
- Any Azure Functions work — stagingDLRM only; there is no such module here.
- Opportunistic dependency bumps beyond what the enforcer requires (FR4).
- Reformatting `ReceiveMigratedCaseRuleTest` (stored as a single unformatted line) — noted in the parity
  design as a separate trivial PR, deliberately kept out of the upgrade diff.

## Risks and notes

- **FR8 is this story's most likely failure, and it is quiet.** Six plugin-internal `javaee-api`
  declarations across three modules, inside `<plugin><dependencies>`, surfacing as code-generation
  errors that do not name the coordinate. Three times stagingDLRM's exposure. Enumerate them before
  starting; the count is the checklist.
- **FR7 protects against a trap this repo does not currently have.** No JDK-namespace `javax` import
  exists here today, so a blanket replace would work — until someone adds a `javax.crypto` or
  `javax.naming` import between now and implementation. DD-43192 has a live instance; this repo is one
  commit away from one.
- **FR3's failure mode is the epic's worst case.** The parity tests pin `ZonedDateTime` zone identity
  on `ListHearingRequest.listedStartDateTime` and `Defendant.courtProceedingsInitiated` — both cross
  into another context. A red test here means migrated hearing data would have been silently wrong.
  Treat a red as the system working.
- **FR5's upstream pin is a real fork in the road, not a formality.** Pinning the released Java 25
  `prosecution-casefile` couples this story to that context's release cadence; staying on the J17
  interface risks a mismatch at runtime. `mi-reportdata` chose wrong and needed a follow-up bump.
- **The tracker's version figures are a month old** (06 Aug 2026). FR2 says reconfirm.
- **Owner unassigned.** `prosecution-casefile-dlrm` shows owner "?" on the PEG-3296 tracker. Confirm
  with Platform Engineering before cutting the branch.
- **Thin IT coverage cuts both ways.** 2 IT classes means FR20 is cheap to satisfy and weak as evidence.
  Do not read a green IT run here as the assurance it would be in a context with 30.

- **The first `mvn clean install` on the new branch will fail at dependency resolution, not
  compilation** (FR21). Four dead coordinates sit in a module with no Java in it. Expect this as the
  opening move of implementation rather than as a surprise, and resolve FR22 before spending time
  migrating scaffolding that may not need to exist.

## Notes for the design stage

1. **Enumerate every `javaee-api` site before starting** (FR8), separating the 8 normal dependencies
   from the 6 plugin-internal ones, by module and line. This is the single highest-value piece of
   design preparation for this story.
2. **Decide the migration mechanism for FR6/FR7.** A scripted per-package rewrite against an explicit
   allowlist of the five Jakarta namespaces is safer than a blanket replace, and makes AC4 checkable.
3. **Resolve FR5 (the upstream pin) at the gate, not during implementation.** It affects what
   `mvn clean install` even resolves, so discovering it late is expensive.
4. **Do FR18's core-domain field check first.** It is a read-only check that could invalidate the
   `coredomain` bump, and this repo builds core-domain types directly.
5. **Coordinate with DD-43192 on shared learnings** — the generator fixes (FR10), the `beans.xml`
   namespace migration (FR9) and the pipeline track (FR12) are the same work in both repos. Whichever
   runs second should reuse, not re-derive.
6. **Plan the follow-up image PR into the story from the start** (FR19). Nine contexts learned this the
   expensive way.

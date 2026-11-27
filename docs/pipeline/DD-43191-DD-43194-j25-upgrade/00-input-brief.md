# Input brief — Java 25 / WildFly 40 / Jakarta EE 11 upgrade of PCFDLRM

> Stage 0 artefact. Feeds [`01-requirements.md`](./01-requirements.md).
> **Self-contained** — everything an SDLC stage needs to run against this story is in this directory.

| | |
|---|---|
| Epic | [DD-43191](https://tools.hmcts.net/jira/browse/DD-43191) — Java 25 upgrade, DLRM contexts |
| Story | [DD-43194](https://tools.hmcts.net/jira/browse/DD-43194) — PCFDLRM, **upgrade stage** |
| Repo | `cpp-context-prosecution-casefile-dlrm` — **this repo only** |
| Target branch | **`team/25.104.x`** — cut from `main` before the parity story; this story is the second of two PRs onto it |
| Depends on | [DD-43194 parity stage](../DD-43191-DD-43194-j25-parity/00-input-brief.md) — **hard dependency**, see below |
| Platform ticket | PEG-3296 |

## The epic this story belongs to

**DD-43191 — Java 25 upgrade, DLRM contexts.** Move `cpp-context-stagingdlrm` and
`cpp-context-prosecution-casefile-dlrm` from Java 17 to Java 25 (WildFly 26.1→40, Jakarta EE 8→10/11,
~30 transitive library bumps), de-risked by behavioural-parity tests written and executed on J17 first.

Four pipelines: parity and upgrade, for each of the two repos. Cross-cutting decisions live in two ADRs, **mirrored into this repo so nothing here depends on the other
repo being checked out**. They are not restated here:

- [**`DD-43191-j25-upgrade-mechanics`** — J25 upgrade mechanics](../adrs/DD-43191-j25-upgrade-mechanics.md)
  — branch/milestone strategy and the pipeline track. **Read this before stage 2.** Its decisions 4–6
  (Function App JDK, BC-12 carve-out, `anonymise` module) are stagingDLRM-only and do not apply here.
- [**`DD-43191-j25-parity-method`** — J25 parity method and BC scope](../adrs/DD-43191-j25-parity-method.md)
  — the parity tests this story must keep green, and the BC matrix explaining which risks apply here.

Sibling story in the other repo: [DD-43192](https://github.com/hmcts/cpp-context-stagingdlrm/blob/team/25.104.x/docs/pipeline/DD-43191-DD-43192-j25-upgrade/00-input-brief.md).
The two are independently deliverable — no shared code, no ordering constraint. Either can go first.

## This story's request

Upgrade the context to Java 25 / WildFly 40 / Jakarta EE 11 on a new `team/25.104.x` branch, keeping the
DD-43194 parity tests green, and produce a QA Docker image.

**The hard dependency, and why it is hard.** Both stages share `team/25.104.x`. The parity PR must
merge into it **while it is still J17** — that is what makes its runs J17 evidence. This story is what
ends that state, so it must not start until the parity PR has landed (the parity-method ADR's Method 1, the upgrade-mechanics ADR
decision 1). Sequence:

```
cut team/25.104.x from main  ▸  parity PR → team/25.104.x  ▸  upgrade PR → team/25.104.x  ▸  pipeline/Docker fix PR
```

A useful consequence of the shared branch: the parity tests are migrated `javax`→`jakarta` by **this
story's own sweep**, along with every other source file. They are authored once, not twice.

**Reference implementation:** `cpp-context-prosecution-casefile` commit `122a5a8fdc` on
`team/25.104.x` (401 files, +5870/−5018) — the upstream context this one forwards to, so its shape is
the closest available match. Take its **shape, not its version numbers**: it pinned `25.104.0-M4` and
has since been bumped twice.

**Target versions** (tracker, 06 Aug 2026 — reconfirm at stage 5):
`cpp-platform-maven-service-parent-pom` `17.104.1` → **`25.104.0-M10`**;
`cpp-platform-core-domain` → **`25.104.0-M11`**.

**Known work, from the fleet's recorded failures and a scan of this repo:**

| Area | Detail |
|---|---|
| `javax.json` → jakarta/parsson | 7 coordinates: domain-aggregate, domain-event, query-view, event-listener, command-handler. The largest mechanical item |
| Generator plugins | `messaging-client-generator` needs parsson in its **plugin** deps (system-scheduling hit this); `rest-client-generator` needs the jakartaee-api swap (system-announcement hit this). This repo uses all four generators + RAML |
| Jackson / `ZonedDateTime` | BC-08 lands in **main** code here — `MigratedCaseFileAggregate` and both `…ToCC…Converter` classes. The parity tests from DD-43194 are the gate; expect assertion churn |
| `@Inject EntityManager` | → `@PersistenceContext(unitName)` (staging-dcs) — *likely N/A, no JPA code* |
| `persistence.xml` | 1.0 → 3.0 namespace (system-announcement) |
| `h2` (test scope) | `1.4.196` → `2.3.232`, dropping MVCC/MV_STORE URL settings (notification) |
| Pipeline | `centos8-j17` → `ubuntu-j25`; `wildfly40` template ref; `aksDeployBranch`; Dockerfile base Ubuntu 24.04, RHEL `yum` lines removed; jacoco → 0.8.14 |
| `jboss-deployment-structure.xml` | Reference added one — check whether this repo needs it |

**Not needed here, verified:** the "material-client decoupling" half of the standard fleet upgrade —
neither DLRM repo has that dependency, so no decouple PR (the upgrade-mechanics ADR decision 3). No `anonymise` module
in this repo either, so the upgrade-mechanics ADR's open question does not apply.

## Decisions taken with the requester

| Question | Decision |
|---|---|
| Pipeline shape | Four pipelines — parity + upgrade, per repo. This is the PCFDLRM upgrade one |
| Who cuts the branch | We do, from `main`, after the parity PR merges |
| Milestone target | **Latest** (`service-parent-pom M10` / `coredomain M11`), not the reference's M4 |
| Shared ADRs | Mirrored into both repos under the same filename — each repo is self-contained |

## Scope boundaries

| In scope | Out of scope |
|---|---|
| All pom/jakarta/codegen changes; migrating the parity tests to `jakarta` | Writing parity tests — DD-43194 parity stage, already merged to this branch |
| `azure-pipelines.yaml` + `Dockerfile` for the `wildfly40`/`ubuntu-j25` track | `cpp-context-stagingdlrm` — DD-43192, its own pipeline |
| Producing a QA Docker image (this is the definition of done) | Framework/platform repo changes — PEG-3296 owns those |
| Keeping the DD-43194 parity tests green | Production release — the tracker shows only `support` has gone that far |

## Known blockers / open items

- **`liquibase.properties` carries a property Liquibase 5 has removed — a live deploy blocker.**
  Found during a dependency and configuration scan of this repo on 2026-09-01. Verified against the
  file, not inferred.
  `pcfdlrm-viewstore/pcfdlrm-viewstore-liquibase/src/main/resources/liquibase.properties` declares
  `liquibase.hub.mode: off`. Liquibase Hub was sunset and its configuration removed; the property
  existed only to silence Hub warnings in 4.1.0–4.17.2. On Liquibase 5.0.3 this is an unknown-parameter
  failure in the **K8s pre-install migration job**, before any application code runs. **The same line
  exists in `cpp-context-stagingdlrm`** — BC-07's residual is *"a per-context sweep of copied
  `liquibase.properties`"*, and it is unswept in both DLRM repos. **Delete the line; verify
  `liquibase.headless` the same way** (lower confidence) by running Liquibase 5 against the file. It was
  deliberately not fixed in the parity story: there is no J17 behaviour to pin, and FR13/AC10 there keep
  that PR test-only.

- **Check the 8 missing core-domain fields *before* bumping `coredomain`, not after.** Found during the
  same scan. This context constructs `uk.gov.justice.core.courts.Defendant` and
  `ListHearingRequest` **directly**, so it is more exposed to BC-15 than stagingDLRM is. The report
  records 8 schema fields absent from the J25 `cpp-platform-core-domain` line pending a
  release-management cherry-pick. If any sit on the types this repo builds, the upgrade breaks at
  compile time — recoverable, but far cheaper to check first.

- **Expect the Docker image build to fail first time.** Nine contexts on the tracker are "Merged — no
  Docker image produced (build failed)", several still carrying an open pipeline-revert or Docker-fix
  PR. **The image build never runs on a pull request** — `azure-pipelines.yaml` sends PR builds to
  `context-verify.yaml` (SonarQube only) and only merge builds to `context-validation.yaml`, which is
  where `docker-build.yaml` pushes to `crmdvrepo01.azurecr.io`. So whatever breaks it is undiscoverable
  until after this story's PR has merged. The image stays **in this story's scope** — treat "QA Docker
  image available", not "upgrade PR merged", as done — but expect it to take a second merge to get
  there.
- **The upstream `prosecution-casefile` pin.** This context forwards to `cpp-context-prosecution-casefile`,
  which is already merged on `team/25.104.x` with a QA image at `25.104.3-M4-SNAPSHOT`. Decide at
  stage 2 whether to pin its released J25 version or stay on the J17 interface — the tracker records
  `mi-reportdata` hitting exactly this problem (merged against a `-M1-SNAPSHOT` and needing a
  follow-up bump to the released version).
- **BC-08 is this repo's real risk.** Unlike stagingDLRM, `ZonedDateTime` sits in main code on the
  outbound payload path. If the DD-43194 parity tests go red here, that is the gate working — treat a
  red as a finding, not a test to relax.
- **No local WildFly 40 Docker image existed** as of the investigation report (`cpp-developers-docker`
  on 26.1.3); the tracker says a `java-25` branch now exists. Confirm before relying on local ITs.
- **BC-15 watch item.** `coredomain` moves to M11. The report records 8 schema fields missing from the
  J25 core-domain line, pending a release-management cherry-pick. Check whether any field this context
  reads is among them before bumping.
- Owner is unassigned for `prosecution-casefile-dlrm` on the PEG-3296 tracker. Confirm with Platform
  Engineering before cutting the branch.

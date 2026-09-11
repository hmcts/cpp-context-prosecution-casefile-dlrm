# User stories — DD-43194: J25/WildFly 40/Jakarta EE 11 upgrade of PCFDLRM

> Stage 3 artefact (user stories). Source: [`01-requirements.md`](./01-requirements.md) (FRs/ACs),
> [`02-design.md`](./02-design.md) §M (task table T0–T9) and §N (open questions, resolved state as of
> the stage 2 gate). Mirrored ADRs referenced, not restated:
> [`DD-43191-j25-upgrade-mechanics`](../adrs/DD-43191-j25-upgrade-mechanics.md),
> [`DD-43191-j25-parity-method`](../adrs/DD-43191-j25-parity-method.md).

> **Jira ticket linkage — resolved at the Stage 3 gate, 2026-09-09.** DD-43194 stays as the single
> ticket covering all three stories; Story A/B/C are tracked as `T`-numbered tasks under it, matching
> the parity stage's own convention, despite being three separately-mergeable PRs on three different
> timelines rather than the parity stage's single-PR shape. No Jira MCP is connected in this session, so
> no ticket state has actually been updated — that update (T7/T8/T9 sub-task entries, or equivalent, on
> DD-43194) is still a follow-up action, not performed by this stage.

## How to read this doc

Three stories, split exactly as `02-design.md` §M recommends (Story A = T0a–T6, Story B = T7, Story C =
T8+T9). Each: GDS-format user story, Given/When/Then ACs mapped to the actual AC numbers in
`01-requirements.md` (none invented), a Definition of Done grounded in this repo's own gates (`mvn
clean install`, the DD-43194 parity suite, the review checklist, CI), an explicit accessibility/NFR
line, and an explicit Jira-ticket-linkage call for the human gate.

None of these needs an ADR of its own — `02-design.md`'s closing section already confirms no FR/AC
in this design surfaced a trade-off big enough for one (FR5 resolved to "no pin exists" as a finding;
FR22's module-retention trade-off is recorded in `02-design.md` §F/AC14 directly, scoped to one module
in one repo).

---

## Story A — Upgrade PCFDLRM to Java 25 / WildFly 40 / Jakarta EE 11

**As a** CPP platform engineer retiring the Java 17 estate,
**I want** PCFDLRM's full Maven reactor building and passing the DD-43194 parity suite on Java 25,
WildFly 40 and Jakarta EE 11 — every `javax` namespace migrated, every dead dependency-resolution
blocker removed, and the pipeline pointed at the `ubuntu-j25`/`wildfly40` track,
**so that** the platform-milestone bump (`service-parent-pom` → `25.104.0-M10`, `coredomain` →
`25.104.0-M11`) is a single, reviewable PR rather than an unbounded one, and the date-time values this
context forwards on the outbound payload path are provably unchanged, not plausibly unchanged.

### Background

This is the upgrade PR itself — `02-design.md` §M tasks **T0a, T0, T1, T2, T3, T4, T5, T6**, in that
dependency order. It depends on the DD-43194 parity stage already merged (`6a131c1`, PR #28,
`../DD-43191-DD-43194-j25-parity/`) — that dependency is already satisfied on `team/25.104.x` today, so
this story is unblocked, not blocked-pending. It is the reference-implementation-shaped PR: 15
`javaee-api` sites, ~93 `javax` import lines across 58 files, 12 `beans.xml`, one dead-dependency
module, three pipeline lines.

### Acceptance criteria

- [ ] **AC-A1 (maps to AC1)**: Given `team/25.104.x` at its current tip, when this story's first
      commit lands, then `git log` shows the DD-43194 parity commits as ancestors of it, and no commit
      of this story predates the parity merge.
- [ ] **AC-A2 (maps to AC2)**: Given the full reactor after T2–T6 land, when `mvn clean install` is run
      on JDK 25, then it passes for every module, including `pcfdlrm-viewstore-persistence` (T2 must
      resolve first — FR21 is a hard dependency-resolution failure, not a compile error).
- [ ] **AC-A3 (maps to AC3)**: Given the DD-43194 parity suite (zero `javax` imports on this branch —
      the sweep does not touch it), when it is run on Java 25, then every test passes with no assertion
      weakened — only the one deliberate, in-scope exception: the BC-07 `liquibase.properties` key-set
      test, whose *inventory* (not behaviour) changes under Story B/T7, tracked there not here. Any
      other red parity test is a finding recorded and the story stops (FR3).
- [ ] **AC-A4 (maps to AC4)**: Given the five-prefix allowlist sweep (`02-design.md` §C) with its
      pre-flight FQN diff, when `grep -rE '^import +(static +)?javax\.' --include='*.java' .` is run,
      then it returns empty, or names a genuine JDK package explicitly in the PR description.
- [ ] **AC-A5 (maps to AC5)**: Given all 15 `javaee-api` sites enumerated in `02-design.md` §B (10
      normal + 5 plugin-internal), when `grep -rn '<artifactId>javaee-api<' --include=pom.xml .` is run,
      then it returns empty.
- [ ] **AC-A6 (maps to AC6)**: Given all 12 `beans.xml`, when they are migrated to the Jakarta
      namespace, then all 12 still declare `bean-discovery-mode="all"` explicitly (BC-14's *Refuted*
      verdict depends on this surviving).
- [ ] **AC-A7 (maps to AC7)**: Given `azure-pipelines.yaml`, when the three lines in T6 land (`ref:
      'wildfly40'`, `identifier -equals ubuntu-j25`, `aksDeployBranch: 'wildfly40'`), then the build
      runs on the `ubuntu-j25` agent and the `wildfly40` template track.
- [ ] **AC-A10 (maps to AC10)**: Given T1's re-run of the BC-15 seven-literal field check, when it is
      executed against the branch as it stands at implementation time (not trusted from the design-time
      run in `02-design.md` §E), then its result is pasted into the PR before T3's `coredomain` bump.
- [ ] **AC-A13 (maps to AC13)**: Given `pcfdlrm-viewstore-persistence` trimmed per T2, when the repo is
      searched, then no `deltaspike`, `persistence-deltaspike` or `hibernate-entitymanager` coordinate
      remains in any pom, and `apache-deltaspike_test-container.properties` is gone.
- [ ] **AC-A14 (maps to AC14)**: Given the retain-and-trim decision in `02-design.md` §F, when the PR
      is opened, then it records that decision and its reasoning (module retained, seven live
      dependencies kept — not the deletion alternative) rather than leaving a reviewer to ask why.

### Out of scope for this story

- Story B's Liquibase fix (T7) — independently deliverable, tracked separately so this PR's diff stays
  to poms/Java/descriptors/CI, per the design's own "keeps the PR free of unrelated content" rationale.
- Story C's image publication and integration-test run (T8/T9) — cannot start until this story merges.
- Migrating `pcfdlrm-viewstore-persistence`'s `persistence.xml` namespace — deliberately left on the
  legacy 1.0 namespace (`02-design.md` §G, §N-5, reversed at the 2026-09-09 gate). Not a gap; a
  recorded deviation.
- Adding a `jboss-deployment-structure.xml` — confirmed not needed; BC-12's flagged deploy-breaker
  cannot arise here (`02-design.md` §I/FR14).
- Pre-emptively adding a parsson plugin-dependency workaround — build first, add only if code
  generation actually fails on M10 (`02-design.md` §H/FR10).
- Any change to `docker/Dockerfile_pcfdlrm-service` — confirmed no target for the fleet's Ubuntu-base
  item (`02-design.md` §I/FR15).
- Pinning a Maven coordinate on the upstream `prosecution-casefile` context — confirmed not to exist
  (`02-design.md` §D/FR5/AC9); this story's PR description should still state that finding as AC9's
  evidence even though no task delivers it.
- Reformatting `ReceiveMigratedCaseRuleTest` — kept out of the upgrade diff per the parity design.

### Definition of done

- [ ] `mvn clean install` green on JDK 25 for the full reactor (AC2).
- [ ] The DD-43194 parity suite green on Java 25, with `docs/j25-parity-checklist.md`'s status column
      updated so the next reader can tell J17-green from J25-green (AC3, `02-design.md` §L).
- [ ] AC1, AC4, AC5, AC6, AC7, AC10, AC13, AC14 all satisfied and their evidence (greps, `mvn` output,
      the BC-15 re-run, the retain/trim reasoning) pasted into the PR description, not left implicit.
- [ ] AC9's finding (no upstream Maven pin exists) restated in the PR description as pre-discharged
      evidence, per `02-design.md` §D — no task delivers it, but the PR should not leave a reviewer to
      re-derive it.
- [ ] Code reviewed against `skills/review-checklist.md` (Spring Boot/Azure items not applicable — this
      is a WildFly/Jakarta EE service; the generic Java/Maven/CI items do apply).
- [ ] No critical or high Snyk findings introduced by the ~30 transitive bumps.
- [ ] `runIntegrationTests.sh` is **not** required for this story — it adds no endpoint, no RAML change,
      no `@Handles` handler, no descriptor subscription (`02-design.md` §K). Story C runs the existing
      two ITs.
- [ ] **No user-facing UI in this story; the WCAG/accessibility Definition-of-Done item does not
      apply** — this is a Maven/pom/Java/CI upgrade with no rendered output.
- [ ] **M1 (added at the Stage 4 gate, 2026-09-09)**: after the first successful JDK 25 `mvn clean
      install`, the BC-21 generated-adapter name-set check (`04-test-specs-story-a.md` §6) run and its
      output — the full class-name set under `pcfdlrm-command-api` and `pcfdlrm-event-processor`,
      compared against the J17 baseline of 23 names — pasted into the PR.
- [ ] Merged to `team/25.104.x`, unblocking Story C.

### Jira ticket linkage — resolved

Tracked directly against DD-43194 using this repo's existing `T`-task convention (T0a–T6), matching the
parity stage's `03-stories.md`. PR linked in a comment on DD-43194 on merge.

### Notes / open questions

- T0a (project-version bump, `17.104.x` → `25.104.x-DLRMJ25-SNAPSHOT`) has a working template already
  merged in the sibling repo (`cpp-context-stagingdlrm` PR #54) — confirm the exact scheme string with
  the branch owner before this story's first commit, per `02-design.md` §N-1. Not otherwise blocking.
- §N-4 (Progression has no J25/coredomain-M11 branch at all — its `main` sits on `coredomain
  17.103.13`) is **not** a blocker for this story and not this story's to resolve — it's a
  deployment-ordering question for whoever schedules the Stage 8 sandbox rollout. Carried forward here
  only so it isn't lost between stages: raise it as a named cross-team dependency at this human gate,
  with an owner, rather than letting it surface itself at Stage 8.
- Reconfirm the milestone pins (T0) against the PEG-3296 tracker at implementation time — FR2 says take
  the newer if they've moved past M10/M11 since 06 Aug 2026.
- Owner is still unassigned for `prosecution-casefile-dlrm` on the PEG-3296 tracker (§N-7) — not
  something this stage resolves, flagged again so it isn't dropped.

---

## Story B — Remove the obsolete `liquibase.hub.mode` key (hygiene cleanup, not a deploy blocker)

**As a** developer keeping this branch's Liquibase config current with Liquibase 5's defaults,
**I want** the obsolete `liquibase.hub.mode` property removed from `liquibase.properties`, with
`liquibase.headless` verified against Liquibase 5,
**so that** the config file matches what Liquibase 5 actually recognises, even though — corrected
2026-09-09 — no test needs updating and no deploy is actually at risk.

### Background

`02-design.md` §J / task **T7**. Standalone: touches only
`pcfdlrm-viewstore/pcfdlrm-viewstore-liquibase/src/main/resources/liquibase.properties` — nothing
Story A or Story C touches, and (corrected below) no test file either. **Severity corrected at this
gate, 2026-09-09**: the original framing ("deploy blocker", "pre-install job rejects the file") is
overstated.
- The parity stage's own checklist already found an unsupported Liquibase key only logs a warning
  under non-strict config (which this repo uses) — it does not crash the pre-install job, on either
  Liquibase 4.10.0 or 5.0.4, tested against real jars.
- Cross-checked directly against the actual reference repo's completed upgrade
  (`cpp-context-prosecution-casefile`, `team/25.104.x`, confirmed on the Jakarta 4.0 `beans.xml`
  namespace, confirmed to contain the real upgrade commit `122a5a8fdc`): its equivalent
  `liquibase.properties` **still has `liquibase.hub.mode: off` in place**, untouched since the initial
  migration commit — the reference never removed it, and it evidently hasn't blocked their deploys.
  `cpp-context-stagingdlrm` offers no precedent either way (same file, but no parity or J25 work has
  started there at all).
- Still worth doing as low-effort hygiene — Liquibase 5.0.4 does genuinely drop this key from its
  defaults — but it can be deprioritised relative to how FR17 originally framed it, and it can still
  ship independently on its own timeline.

### Acceptance criteria

- [x] **AC-B8 (maps to AC8)**: Given `liquibase.properties` with `liquibase.hub.mode: off` removed, when
      Liquibase 5 is run against the file, then it accepts the file with no unknown-parameter failure.
      `liquibase.headless` verified the same way (lower-confidence item named explicitly in FR17;
      confirm rather than assume). **Verified 2026-09-11**: `liquibase.headless` maps to
      `GlobalConfiguration.HEADLESS`, still present (deprecated, not removed) in Liquibase 5.0.x —
      confirmed via Liquibase's own 5.0 javadoc/deprecated-list, not assumed.

### Out of scope for this story

- Any change to `pcfdlrm.xml` (the changelog the properties file points at) — it currently has no
  changesets; this story touches configuration, not migration content.
- Anything in Story A's `javax`→`jakarta` sweep, T2's dependency trim, or the pipeline-track change —
  none of that is needed for this fix and none of it is touched.
- Resolving the same line in `cpp-context-stagingdlrm` — noted as the same defect existing there, but
  that is DD-43192's own PR in its own repo, per this repo's "a story belongs to exactly one repo"
  convention.

### Definition of done

- [x] `liquibase.hub.mode` no longer present in `liquibase.properties`.
- [x] `liquibase.headless` verified (not assumed) to be accepted by Liquibase 5, with the verification
      method recorded in the PR — still a valid (deprecated but not removed) `GlobalConfiguration` key
      in Liquibase 5.0.x per its own javadoc/deprecated-list.
- [ ] **No test to update — confirmed, not assumed.** `LiquibasePropertiesKeyInventoryTest` does not
      exist on this branch (verified by direct grep, 2026-09-09): the parity stage wrote it, ran it
      green, then deliberately reverted it. The earlier plan to "update the pinned key set in the same
      commit" no longer applies — there is nothing pinned to update.
- [ ] Code reviewed against `skills/review-checklist.md`.
- [ ] No critical or high Snyk findings introduced.
- [ ] **No user-facing UI in this story; the WCAG/accessibility Definition-of-Done item does not
      apply.**
- [ ] Deployed to and verified on sandbox (this is a deploy-time fix; sandbox verification is the real
      proof, not just the Liquibase 5 dry-run).

### Jira ticket linkage — resolved

Tracked directly against DD-43194 as its `T7` task, matching this repo's convention. It is a genuine
live deploy blocker independent of the Java 25 epic, but rides under the same ticket rather than a
separate defect ticket — the team's decision was to keep all three stories under one ticket.

### Notes / open questions

- None outstanding — this is the most self-contained of the three stories, per `02-design.md` §J's own
  framing ("cheap... and touches nothing T2–T6 touch"), and its severity is now corrected to match: a
  low-effort hygiene cleanup, not a live deploy risk.

---

## Story C — Publish the QA Docker image and run integration tests on Java 25

**As a** CPP platform engineer who needs proof the upgrade actually deploys, not just compiles,
**I want** a QA Docker image published from the merged upgrade PR, and the two existing integration
tests run against a Java 25 / WildFly 40 stack with their result recorded,
**so that** "merged" and "done" aren't conflated the way they were for nine other contexts on the
fleet tracker that merged with no image produced.

### Background

`02-design.md` §I/§K, tasks **T8 + T9**. Starts only after Story A merges — the image build never runs
on a pull request (`context-validation.yaml` sends PR builds to `context-verify.yaml`, SonarQube only;
only a merge build reaches the `ContextValidation` stage that runs `docker-build.yaml`), so this story's
first real signal cannot exist before that point.

**Resolved findings carried forward from the design gate, not to be re-opened:**
- **§N-3 / §K**: the WildFly 40 image is confirmed published and current in ACR —
  `40.0.0.Finaljdk25_latest` (and the Camunda variant), with daily-dated tags through the design
  review date. `cpp-developers-docker`'s `java-25` branch carries substantive IT-stack work (Dockerfile,
  `docker-compose.yml`, `standalone.xml` changes), not a stub. **No fetch-and-check gate is needed before
  running the ITs** — that pre-step is dropped from this story's scope.
- **§N-2 / §I**: `docker-build.yaml` (the template actually on pcfdlrm's path via `ContextValidation`,
  not `build-docker-image.yaml`) globs `Dockerfile_*` under `docker/` with no service-name convention
  required. `docker/Dockerfile_pcfdlrm-service` matches as-is. **No `dockerfilePath` override is needed**
  — do not add one speculatively.

### Acceptance criteria

- [ ] **AC-C11 (maps to AC11)**: Given Story A merged to `team/25.104.x`, when the merge build runs,
      then a QA Docker image is published to `crmdvrepo01.azurecr.io/hmcts/` and its tag is recorded in
      this story. If the build fails first time (budgeted for per FR19 — nine contexts on the tracker
      needed a second merge), a follow-up pipeline/image fix PR is raised and tracked here, and "done"
      is not declared until the image exists.
- [ ] **AC-C12 (maps to AC12)**: Given the published image, when `ReceiveMigratedCaseFileIT` and
      `AddMaterialIT` are run against the Java 25 / WildFly 40 stack (`./runIntegrationTests.sh` /
      `$CPP_DOCKER_DIR`), then the result — passing, or blocked with the named blocker — is recorded.
      A green result here is recorded as what it is: cheap to obtain (2 IT classes) and weak as
      standalone evidence next to a 30-IT context, not overstated.

### Out of scope for this story

- Writing any new integration test — this story adds no endpoint, no RAML change, no `@Handles`
  handler, no descriptor subscription, so this repo's "integration tests for new endpoints" hard rule
  does not create a new obligation here; it only requires the two existing ITs to run.
- Fixing the fetch-and-check gate for the developer WildFly 40 image — resolved, not needed (§N-3).
- Adding a `dockerfilePath` override — resolved, not needed (§N-2).
- Confirming which branch (`master` vs `main`) `cpp.pipeline`'s ADO template resource checks out for
  `widlfly40_base_tag` — small residual risk named in `02-design.md` §K, worth a one-line confirmation
  during this story but not a blocker to starting it.

### Definition of done

- [ ] QA Docker image published; tag recorded in this story / the PR (AC11).
- [ ] Integration-test result recorded — passing, or blocked with the blocker named (AC12).
- [ ] Any image-build failure diagnosed and fixed via a follow-up PR, not silently left as "merged, no
      image" (FR19's explicit warning).
- [ ] The `cpp.pipeline` `master`-vs-`main` branch question for `widlfly40_base_tag` confirmed with one
      line in the PR, one way or the other.
- [ ] Code reviewed against `skills/review-checklist.md` for whatever the image-build fix touches (if
      any is needed).
- [ ] **No user-facing UI in this story; the WCAG/accessibility Definition-of-Done item does not
      apply.**
- [ ] Deployed to and verified on sandbox.

### Jira ticket linkage — resolved

Tracked directly against DD-43194 as its `T8`/`T9` tasks. The design doc's caution about nine fleet
contexts treating the image as an implicit afterthought still applies — worth keeping T8/T9 visible as
open sub-tasks on DD-43194 rather than letting the ticket read as "done" once Story A merges, even
though it stays one ticket rather than a separate one.

### Notes / open questions

- §N-4 (no J25/coredomain-M11 branch on `cpp-context-progression`) applies to this story's sandbox
  verification step too, not just Story A's merge — a sandbox deploy here sends M11-shaped payloads to
  whatever `progression` instance is running. Same recommendation as Story A: named cross-team question,
  owner needed, not a blocker to starting this story.
- If the image build fails in a way that implicates the empty `jboss-deployment-structure.xml` question
  Story A deliberately left closed (`02-design.md` §I/FR14), that is the trigger the design doc names
  for revisiting it — with a real descriptor, not the inert template. Not expected, but named so it
  isn't treated as a Story A regression if it happens.

---

## Sequencing summary

```
Story A (T0a–T6, the upgrade PR)  ──merges──▶  Story C (T8+T9, image + ITs)
Story B (T7, Liquibase fix)  — independent, any time relative to A/C
```

Story A and Story B have no shared files and no ordering constraint between them — either can merge
first, or they can run in parallel. Story C is the only one with a hard dependency, and it is on Story
A's merge, not on Story B.

## Jira ticket linkage — roll-up, resolved 2026-09-09

**DD-43194 stays as one ticket for all three stories**, tracked via this repo's `T`-task convention
(T0a–T6 / T7 / T8–T9) exactly as the parity stage's `03-stories.md` did for its own 8 sub-tasks — despite
Story A/B/C being three separately-mergeable PRs on three different timelines rather than the parity
stage's single-PR shape. No Jira MCP is connected this session, so the ticket itself has not actually
been updated with these sub-task entries yet — that update is a follow-up action for whoever has Jira
access, not performed by this stage.

---

## Stage 3 gate — approved 2026-09-09

Three stories reviewed and approved as written. Jira ticket-linkage decision made (above) — one ticket,
DD-43194, tracked via `T`-tasks. **Outstanding before Stage 4 can start on any given story:** the
T-task entries themselves still need adding to DD-43194 — no Jira MCP is connected this session, so
that action has not actually been performed yet, only decided. Story A and Story B are otherwise
unblocked for Stage 4; Story C's Stage 4 start is additionally gated on Story A merging.

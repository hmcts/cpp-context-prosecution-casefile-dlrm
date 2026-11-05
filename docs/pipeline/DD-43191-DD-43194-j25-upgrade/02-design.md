# Design — DD-43194: Java 25 / WildFly 40 / Jakarta EE 11 upgrade of PCFDLRM

> Stage 2 artefact (architecture & design). Source: [`01-requirements.md`](./01-requirements.md),
> [`00-input-brief.md`](./00-input-brief.md), and the two mirrored ADRs
> ([upgrade mechanics](../adrs/DD-43191-j25-upgrade-mechanics.md),
> [parity method](../adrs/DD-43191-j25-parity-method.md)).
>
> Every count, path and line number below was **re-derived against `team/25.104.x` at commit
> `6ab1b90`** (the branch tip after the parity PR #28 merged), not carried over from stage 1. Where a
> re-derived figure disagrees with `01-requirements.md`, §A says so and this document's figure wins —
> the requirements text is not rewritten.
>
> **No code, pom, ADR or CI config is touched by this stage.** Everything here is "which file changes,
> to what, in which order."

## How to read this doc

Sections A–L each take one requirement cluster and give: **what is actually there** (verified),
**the decision**, and **why** (which FR/AC it discharges, what was rejected). §M turns that into a
task breakdown the story-writer can cut into independently-deliverable stories. §N is the open-questions
list — nothing in it is invented, each is a real gap found while verifying.

### On the reference implementation

`cpp-context-prosecution-casefile` commit **`122a5a8fdc`** (and its branch tip `abfae825` on
`origin/team/25.104.x`) turned out to be **checked out locally** at
`~/moj/cpp-context-prosecution-casefile`, so its diff was read directly rather than described from
the tracker. Every load-bearing fact taken from it is **inlined below**, with the SHA that carries it.
This story does not require that checkout — per this repo's CLAUDE.md, no stage may need a file that
lives only in another repo.

---

## A. Verified inventory — five corrections to the stage 1 figures

| Item | `01-requirements.md` says | Re-derived on `team/25.104.x` @ `6ab1b90` | Effect |
|---|---|---|---|
| `javaee-api` normal module deps | 8 | **10** | §B — two more modules than the checklist expected |
| `javaee-api` plugin-internal deps | 6 (`event-processor` ×2, `command-handler` ×4, `command-api` ×2) | **5** (`event-processor` ×2, `command-handler` ×2, `command-api` ×1) | §B — the shape is right, the arithmetic was not |
| `javax.*` import lines / files | 94 / 58 | **93 / 58** | §C — immaterial; the allowlist is derived from FQNs, not counts |
| `javax.json` provider coordinates | "7 coordinates" (input brief) | **5** (`org.glassfish:javax.json` ×3, `javax.json:javax.json-api` ×2) | §C |
| Modules running a generator plugin | 8 | **9 poms** — 8 leaf modules + the `pcfdlrm-event` aggregator, whose `<build><plugins>` are inherited by `event-listener` and `event-processor` | §H |

Derivation commands (re-run these at stage 5 rather than trusting the table — the branch may move):

```bash
grep -rn '<artifactId>javaee-api</artifactId>' --include=pom.xml .          # → 15 sites
grep -rhoE 'import +(static +)?javax\.[A-Za-z0-9_.]+' --include='*.java' .  # → the FQN allowlist
grep -rn 'generation-plugin\|generator-plugin' --include=pom.xml . | grep artifactId
```

**Nothing here changes an FR or an AC.** AC5 ("no `javaee-api` coordinate remains anywhere, including
all six plugin-internal declarations") is satisfied by removing all **fifteen**; the grep, not the
number in the prose, is the gate.

---

## B. FR8 / AC5 — the `javaee-api` site register

This is the story's most likely quiet failure, so it is enumerated exhaustively. All fifteen carry
`<groupId>javax</groupId>`. All ten normal declarations are `<scope>provided</scope>`; all five
plugin-internal ones carry `<version>${javaee-api.version}</version>` and no scope.

### B1. Normal `<dependencies>` declarations — 10

| # | Module | Pom | Line (`<artifactId>`) |
|---|---|---|---|
| 1 | `pcfdlrm-command-api` | `pcfdlrm-command/pcfdlrm-command-api/pom.xml` | 22 |
| 2 | `pcfdlrm-command-handler` | `pcfdlrm-command/pcfdlrm-command-handler/pom.xml` | 22 |
| 3 | `pcfdlrm-domain-aggregate` | `pcfdlrm-domain/pcfdlrm-domain-aggregate/pom.xml` | 42 |
| 4 | `pcfdlrm-event-listener` | `pcfdlrm-event/pcfdlrm-event-listener/pom.xml` | 20 |
| 5 | `pcfdlrm-event-processor` | `pcfdlrm-event/pcfdlrm-event-processor/pom.xml` | 31 |
| 6 | `pcfdlrm-healthchecks` | `pcfdlrm-healthchecks/pom.xml` | 15 |
| 7 | `pcfdlrm-query-api` | `pcfdlrm-query/pcfdlrm-query-api/pom.xml` | 20 |
| 8 | `pcfdlrm-query-view` | `pcfdlrm-query/pcfdlrm-query-view/pom.xml` | 17 |
| 9 | `pcfdlrm-refdata` | `pcfdlrm-refdata/pom.xml` | 15 |
| 10 | `pcfdlrm-viewstore-persistence` | `pcfdlrm-viewstore/pcfdlrm-viewstore-persistence/pom.xml` | 16 |

*(Site 10 disappears if §F resolves to deletion; it does not, so it stays.)*

### B2. Plugin-internal `<plugin><dependencies>` declarations — 5

| # | Module | Plugin (line) | `javaee-api` line |
|---|---|---|---|
| 11 | `pcfdlrm-command-api` | `rest-client-generator-plugin` (113) | **141** |
| 12 | `pcfdlrm-command-handler` | `pojo-generation-plugin` (98) | **169** |
| 13 | `pcfdlrm-command-handler` | `rest-client-generator-plugin` (176) | **180** |
| 14 | `pcfdlrm-event-processor` | `messaging-client-generator-plugin` (123) | **139** |
| 15 | `pcfdlrm-event-processor` | `rest-client-generator-plugin` (147) | **151** |

`pcfdlrm-domain-aggregate` also has a `<plugin><dependencies>` block (`pojo-generation-plugin`,
lines 147–158) — **verified to contain no `javaee-api`**. Do not "fix" it.

### B3. The swap

Reference shape, read from `cpp-context-prosecution-casefile` `origin/team/25.104.x`:

```xml
<groupId>jakarta.platform</groupId>
<artifactId>jakarta.jakartaee-api</artifactId>
```

- Normal deps: group + artifact change, `<scope>provided</scope>` retained.
- Plugin-internal deps: group + artifact change, **`<version>${javaee-api.version}</version>` retained
  verbatim** — the property name did not change in the reference and is supplied by the parent POM.
- Plugin-internal deps **additionally gain** a sibling:
  ```xml
  <groupId>jakarta.xml.bind</groupId>
  <artifactId>jakarta.xml.bind-api</artifactId>
  <version>${jakarta.xml.bind-api.raml.version}</version>
  ```
  The reference added exactly this in all six of its generator-plugin blocks. **This is FR10's
  `rest-client-generator-plugin` fix as the reference actually implemented it** — see §H, which
  revises FR10's description.

**AC5 gate:** `grep -rn 'javaee-api' --include=pom.xml .` returns only the
`${javaee-api.version}` property references, and `grep -rn '<artifactId>javaee-api<' --include=pom.xml .`
returns nothing.

---

## C. FR6 / FR7 / AC4 — the `javax`→`jakarta` migration mechanism

### What is actually there

**93 `javax.*` import lines across 58 files, and — decisively — only 17 distinct fully-qualified
names.** There are **zero** inline (non-import) `javax.` references in any `.java` file, verified by
`grep -rnE 'javax\.' --include='*.java' . | grep -v ':import '` returning empty. So the whole
migration is an import-line rewrite; no body edits.

The complete FQN set on this branch:

| Jakarta namespace | FQNs present |
|---|---|
| `javax.json` (43 lines) | `JsonArray`, `JsonArrayBuilder`, `JsonNumber`, `JsonObject`, `JsonObjectBuilder`, `JsonReader`, `JsonValue`, `JsonValue.NULL` (static) |
| `javax.inject` (35) | `Inject` |
| `javax.ws.rs` (7) | `core.MediaType`, `core.MediaType.APPLICATION_JSON` (static), `core.Response`, `core.Response.Status`, `core.Response.Status.OK` (static) |
| `javax.annotation` (5) | `PostConstruct` |
| `javax.enterprise` (3) | `inject.Instance`, `inject.Specializes` |

**No JDK-namespace `javax` import exists** (no `javax.net.ssl`, `javax.crypto`, `javax.xml.parsers`,
`javax.naming`). FR7 is currently a guard against a hazard the branch does not yet carry — which is
exactly why it must be mechanised rather than eyeballed.

### Decision — a five-prefix allowlist rewrite, re-derived at run time

Migrate with a **scripted, prefix-scoped rewrite against an explicit allowlist**, not a blanket
`javax.`→`jakarta.` substitution, and not per-FQN. Three steps, in order:

1. **Derive, then diff.** Before rewriting, run the FQN derivation command from §A and compare the
   result to the 17-name table above. **A new FQN outside the five allowed prefixes is a stop.** This
   is the step that discharges FR7: it catches a `javax.crypto` added between now and implementation,
   because the rewrite never sees a prefix the operator has not first acknowledged.
2. **Rewrite, prefix by prefix, one commit per prefix.** Five substitutions, anchored to the import
   keyword so nothing in a string literal, comment or Javadoc can be caught:

   ```bash
   for p in json inject ws. annotation.PostConstruct enterprise.; do
     grep -rlE "^import +(static +)?javax\.${p}" --include='*.java' . \
       | xargs sed -i '' -E "s/^(import +(static +)?)javax\.${p}/\1jakarta.${p}/"
   done
   ```
   `annotation.PostConstruct` is deliberately spelled in full rather than as `annotation.` —
   `javax.annotation.*` is *not* wholly a Jakarta namespace (`javax.annotation.processing` is JDK),
   and this repo's only `javax.annotation` import is `PostConstruct`. Naming the class, not the
   package, is what makes the rule safe if someone later adds a `javax.annotation.processing` import.
3. **Gate.** AC4's check, run as the last step of the sweep commit:
   ```bash
   grep -rE '^import +(static +)?javax\.' --include='*.java' .
   ```
   Expected: **empty**. Any survivor must be a genuine JDK package and must be listed by name in the PR
   description. An empty result is the stronger outcome and the one to expect here.

**Rejected — blanket `javax.`→`jakarta.` replace.** It would work today (there are no JDK-namespace
imports) and produce an identical diff, which is precisely the trap: it encodes no rule, so the next
person to run it on a branch that *has* grown a `javax.naming` import silently breaks the build.
DD-43192 already has a live `javax.net.ssl.SSLContext` instance. The cost of the allowlist over the
blanket form is one loop and one pre-flight diff.

**Rejected — an FQN-exact 17-entry mapping table.** Safer still, but it hard-codes a snapshot of the
branch and turns any new `jakarta.json` class (e.g. someone adding `JsonString`) into a spurious stop.
Prefix scope with a pre-flight prefix diff gets the same protection without the brittleness.

### FR1a — the parity tests

Verified against the parity merge commit `6a131c1`: it touched three test files
(`MigratedCaseFileAggregateTest`, `ProsecutionCaseFileMigratedDefendantToCCDefendantConverterTest`,
`ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverterTest`) and
`docs/j25-parity-checklist.md`. **All three test classes contain zero `javax` imports.**

So FR1a is, on this branch, a **no-op in practice**: the sweep will not modify a single line of parity
test code. AC3 therefore reduces to "run them on JDK 25 and record the result" — there is no
import-migration risk to their assertions at all. This is worth stating plainly so the story-writer
does not size a task for it.

### Provider coordinates that move with the namespace

Five pom coordinates back the `javax.json` imports and must move in the same commit, or the imports
compile against nothing:

| Module | Current | → |
|---|---|---|
| `pcfdlrm-domain-aggregate` (pom:100–101) | `org.glassfish:javax.json` | `org.glassfish:jakarta.json`, `<version>2.0.1</version>` |
| `pcfdlrm-query-view` (pom:35–36) | `org.glassfish:javax.json` | `org.glassfish:jakarta.json`, `<version>2.0.1</version>` |
| `pcfdlrm-event-listener` (pom:61–62) | `org.glassfish:javax.json` | `org.glassfish:jakarta.json`, `<version>2.0.1</version>` |
| `pcfdlrm-domain-event` (pom:25–26) | `javax.json:javax.json-api` | `jakarta.json:jakarta.json-api` |
| `pcfdlrm-command-handler` (pom:31–32) | `javax.json:javax.json-api` | `jakarta.json:jakarta.json-api` |

The `2.0.1` pin on the glassfish artifact is the reference's, carried at its branch tip — the BOM does
not manage `org.glassfish:jakarta.json`, so an explicit version is required. `jakarta.json:jakarta.json-api`
**is** BOM-managed (the reference declares it with no version, `provided` where it is API-only).

One more, in `pcfdlrm-viewstore-persistence` (pom:47–48) and **not named by any FR**:
`javax.xml.bind:jaxb-api` → `jakarta.xml.bind:jakarta.xml.bind-api`. The reference made exactly this
swap in its equivalent module. Folded into §F.

---

## D. FR5 / AC9 — the upstream `prosecution-casefile` pin

**Decision: there is no pin to move. FR5 resolves to "not applicable, for a reason worth recording",
and AC9 is discharged by this section.** No ADR is needed.

### Why — the coupling is not a Maven coordinate

FR5's premise is that "this context forwards to `cpp-context-prosecution-casefile`". At the message
level that is true; at the **build** level it is not. Verified three ways:

1. **No coordinate.** `grep -rn "prosecutioncasefile\|prosecution-casefile" --include=pom.xml .`
   returns nothing outside this repo's own `pcfdlrm-*` artifacts. The full distinct artifactId list
   across all 22 poms contains no upstream prosecution-casefile artifact of any kind — no
   `-command-api`, no `-query-api`, no `raml` classifier.
2. **The outbound call goes to Progression, not to prosecution-casefile.**
   `MigratedCaseReceivedProcessor:27` sends the action `progression.initiate-court-proceedings`; the
   payload type is `uk.gov.justice.core.courts.InitiateCourtProceedings`
   (`MigratedCaseToProsecutionCaseConverter:9,121`). The RAML/classifier dependency that backs it is
   `progression-command-api`, pinned by `progression.version` — an interface pin already governed by
   FR4, not by FR5.
3. **The only textual reference is a test constant.**
   `pcfdlrm-refdata/src/test/.../ReferenceDataQueryServiceTestHelper.java:76` declares
   `"prosecutioncasefile.command.assign-hearing"` as a string. No production code, no descriptor, no
   RAML references it. `subscriptions-descriptor.yaml` and `public-publications-descriptor.yaml`
   name only `progression`, `material` and this context's own topics.

### What this means for the story

- **Nothing to pin, nothing to bump, and the `mi-reportdata` failure mode cannot occur here.**
  `mi-reportdata` merged against a `-M1-SNAPSHOT` of a *declared dependency*; this repo declares none.
- The real shared surface with `cpp-context-prosecution-casefile` is **`cpp-platform-core-domain`**
  (`coredomain.version`, `criminal-court-public-model`), which FR2 already moves to `25.104.0-M11` —
  the identical value the reference sits on at its branch tip. Both contexts building the same
  `uk.gov.justice.core.courts.*` types from the same coredomain milestone is what keeps the
  `InitiateCourtProceedings` payload compatible. **`coredomain.version` is the pin FR5 was reaching
  for**, and §E is its precondition.
- **Follow-on risk, checked 2026-09-09 and confirmed real, not hypothetical.** Read `cpp-context-progression`
  directly: its `main` is on `coredomain.version` **`17.103.13`**, and **no `team/25.104.x` (or any
  other J25/coredomain-M11) branch exists on that repo at all** — it has not started its own upgrade.
  So the version skew this section worried about in the abstract is a live fact today: whenever pcfdlrm's
  Stage 8 sandbox build (built against `coredomain` M11) sends `progression.initiate-court-proceedings`
  to a sandbox `cpp-context-progression` instance running the deployed `17.103.13` line, that instance
  is on a coredomain milestone from before this branch's own parity/upgrade work. Messaging here is
  JSON-schema-validated, not a shared classpath, so an additive M11 field does not necessarily break a
  17.103.13 consumer — but "does not necessarily" is not "confirmed safe", and nothing in this repo can
  settle that; it requires reading `progression`'s own inbound schema for `initiate-court-proceedings`.
  Still a *deployment-ordering* question for whoever schedules the sandbox rollout, not a build question
  for this story — out of scope per the requirements ("a production release" is out of scope) — but now
  backed by a concrete fact (no progression J25 branch exists) rather than a general caution. See §N-4.

**Rejected — "add a pin on the released J25 `prosecution-casefile` to be safe."** It would create a
build-time coupling that does not exist today, to a context on a different release cadence, for no
compile-time benefit. That is the expensive half of both options FR5 offered, with none of the payoff.

---

## E. FR18 / AC10 — the BC-15 core-domain field check, performed

FR18 asks for a read-only check **before** the `coredomain` bump. It can be performed now, at design
time, and largely is. Doing it here is the point of design note #4.

### The 8 "missing fields" are 6 fields + 2 whole schemas, and they are named

The requirements and input brief both say "8 missing core-domain fields" without naming them. They
**are** named, in this repo, in
`docs/analysis/j25-upgrade/j25-behavioural-change-investigation-report.md`:

- **The BC-15 entry (line 1009)** — "4 JIRA-tracked commits (CCT-2357, CHD-1798/CHD-2236, cad-833,
  DD-42378) landed schema-only changes … adding **8 fields/refs across 6 schemas plus 2 brand-new
  schema files**". The 2026-07-08 re-run (line 1035) restates this as "**6 missing fields** (and the
  2 missing schemas) remain absent at HEAD" — so "8" is 6 fields + 2 schemas, and the two figures are
  the same finding counted differently.
- **The appendix (line 1440)** gives the literal signal set — **7 field literals**:
  `isAddressConfidential`, `isDeemedServed`, `deletedJudicialResult`, `crackedIneffectiveSubReasonId`,
  `welshProsecutorCost`, `migrationCaseStatus`, `defendantFineAccountNumber`.

That 7-vs-6 wobble is itself flagged by the report (§"Treat fleet-wide counts as directional"). The
**literal list is the usable artefact**; the count is not. No separate tracker or finding doc exists
in this repo — the report is the source, and it is checked in, so the check is self-contained.

### The check, run against this branch

```bash
for t in isAddressConfidential isDeemedServed deletedJudicialResult crackedIneffectiveSubReason \
         welshProsecutorCost migrationCaseStatus defendantFineAccountNumber MigrationCaseStatus; do
  grep -rli "$t" --exclude-dir=target .
done
```

**Result: every one of the seven literals appears in exactly one file — the investigation report
itself. Zero hits in any `.java`, `.json`, `.raml`, `.yaml` or `.xml` under any module.**

The near-miss worth naming explicitly, because it is the one a reviewer will ask about:
`uk.gov.justice.core.courts.MigrationSourceSystem` **is** imported and constructed here
(`MigratedCaseToProsecutionCaseConverter:22,126–128`), and in `cpp-context-prosecution-casefile` the
missing `migrationCaseStatus` field sits on exactly that type
(the report cites `'migrationSourceSystem' ->> 'migrationCaseStatus'`). But this repo touches only two
of its setters — `withMigrationSourceSystemName` and `withMigrationSourceSystemCaseIdentifier` — and
its own schema, `pcfdlrm-domain-value-schema/.../migrated-migrationSourceSystem.json`, declares those
two properties and `"additionalProperties": false`. **`migrationCaseStatus` is neither read nor
written here.**

### Decision

- **The BC-15 field check is CLEAR for this context, on the evidence available at design time.**
  Record this section as AC10's evidence.
- **Two confirmations remain for implementation time**, and they are cheap:
  1. **Re-run the grep on the branch as it stands at stage 5** (a story could have added a field in
     between). One command, above.
  2. **Treat the first `mvn clean install` after the `coredomain` bump as the second gate.** This repo
     is a *producer* using generated builders, so BC-15's dangerous mechanism (2) — "silently compiled
     out" — cannot apply: a field absent from the J25 line means a missing `.withX()` method, i.e. a
     **compile error, not a silent no-op**. That inverts the usual BC-15 risk in our favour. The
     `.withX()` surface actually used is enumerable from
     `grep -rhoE '\.with[A-Z][A-Za-z]*\(' --include='*.java' pcfdlrm-event/pcfdlrm-event-processor/src/main`
     if a reviewer wants it explicit.
- BC-15 mechanism (1) — schema-validation rejection of a payload carrying one of the 8 — cannot apply
  either, since this context emits none of them.

**This does not de-risk the `coredomain` bump generally**, only BC-15. The 43 `uk.gov.justice.core.courts.*`
types this repo constructs are still a real compile surface against M11; that is what AC2 is for.

---

## F. FR21 / FR22 / AC13 / AC14 — `pcfdlrm-viewstore-persistence`

### What is actually there

The module contains **four files and no Java whatsoever**:

```
pcfdlrm-viewstore/pcfdlrm-viewstore-persistence/
├── pom.xml
├── src/main/resources/META-INF/beans.xml
├── src/main/resources/META-INF/persistence.xml
└── src/test/resources/META-INF/apache-deltaspike_test-container.properties
```

`persistence.xml` declares one PU, `pcfdlrm-persistence-unit`, on `<jta-data-source>java:/DS.pcfdlrm</jta-data-source>`,
with no `<class>` entries. `DS.pcfdlrm` is wired in `pcfdlrm-service/src/main/descriptors/resource-descriptor.yml:23`.
The PU name appears **nowhere else in the repo** — no `@PersistenceContext`, no `persistence.unit.name`
property, no `@Inject EntityManager`, and zero `javax.persistence` imports repo-wide.

**Three modules depend on it** — `pcfdlrm-query-view` (pom:26), `pcfdlrm-query-api` (pom:31),
`pcfdlrm-event-listener` (pom:40) — and it reaches the WAR transitively through them.

Dead coordinates confirmed at the lines FR21 names, plus three FR21 does not:

| Line | Coordinate | Fate |
|---|---|---|
| 21 | `uk.gov.justice.services:persistence-deltaspike` | **delete** — removed from the framework reactor and BOM |
| 99, 104 | `org.apache.deltaspike.modules:deltaspike-test-control-module-api` / `-impl` | **delete** |
| 109, 119 | `org.apache.deltaspike.cdictrl:deltaspike-cdictrl-openejb` / `-api` | **delete** |
| 130 | `org.hibernate:hibernate-entitymanager` | **delete** — merged into `hibernate-core` in Hibernate 6 |
| 28–31 | `org.hibernate:hibernate-core` | **retain, re-group** to `org.hibernate.orm` |
| 33–37 | `org.hibernate:hibernate-jpamodelgen` | **delete** — annotation processor for entities that do not exist |
| 47–48 | `javax.xml.bind:jaxb-api` | **re-coordinate** to `jakarta.xml.bind:jakarta.xml.bind-api` |
| 84–97 | `org.apache.tomee:openejb-core`, `openejb-server`, `org.apache.activemq:activemq-ra` | **delete** — the DeltaSpike CDI test container these existed to boot is gone |
| 68–82, 114–117, 138–142 | `test-utils-common`, `test-utils-persistence`, `test-utils-logging-log4j`, `guava-testlib`, `test-utils-core`, `mockito-core`, `byte-buddy`, `hamcrest` | **delete** — no test sources |
| 124–127 | `com.h2database:h2` (test, no local version) | **delete** — see FR16 below |
| test resources | `apache-deltaspike_test-container.properties` | **delete** (AC13) |

### Decision — retain the module, trim it to its seven live dependencies. Do not delete it.

Post-trim the pom should carry, at most:

```
jakarta.platform:jakarta.jakartaee-api        (provided)
uk.gov.justice.services:common
org.hibernate.orm:hibernate-core              (provided)
org.apache.commons:commons-lang3
uk.gov.justice.utils:utilities-core
jakarta.xml.bind:jakarta.xml.bind-api
${project.groupId}:pcfdlrm-domain-event
```

— i.e. the module keeps only what its two descriptors and its three consumers need, and every one of
its test-scoped dependencies goes.

**Why retain, given the module has no code:**

1. **The reference retained its equivalent, and trimmed it in exactly this shape.**
   `cpp-context-prosecution-casefile` `122a5a8fdc` removed `persistence-deltaspike`, all four
   deltaspike artifacts, `hibernate-entitymanager`, `hibernate-jpamodelgen`, the tomee/openejb/activemq-ra
   trio, the `h2.version 1.4.196` property and `apache-deltaspike_test-container.properties`; it
   re-grouped `hibernate-core` to `org.hibernate.orm`, swapped `javax.xml.bind:jaxb-api` for
   `jakarta.xml.bind:jakarta.xml.bind-api`, and swapped `javaee-api`. It **did not** add
   `persistence-jpa`. Following a shape the fleet has already merged is cheaper than inventing one.
2. **Deletion is not free and is not obviously safe.** It means editing three consumer poms and
   removing a `<jta-data-source>`-bound PU from the deployed WAR. Whether WildFly 40 / the framework's
   `EVENT_LISTENER` component tolerates the absence of that PU is **not determinable from this repo** —
   no framework source is checked out here. A wrong answer surfaces at deploy time, after the merge,
   in the same window where the Docker image is already expected to be flaky (FR19). That is the worst
   possible place to discover it.
3. **FR22's own instruction points here.** "A module deleted by accident is worse than a dependency
   list trimmed too cautiously," and "it ships a `persistence.xml` and a `beans.xml` that the runtime
   may still require." The runtime requirement could not be *disproved*; absent disproof, retain.
4. **Retention is reversible in one commit; deletion is not.** Per the mechanics ADR's own posture on
   the parallel `anonymise` question (decision 6, "default if unresolved: retain and migrate").

**Rejected — delete the module.** It is the intellectually cleaner answer for a module with no Java,
and it may well be correct. But it converts a build-time question into a deploy-time one, in the story
whose definition of done is already "a QA image that is expected to fail first time." Record it as the
follow-up in §N-6: revisit when the read-model is actually wired (this repo's CLAUDE.md describes the
viewstore as "in place ready for read-model wiring", and `pcfdlrm.xml` currently has no changesets),
at which point the module either grows entities or is provably inert.

**FR16 (`h2`) is confirmed a no-op.** The only JDBC URL in the repo is `jdbc:h2:mem:test` inside
`apache-deltaspike_test-container.properties`, which AC13 deletes; the `h2` dependency has no local
version and no remaining consumer, so it is deleted rather than bumped. There is no MVCC/MV_STORE
setting anywhere. **No `h2` version work is needed. Do not spend time on it.** *(The reference's diff
also deleted a local `<h2.version>1.4.196</h2.version>` property — this repo has no such property, so
that part does not apply.)*

### Sequencing note

FR21 is a **hard dependency-resolution failure**: `mvn clean install` dies on this module before
compiling anything. It must therefore be in the **first** implementation commit, not batched with the
namespace sweep. See §M-T2.

---

## G. FR9 / AC6 — CDI and persistence descriptors

### `beans.xml` — 12 files, all identical in shape

Verified: exactly 12, all on `xmlns="http://xmlns.jcp.org/xml/ns/javaee"`, all `version="1.1"`, and
**all 12 already carry `bean-discovery-mode="all"`**.

| Module | Path |
|---|---|
| `pcfdlrm-command-api` | `pcfdlrm-command/pcfdlrm-command-api/src/main/resources/META-INF/beans.xml` |
| `pcfdlrm-command-handler` | `pcfdlrm-command/pcfdlrm-command-handler/src/main/resources/META-INF/beans.xml` |
| `pcfdlrm-domain-aggregate` | `pcfdlrm-domain/pcfdlrm-domain-aggregate/…` |
| `pcfdlrm-domain-event` | `pcfdlrm-domain/pcfdlrm-domain-event/…` |
| `pcfdlrm-event-listener` | `pcfdlrm-event/pcfdlrm-event-listener/…` |
| `pcfdlrm-event-processor` | `pcfdlrm-event/pcfdlrm-event-processor/…` |
| `pcfdlrm-healthchecks` | `pcfdlrm-healthchecks/…` |
| `pcfdlrm-query-api` | `pcfdlrm-query/pcfdlrm-query-api/…` |
| `pcfdlrm-query-view` | `pcfdlrm-query/pcfdlrm-query-view/…` |
| `pcfdlrm-refdata` | `pcfdlrm-refdata/…` |
| `pcfdlrm-service` | `pcfdlrm-service/…` |
| `pcfdlrm-viewstore-persistence` | `pcfdlrm-viewstore/pcfdlrm-viewstore-persistence/…` |

**Decision:** apply the reference's exact replacement, byte for byte, to all 12:

```xml
<beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
       version="4.0" bean-discovery-mode="all">
</beans>
```

`bean-discovery-mode="all"` is retained explicitly on every one. This is the single line keeping BC-14's
*Refuted* verdict alive (the parity ADR, Bucket B); the report's own evidence is that Weld 6 honours it
"even on the legacy namespace", so the namespace move is safe *because* the attribute survives it.
Losing it on even one module would silently empty that module's interceptor chain.

**AC6 gate:**
`grep -rL 'bean-discovery-mode="all"' $(find . -name beans.xml -not -path '*/target/*')` → empty, and
`grep -rl 'xmlns.jcp.org' --include=beans.xml .` → empty.

### `persistence.xml` — one file, and a divergence from the reference to make deliberately

This repo has one, in `pcfdlrm-viewstore-persistence`, on
`xmlns="http://java.sun.com/xml/ns/persistence"` version `1.0`.

**The reference did not migrate its main `persistence.xml` namespace.** Read at
`122a5a8fdc` and still at its branch tip, `prosecutioncasefile-viewstore-persistence`'s main
`persistence.xml` remains on `http://java.sun.com/xml/ns/persistence` version `1.0`; the only Jakarta
`persistence_3_0` namespace it uses is in the **test** `persistence.xml` it added.

**Decision — reversed 2026-09-09, leave it on the legacy 1.0 namespace. Do not migrate.**

Owner's call at the Stage 2 gate: this module has no persistence to speak of (no entities, and §F/§N-6
already treats the whole module as a future removal candidate, unused today), so it doesn't warrant
FR9's effort or the WildFly-40-rejection risk the migration would introduce for zero functional benefit.
Cross-checked `cpp-context-stagingdlrm` at the same request: its equivalent `persistence.xml` is
byte-identical in shape (one PU, no entities) and also on the 1.0 namespace — but that repo hasn't
started its own jakarta migration at all yet (its `beans.xml` is still on `xmlns.jcp.org` too), so it's
not independent precedent, just the same unmigrated starting state this repo is in now. The only actual
precedent is the reference (`cpp-context-prosecution-casefile`), which left its equivalent file on 1.0
even *after* completing its full jakarta migration — so leaving this one alone matches the one real
data point available, and needs no revert-readiness because nothing changes.

FR9 is therefore **not satisfied for this one file**, recorded here as a deliberate, scoped deviation —
not an oversight — with the reasoning above as the justification a reviewer would otherwise ask for.

**No test `persistence.xml` is added.** The reference added one because it has entities and
repository tests. This module has neither. FR21's note that "the reference … added a test
`persistence.xml` in its place" is correct about the reference and does not transfer.

---

## H. FR10 / FR11 / AC2 — code generation

### Where the generators actually run — 9 poms

| Pom | Plugins declared |
|---|---|
| `pcfdlrm-datatypes-common` | `catalog-generation-plugin` |
| `pcfdlrm-domain-value-schema` | `catalog-generation-plugin` |
| `pcfdlrm-domain-event` | `catalog-generation-plugin`, `pojo-generation-plugin` |
| `pcfdlrm-domain-aggregate` | `pojo-generation-plugin` |
| `pcfdlrm-domain-event-processor` | `pojo-generation-plugin` |
| `pcfdlrm-command-handler` | `pojo-generation-plugin`, `rest-client-generator-plugin` |
| `pcfdlrm-command-api` | `messaging-client-generator-plugin`, `rest-client-generator-plugin` |
| `pcfdlrm-event-processor` | `messaging-client-generator-plugin`, `rest-client-generator-plugin` |
| `pcfdlrm-event` *(aggregator, `packaging=pom`)* | `messaging-adapter-generator-plugin`, `messaging-client-generator-plugin` — **inherited by `event-listener` and `event-processor`** |

RAML lives in two places only: `pcfdlrm-command/pcfdlrm-command-api/src/raml` and
`pcfdlrm-command/pcfdlrm-command-handler/src/raml`.

### FR10 — revised, on reference evidence

FR10 names two recorded fleet fixes. Both need adjusting against what the reference actually did:

- **`rest-client-generator-plugin` needs the jakartaee-api swap** — **confirmed and expanded.** The
  reference swapped `javaee-api` → `jakarta.platform:jakarta.jakartaee-api` in every generator-plugin
  block **and added `jakarta.xml.bind:jakarta.xml.bind-api` alongside it** (six blocks, across
  `command-api`, `command-handler` ×2, `event-processor` ×2, `query-api`). The `jakarta.xml.bind-api`
  addition is not mentioned in FR10 but is part of the same fix — a RAML/JAXB codegen dependency the
  removal of `javaee-api` no longer supplies. **Add it to all five of this repo's plugin blocks**
  (§B2), using `${jakarta.xml.bind-api.raml.version}`, which the parent supplies.
- **`messaging-client-generator-plugin` needs parsson in its plugin dependencies** — **likely already
  fixed upstream, verify before adding.** `122a5a8fdc`'s own commit message reads: *"Remove parsson
  workaround (now included in service-parent-pom M4)"*, and the reference carries no parsson
  coordinate anywhere at its branch tip on M10. FR2 targets M10. **Decision: do not pre-emptively add
  parsson.** Build first; add it only if code generation actually fails, and record it if so. Adding a
  workaround the parent already carries is how a repo ends up with a duplicate JSON-P provider on the
  classpath — the very collision BC-11 describes.

### FR11 / BC-21 — the generated-artefact inventory

The parity story's BC-21 assertion is the gate. `AC2` (full reactor `mvn clean install`) plus that
assertion is the whole of FR11's mechanism here — this story adds no new codegen test.

Two things for the implementer to hold:

- The parity design scoped each BC-21 check to **its own module's package prefix**, because
  RAML-referenced external schemas generate classes under unrelated packages in the same
  `target/generated-sources` tree. A shrinking count outside the module's prefix is *not* this story's
  defect; a shrinking count inside it is.
- The blast radius, re-derived: `pojo` ×4 modules, `catalog` ×3, `messaging-client` ×3
  (`command-api`, `event-processor`, and the `pcfdlrm-event`-inherited run), `rest-client` ×3,
  `messaging-adapter` ×1 — matching FR11's figures.

---

## I. FR12–FR15, FR19 / AC7, AC11 — build, pipeline, packaging, image

### FR12 / AC7 — the pipeline track

Current `azure-pipelines.yaml`: agent demand `identifier -equals centos8-j17` (line 29), template
`ref: 'main'` (line 24), **no `aksDeployBranch` parameter at all**.

Target — the reference's end state, read from its branch tip:

| Line | From | To |
|---|---|---|
| 24 | `ref: 'main'` | `ref: 'wildfly40'` |
| 29 | `identifier -equals centos8-j17` | `identifier -equals ubuntu-j25` |
| under `context-validation.yaml` parameters | *(absent)* | `aksDeployBranch: 'wildfly40'` |

**Sequencing evidence worth acting on.** The reference did **not** do all three in its upgrade commit.
`122a5a8fdc` changed only the agent demand, and to the *intermediate* value `ubuntu-j25-postgres`; two
later commits — `dbed71d8` ("azure-devops-templates ref is updated to wildfly40") and `c834ff3a`
("azure-pipelines: image changed to ubuntu-j25") — reached the end state. That is the "expect a
follow-up pipeline PR" pattern of FR19 and mechanics-ADR decision 7, observed rather than predicted.

**Decision: make all three changes in the upgrade PR** (they are three lines and there is no reason to
stage them), but **plan the follow-up PR regardless** — see §M-T8. Do not treat the upgrade PR merging
green as evidence the image track works; the image step does not run on a PR.

### FR13 — jacoco

**No jacoco plugin declaration or version property exists anywhere in this repo's poms** (only the
`sonar.jacoco.itReportPath` sonar property, which is unrelated). And — worth knowing before writing a
task for it — **the reference carries no jacoco override either**, on M10.

**Decision: verify-then-act, not act-then-verify.** Run the reactor on M10 first. If coverage
generation works, add nothing and record why (FR4 forbids opportunistic changes). If it fails on JDK 25
bytecode, add a local `jacoco-maven-plugin` `<version>0.8.14</version>` override in the root pom's
`pluginManagement`. Sized as a conditional sub-task of §M-T5, not a standalone story.

### FR14 — `jboss-deployment-structure.xml`

**Decision: do not add one.**

FR14 asks whether the WildFly 40 module set requires it, noting the reference "added one at its root."
It did — and the file it added is:

```xml
<jboss-deployment-structure xmlns="urn:jboss:deployment-structure:1.2" …>
    <deployment/>
</jboss-deployment-structure>
```

— an **empty `<deployment/>`**, at the **repository root**, not in `src/main/webapp/WEB-INF/`. It
excludes nothing, adds nothing, and is not packaged into the WAR by any build step this repo has. It is
functionally inert. Copying it would add a file that looks meaningful, is not, and will confuse the
next reader.

Consequently **BC-12's flagged deploy-breaker cannot arise here**: that hazard is a descriptor which
*disables the `jaxrs` subsystem* combined with `packagingExcludes` stripping bundled RESTEasy. This repo
has neither — no `packagingExcludes` anywhere, and its only RESTEasy coordinate is
`org.jboss.resteasy:resteasy-multipart-provider` at `pcfdlrm-integration-test/pom.xml:66`, IT scope,
never packaged into the WAR. Record BC-12 as N/A-by-construction.

**Revisit only if** the WildFly 40 deploy actually reports a module-visibility failure — at which point
the descriptor written is a real one with real `<exclusions>`, not the empty template.

### FR15 / AC11 — where the image comes from

**Resolved: this repo already ships a Dockerfile, and FR15's premise needs correcting.**

FR15 states "There is no `Dockerfile` at this repo's root." True, but incomplete —
**`docker/Dockerfile_pcfdlrm-service` exists**, alongside `docker/scripts/liquibase.sh`. This is
precisely the reference's pattern (`docker/Dockerfile_prosecutioncasefile-service` + `docker/scripts`),
so the third of FR15's three candidate patterns applies, not the `support`/`system-id-mapper`/`notification`
"no Dockerfile at all" one.

Its content, verified: `ARG baseImageUri` / `baseImageTag` with `FROM ${baseImageUri}:${baseImageTag}` —
**the base image is injected by the pipeline, not hard-coded**. So the fleet's "Dockerfile base →
Ubuntu 24.04, remove the RHEL `yum` lines" item has **no target here**: there is no `FROM` literal to
change and no `yum` line anywhere in the file. The file's remaining content is `ADD`s of the WAR and six
liquibase/tooling JARs from `${mavenArtifactBaseUrl}`, `chown`/`chmod`, and a `COPY scripts/liquibase.sh`.

**Decision:**
- **Change nothing in `docker/Dockerfile_pcfdlrm-service` in the upgrade PR.**
- **Path resolution — resolved 2026-09-09, read directly from `hmcts/cpp-azure-devops-templates`
  (`wildfly40` branch) via `gh api`.** `context-validation.yaml`'s `dockerfilePath` parameter (default
  `'Dockerfile'`) is a red herring for this repo: it only feeds `steps/common/build-docker-image.yaml`,
  which is wired into the `cp-ai-rag-service`-specific `BuildMigrationTool` job and the generic
  `BuildDockerImage` stage — **neither is on pcfdlrm's path**. The `ContextValidation` stage (the one
  `context-validation.yaml` actually runs for every normal context, pcfdlrm included) calls
  `steps/common/docker-build.yaml` instead, which takes no `dockerfilePath` parameter at all and
  resolves the file itself: `cd docker; for dockerfile in $(find . -type f -name "Dockerfile_*"); do …`.
  It globs anything named `Dockerfile_*` under `docker/` — no service-name convention required.
  `docker/Dockerfile_pcfdlrm-service` matches that glob as-is (and so does `cpp-context-stagingdlrm`'s
  identically-shaped `docker/Dockerfile_stagingdlrm-service`, confirmed by inspection — same shape,
  same repo family, though that sibling hasn't touched its own pipeline track yet so it isn't a T6
  precedent, just a second data point). **T6 needs no `dockerfilePath` override — there is nothing to
  pass.** This removes what was flagged as the single most likely cause of FR19's "image build failed"
  budget; no longer an open question (was §N-2).
- Base-image ARGs (`baseImageUri`/`baseImageTag`) are supplied by the `wildfly40` template track and are
  how the image becomes a WildFly 40 image. Nothing repo-side selects it.

---

## J. FR17 / AC8 — the Liquibase deploy blocker

`pcfdlrm-viewstore/pcfdlrm-viewstore-liquibase/src/main/resources/liquibase.properties`, verified — the
whole file is three lines:

```
changelogFile: liquibase/pcfdlrm.xml
liquibase.hub.mode: off
liquibase.headless: true
```

**Decision, and severity corrected 2026-09-09 — this is a hygiene cleanup, not a deploy blocker.**
Delete line 2. Verify line 3 by running Liquibase 5 against the file rather than by reasoning about it
— `liquibase.headless` is a lower-confidence call and the report says so.

Findings that change the original framing:

- **The pinned-key-set test does not exist on this branch.** The parity stage did write it, run it
  green, then explicitly revert it two commits later — `123c0e0` ("restore liquibase.hub.mode, out of
  scope for tests-only work") and `092017d` ("remove redundant/ineffective parity tests per PR
  review") — to keep that stage's diff scoped to tests-only, per this repo's own scope discipline. The
  parity checklist records this (BC-07 row: "Still reverted"). Confirmed by direct grep on this branch:
  no `LiquibasePropertiesKeyInventoryTest`, no matching class. **There is nothing to update in T7's
  commit** — the earlier framing ("update the pinned key set in the same commit") is stale.
- **The parity checklist's own premise correction already showed this isn't a hard failure**: an
  unsupported key only logs a warning under non-strict Liquibase config (which this repo uses — no
  `GlobalConfiguration.STRICT` anywhere), tested against real 4.10.0 *and* 5.0.4 jars. It does not crash
  the pre-install job.
- **Cross-checked against the actual reference repo's completed upgrade** (`cpp-context-prosecution-casefile`,
  `team/25.104.x`, confirmed via its Jakarta 4.0 `beans.xml` and confirmed to contain the real upgrade
  commit `122a5a8fdc`): its equivalent `liquibase.properties` **still has `liquibase.hub.mode: off` in
  place**, untouched since the initial migration commit. The reference did not remove it as part of
  its own finished J25 upgrade, and evidently that hasn't blocked anything for them.
- The changelog it points at, `liquibase/pcfdlrm.xml`, currently has **no changesets**. So even the
  warning-level exposure has no data-migration risk behind it.
- Still worth doing — 5.0.4 does genuinely drop the key from its defaults, so it's a real, if low-severity,
  piece of drift to clean up — but FR17's "deploy blocker" framing overstates it, and no test-authoring
  is needed since none existed to begin with.
- Standalone and independently deliverable — see §M-T7.

---

## K. FR20 / AC12 — integration tests

Two IT classes, `ReceiveMigratedCaseFileIT` and `AddMaterialIT`, against 103 `*Test.java` unit classes.
`runIntegrationTests.sh` derives `framework`, `event-store` and `framework-libraries` versions from the
poms and drives `$CPP_DOCKER_DIR/containers/wildfly/deployments`.

**Resolved 2026-09-09 — no blocker.** The stale-clone caveat from an earlier pass is superseded: fetched
`cpp-developers-docker` fresh and it now carries a real `java-25` branch (tip `daa1829`, previous commits
`802af79` "Wire Camunda 7.24 / WildFly 40 IT stack", `c083279` "...fix WF40 report-directory"), plus a
`dev/java-25-wildfly-40-upgrade-spike` branch and `release/25.104.x`. Diffed `java-25` against `java-17`:
55 lines changed in `containers/wildfly/Dockerfile`, a new `WILDFLY-32-MIGRATION.md`, and
`docker-compose.yml`/`standalone.xml` updated for WF40 — this is real IT-stack work, not a stub branch.

The image itself is confirmed **published and current**, checked directly against ACR
(`az acr repository show-tags --name crmdvrepo01 --repository hmcts/wildfly`): both
`40.0.0.Finaljdk25_latest` (non-Camunda, the tag pcfdlrm's own docker-build.yaml math produces — see
§I) and `40.0.0.Finaljdk25_Camunda7.24_latest` exist, with daily-dated tags through **today's date**
(`090926` = 2026-09-09). `cpp.pipeline`'s `aks-pipeline.versions.yml` on its `master` branch already
carries `widlfly40_base_tag: latest`, wiring the two together — though note it is on `master`, not
`main`, which is the branch the ADO template resource actually checks out at
`$(Build.SourcesDirectory)/cpp.pipeline`; **worth a one-line confirmation which branch that resource
tracks**, since `main` (checked directly) does not yet carry the `widlfly40_base_tag` key.

**Decision, updated:**
1. No fetch-and-check gate needed at stage 5 — the image exists today. Drop that pre-step from T9.
2. Run both ITs against it and record the result (AC12 satisfied either way — passing, or failing with
   the failure described).
3. The only residual risk is the `cpp.pipeline` `master`-vs-`main` branch question above — small, and
   answerable in the same five minutes as the item it replaces. Not a blocker; note it in T9's story.
4. Still hold the requirements' own caution: 2 IT classes is cheap to run and weak as evidence. A green
   IT run here is not the assurance it would be in a context with 30.

The repo-root `mvn clean && ./runIntegrationTests.sh` rule in the orchestrator CLAUDE.md applies to PRs
that add or change an endpoint. **This story adds no endpoint** — it changes no RAML, no `@Handles`
handler, no descriptor subscription. So no *new* integration test is required; the existing two must
still be run or the blocker recorded.

---

## L. FR1a / FR3 / AC3 — the parity gate

FR1's precondition is **satisfied**: the parity work merged as `6a131c1` (PR #28) and sits on
`team/25.104.x` beneath `6ab1b90` ("New 17.104.24-DLRMJ25-SNAPSHOT"), with no jakarta migration on the
branch. AC1 is discharged by branching this story's work from the current tip.

Per §C, the parity tests carry **zero `javax` imports**, so the sweep does not touch them. What remains:

- **Run them on JDK 25 and read the result as evidence, not as a chore.** BC-08 is this repo's primary
  parity item and it lands in **main** code — `MigratedCaseFileAggregate` and the `…ToCC…Converter`
  classes on the outbound payload path. A red assertion means a migrated hearing timestamp would have
  been silently wrong across a context boundary.
- **A red parity test stops the story.** Not "adjust and continue" — record the divergence, raise it.
  The one exception is the BC-07 `liquibase.properties` key-set test, which §J changes deliberately and
  which is a configuration inventory, not a behavioural pin.
- `docs/j25-parity-checklist.md` is the register of what the gate actually covers. Update its status
  column as part of this story so the next reader can tell J17-green from J25-green.

---

## M. Task breakdown for stage 3

Ordered by dependency, not by size. T1–T6 are one PR (the upgrade PR); T7 and T8 are separately
deliverable. Each is written so a story-writer can lift it into a story with its own AC.

| # | Task | Discharges | Depends on | Notes |
|---|---|---|---|---|
| **T0a** | **Bump `pcfdlrm-parent`'s own version** from `17.104.x` to `25.104.x-DLRMJ25-SNAPSHOT` across all poms — `service-parent-pom` parent-dependency version untouched, nothing else in the diff. | §N-1 | — | Standalone, single-purpose PR, shaped after `cpp-context-stagingdlrm` PR #54 (`f2ef772`/`d8bb295`, DD-43192) — 26 poms, one line each. Confirm the `25.104.x-DLRMJ25-SNAPSHOT` scheme with the branch owner first; not otherwise blocking. |
| **T0** | **Reconfirm the milestone pins.** Check `service-parent-pom` and `coredomain` against the PEG-3296 tracker; take the newer if they have moved past `25.104.0-M10` / `25.104.0-M11`. | FR2 | — | Read-only, minutes. The reference sits on exactly M10/M11 today. |
| **T1** | **Run the BC-15 field check and record it.** Re-run the 7-literal grep from §E; paste the result into the PR. | FR18, **AC10** | T0 | Read-only. §E has already done it once and found it clear; T1 is the re-run on the stage-5 branch. **Must precede T3.** |
| **T2** | **Unblock dependency resolution.** Trim `pcfdlrm-viewstore-persistence` to the seven coordinates in §F; delete `apache-deltaspike_test-container.properties`; re-group `hibernate-core`; re-coordinate `jaxb-api`. | FR21, FR16, FR22, **AC13**, **AC14** | — | **First commit.** `mvn clean install` cannot get past this module until it lands. §F is AC14's recorded reasoning. |
| **T3** | **Bump the platform chain.** `service-parent-pom` → M10, `coredomain` → M11; move `referencedata` / `progression` / `sjp` / `resulting` / `defence` / `material` / `notification.notify` pins **only as far as `RequireLatestMojInterfaceRule` demands**. | FR2, FR4 | T0, T1, T2 | Expect the enforcer to name each stale pin. Do not pre-bump. Leave the unused `stream-transformation-*` `dependencyManagement` entries alone — verified consumed by nothing in this repo, and the reference kept its own. |
| **T4** | **The Jakarta swap.** All 15 `javaee-api` sites (§B); the 5 JSON-provider coordinates (§C); `jakarta.xml.bind-api` added to all 5 plugin blocks (§H); the 5-prefix import sweep with its pre-flight FQN diff (§C); all 12 `beans.xml` (§G). **`persistence.xml` deliberately excluded — left on its legacy namespace, see §G/§N-5.** | FR6, FR7, FR8, FR9 *(partial — beans.xml only, persistence.xml deliberately out of scope, §G)*, FR10, **AC4**, **AC5**, **AC6** | T2, T3 | The bulk of the diff. Splittable into T4a (poms/coordinates) and T4b (imports + descriptors) if the reviewer wants two readable commits; keep them in one PR so the reactor is never half-migrated. |
| **T5** | **Green the reactor.** `mvn clean install` on JDK 25 across all modules; run the parity suite; add a jacoco 0.8.14 override **only if** coverage actually fails (§I). | FR3, FR11, FR13, **AC2**, **AC3** | T4 | Where BC-08 either holds or stops the story. The codegen inventory (FR11) is asserted by the parity tests, not by new code here. |
| **T6** | **Pipeline track.** Three lines in `azure-pipelines.yaml`: `ref: 'wildfly40'`, `identifier -equals ubuntu-j25`, `aksDeployBranch: 'wildfly40'`. No Dockerfile change (§I). | FR12, FR14, FR15, **AC7** | T5 | Include in the upgrade PR. Its effect on the image is unobservable until merge. |
| **T7** | **Liquibase fix.** Delete `liquibase.hub.mode: off`; verify `liquibase.headless` against Liquibase 5; update the parity key-set test in the same commit. | FR17, **AC8** | none | **Independently deliverable and touches nothing T2–T6 touch.** Can ship before, alongside or after the upgrade PR. Good candidate for a separate small story — it is a live deploy blocker on this branch *regardless* of the upgrade. |
| **T8** | **Image / follow-up PR.** After the upgrade PR merges: watch `context-validation.yaml`'s `ContextValidation` stage, confirm the `docker-build.yaml` glob picks up `docker/Dockerfile_pcfdlrm-service` (path resolution itself is no longer a risk — see §I), fix whatever the image build reports, record the published tag. | FR19, **AC11** | T6 merged | **Plan it as a story now, not as an afterthought.** Nine contexts on the tracker are "merged, no image" — the WildFly 40 base image / buildah pipeline itself is unproven territory even though path resolution is now confirmed. |
| **T9** | **Integration tests.** WildFly 40 image confirmed published in ACR (§K) — run both ITs against it and record the result. | FR20, **AC12** | T5 | Can run in parallel with T8. No longer blocker-shaped, but still worth confirming which `cpp.pipeline` branch (`master` vs `main`) the ADO resource checks out — see §K. |

Suggested story split for stage 3, if three stories are wanted rather than one:

- **Story A (the upgrade)** — T0–T6. One PR. Definition of done: `mvn clean install` green on JDK 25,
  parity green, AC1–AC7 + AC13/AC14 satisfied.
- **Story B (the deploy blocker)** — T7. One small PR, no dependency on A.
- **Story C (the image)** — T8 + T9. Starts only after A merges. Definition of done: AC11 + AC12.

That split respects the repo's one-task-at-a-time convention and keeps the PR that changes 15 poms and
58 source files free of unrelated content.

---

## N. Open questions

None of these is invented; each is a gap surfaced while verifying something else. All are for the
Stage 2 human gate.

1. **Project version scheme — superseded by sibling precedent, checked 2026-09-09.** This repo is at
   `17.104.24-DLRMJ25-SNAPSHOT`. Stage 1 read `cpp-context-stagingdlrm`'s `team/25.104.x` as matching, on
   `17.104.25-DLRMJ25-SNAPSHOT`, and this section originally recommended leaving the scheme alone on that
   basis. **That is now stale.** `cpp-context-stagingdlrm` (`~/moj/cpp-context-stagingdlrm`,
   `team/25.104.x`, up to date with origin) merged PR #54 on 2026-09-08 (`f2ef772` → `d8bb295`, DD-43192)
   bumping its own artifact version — `stagingdlrm-parent`'s `<version>`, not its `service-parent-pom`
   parent dependency, which correctly stayed at `17.104.1` — from `17.104.x` to
   **`25.104.26-DLRMJ25-SNAPSHOT`** across all 26 poms, one line each, in a PR scoped to *only* that
   change ("Nothing else touched: service-parent-pom stays at 17.104.1, no Java target change, no
   jakarta rename... deliberately scoped to just the version bump, not the rest of the DD-43192 upgrade
   stage"). The branch has since moved on to `25.104.27-DLRMJ25-SNAPSHOT` (`b23ea10`).
   **Revised recommendation: follow suit.** The sibling DD-43192 story treats the `17.104.x`→`25.104.x`
   version-prefix bump as its own first, standalone, single-purpose PR — the same "one task at a time"
   shape this repo's stories already use (§M). This is real precedent, not a guess to be confirmed later:
   add a **T0a** ahead of T0 — "bump `pcfdlrm-parent`'s own `<version>` from `17.104.x` to
   `25.104.x-DLRMJ25-SNAPSHOT` across all poms, `service-parent-pom` parent-dependency version untouched,
   nothing else in the diff" — sized and shaped identically to stagingdlrm's #54. Still worth a one-line
   confirmation with whoever owns the branch that both DLRM repos are meant to converge on the same
   `25.104.x-DLRMJ25-SNAPSHOT` scheme, but this is no longer an open question blocking T8 — it is a task
   with a working template already merged in the paired repo.
2. **`dockerfilePath` — resolved 2026-09-09.** See §I: read `hmcts/cpp-azure-devops-templates` on the
   `wildfly40` ref directly. `docker-build.yaml` (pcfdlrm's actual path) globs `Dockerfile_*` under
   `docker/`; the `dockerfilePath` parameter belongs to a different template not on this repo's path.
   No override needed in T6. Not an open question any longer.
3. **WildFly 40 developer image — resolved 2026-09-09.** Fetched `cpp-developers-docker`: a real
   `java-25` branch exists with substantive WF40 IT-stack work. Confirmed in ACR: `40.0.0.Finaljdk25_latest`
   is published and current (dated tags through today). See §K. No longer gates T9/AC12 as a blocker —
   only the small `cpp.pipeline` `master`-vs-`main` branch question remains, noted there.
4. **Deployment ordering against Progression — narrowed 2026-09-09, still open.** §D establishes there
   is no build-time coupling, and confirms concretely (not hypothetically) that `cpp-context-progression`
   has **no J25/coredomain-M11 branch at all** — its `main` sits on `coredomain 17.103.13`. So a sandbox
   deploy of this story's build (Stage 8) will send M11-shaped payloads to a progression instance that,
   today, would be running the pre-upgrade line. Whether that is actually safe depends on progression's
   own inbound JSON schema for `initiate-court-proceedings` (additive fields are usually tolerant, but
   "usually" isn't a gate) — unreadable from this repo, since `cpp-context-progression` is a separate
   codebase on a separate cadence. **Recommendation: this is a one-line question for whoever owns the
   `progression` context or the sandbox environment schedule** ("is `initiate-court-proceedings`'s schema
   on progression's deployed line tolerant of the M11 field additions, or does progression need its own
   parallel bump first") — raise it at the Stage 2 gate as a named cross-team dependency, not left to
   surface itself at Stage 8. Still correctly out of this story's own scope; the ask is "name an owner
   and ask the question now", not "resolve it here".
5. **`persistence.xml` namespace — resolved 2026-09-09, owner's decision at the gate.** Leave it on the
   legacy 1.0 namespace; do not migrate. The module has no persistence to speak of (no entities, and is
   itself a future removal candidate per §N-6), so FR9's migration isn't worth the WildFly-40-rejection
   risk for zero functional benefit. Cross-checked `cpp-context-stagingdlrm` — same shape, also on 1.0,
   but only because it hasn't started its own jakarta migration yet, so not independent precedent; the
   reference (which left this file alone even after fully migrating) remains the one real data point,
   and this decision now matches it. FR9 recorded as deliberately partial (§G, T4). Not an open question
   any longer.
6. **`pcfdlrm-viewstore-persistence` deletion — resolved 2026-09-09, confirmed by branch owner.**
   Confirmed unused today and a candidate for removal on its own timeline, independent of this epic.
   §F's retain-and-trim decision for **T2 stands unchanged** — this story is a like-for-like upgrade,
   not the moment to remove a module whose runtime dependency could not be disproved from this repo
   (§F-2), and deletion is a separate, larger-blast-radius change than trimming dead coordinates.
   No follow-up ticket needed: the owner already knows and will remove it separately when ready. Not
   an open question any longer.
7. **Owner unassigned.** `prosecution-casefile-dlrm` still shows owner "?" on the PEG-3296 tracker
   (carried from the input brief; nothing in this repo resolves it).

---

## What this design doc does not do

- **No code, pom, descriptor or CI file is modified.** Every line-numbered target above is a statement
  of intent for stage 5.
- **No FR or AC is changed.** §A corrects five *counts*; the requirements text stands, and the greps
  named in the ACs remain the gates. Where §D and §I-FR15 find an FR's premise factually wrong (no
  upstream Maven pin exists; a Dockerfile does exist), the FR is discharged by recording the finding —
  AC9 and AC11 are satisfied by evidence, not by a change.
- **No ADR is written.** FR5 looked like it needed one and does not: §D resolves it to "no pin exists",
  which is a finding, not a trade-off. FR22 is a genuine trade-off but is scoped to one module in one
  repo, so it is recorded here per AC14 rather than promoted. If the gate disagrees on either, the
  natural filename is `docs/pipeline/adrs/DD-43194-<slug>.md`.
- **No parity test is authored or altered** beyond the one deliberate key-set update in §J.

# User stories — DD-43194: J17→J25 behavioural-parity tests for PCFDLRM

> Stage 3 artefact (user stories). Source: [`01-requirements.md`](./01-requirements.md) (FRs/ACs),
> [`02-design.md`](./02-design.md) (file/class-level decisions).
>
> **One Jira ticket, six implementable tasks (of eight planned).** Per the input brief's decision
> table, this repo gets a single Jira key (DD-43194) for the whole parity effort — these are
> sprint-sized sub-tasks under that ticket, not separate tickets, following this repo's existing
> T1…Tn convention. **T6 and T7 were cut at this stage** — see their entries below for why. Numbering
> is kept stable (gaps left at T6/T7) so this doc, `02-design.md`, and future PR references don't
> drift out of sync.

## How to read this doc

Each task: a GDS-format story, the FR/AC it satisfies, the exact file(s) from `02-design.md`, and a
Definition of Done. Every task is independently committable — none blocks another except where
stated.

---

## T1 — Pin BC-08's zone identity on both outbound carriers (primary item)

**As a** developer who will move PCFDLRM's converters to Java 25,
**I want** the zone identity of both `ZonedDateTime` values reaching the outbound CC payload pinned
against the current J17 behaviour,
**so that** a silent `ZoneId`→`ZoneOffset` drift in Jackson corrupts a build, not a migrated hearing.

**Satisfies:** FR5, FR6, AC3.

**Scope (from `02-design.md` §A1/A2):**
- `ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverterTest` — new test
  `shouldPinZoneIdentityOnListedStartDateTime()`: assert `getListedStartDateTime().getZone()` equals
  the region `ZoneId.of("UTC")`, and the serialized form via
  `new ObjectMapperProducer().objectMapper()` matches the observed J17 rendering.
- `ProsecutionCaseFileMigratedDefendantToCCDefendantConverterTest` — new test
  `shouldPinZoneIdentityOnCourtProceedingsInitiated()`: same pattern for `courtProceedingsInitiated`,
  asserting zone identity and rendering only — never the non-deterministic instant.

**Definition of done:**
- [ ] Both new tests exist and pass on J17 (`mvn -pl pcfdlrm-event/pcfdlrm-event-processor test`).
- [ ] Each test names BC-08 and its carrier (C1/C3) in a comment (FR2).
- [ ] Result recorded in `docs/j25-parity-checklist.md` with the exact `mvn` command and outcome.
- [ ] No production code changed (AC10).

---

## T2 — Pin the `java.time` DST annotation (FR7/AC4); A4 payload-boundary assertion CUT

**As a** developer relying on this suite as the upgrade's regression gate,
**I want** the existing `Europe/London` DST characterization correctly labelled,
**so that** the checklist doesn't under- or over-claim what's already covered.

**Satisfies:** FR7, AC4. (FR8/AC5 — the payload-boundary assertion — is cut; see below.)

**Scope (from `02-design.md` §A3):**
- `MigratedCaseFileAggregateTest.fixedHearingTimeDefaultingScenarios()` +
  `shouldDefaultHearingTimeTo10AmOnlyForFixedHearingWithNoWarnings` — **no new test**; add a comment
  on the method and the `FUTURE_HEARING_DATE_GMT`/`_BST` scenario rows labelling them as the FR7
  `java.time` pin (not a BC-08 carrier). **Correction to `02-design.md` §A3:** the design doc names
  this method's home as a separate `AggregateScenarios.java` class (`AggregateScenarios
  .fixedHearingTimeDefaultingScenarios()`) — that class only exists on the LIBRA branch
  (`dev/dd-43191-j25-stage1`). On this `team/25.104.x`-based branch,
  `fixedHearingTimeDefaultingScenarios()` is a private method inline in `MigratedCaseFileAggregateTest`
  itself (currently around line 1425) — same doc-cherry-pick-without-code cause as elsewhere in this
  story doc.

**Original A4 scope (from `02-design.md` §A4), CUT:** narrow
`MigratedCaseReceivedProcessorTest.shouldConvertAndSendInitiateCourtProceedings`'s blanket exclusion of
`courtProceedingsInitiated`/`listedStartDateTime` (lines 90–94) to a targeted pattern assertion on the
rendered zone suffix.

**Why cut:** that method doesn't exist on this branch. `MigratedCaseReceivedProcessorTest` here is 87
lines, unchanged since the `team/25.104.x` base — no whole-payload comparison, no exclusion list to
narrow. The 769-line version with `shouldConvertAndSendInitiateCourtProceedings` exists only on the
LIBRA branch, built on the whole-payload-comparison machinery (`WholePayloadMatcher` /
`pcfdlrm-test-support`) that this repo's `CLAUDE.md` branch note already excludes from
`team/25.104.x` — the same reason T6/BC-13 was cut. AC5's payload-boundary assertion has no seam to
attach to here without first porting that LIBRA-only infrastructure, which is out of scope for a
tests-only story.

**Disposition:** record FR8/AC5 as 🔴 in `docs/j25-parity-checklist.md` under BC-08's row (not a
separate BC — it was always a sub-item of BC-08), citing this reason, when T2 runs. Don't leave it
silently dropped, per the T6/T7 convention.

**Definition of done:**
- [ ] The FR7 annotation is in place and cited in the checklist as 🟢 with the existing test names —
      not duplicated.
- [ ] FR8/AC5 recorded as 🔴 in the checklist with the cut reason (above).
- [ ] No production code changed (AC10).

**Depends on:** nothing (different module from T1 — `pcfdlrm-domain-aggregate`, not
`pcfdlrm-event-processor` — the original "depends on T1" reasoning applied only to the now-cut A4
item).

---

## T3 — Pin BC-11's corrected null-guard parity

**As a** developer who no longer believes BC-11 is a JSON-P provider collision,
**I want** the `JsonObjects` framework helper's null-value guard pinned directly,
**so that** the checklist reflects the corrected, evidence-backed risk instead of the refuted
provider-collision hypothesis.

**Satisfies:** FR9 (revised), AC6.

**Scope (from `02-design.md` §B):** new file
`pcfdlrm-event/pcfdlrm-event-processor/src/test/java/.../utils/JsonObjectsNullGuardParityTest.java`
— one test asserting `JsonObjects.createObjectBuilder().add(key, (String) null)` throws
`NullPointerException` on J17, exercising the framework helper directly (not via `MetadataHelper`/
`EnvelopeHelper`/`MaterialEventProcessor`).

**Definition of done:**
- [ ] Test exists, passes on J17, and its comment cites BC-11 and the 2026-08-26 correction (FR2).
- [ ] `docs/j25-parity-checklist.md` row for BC-11 reflects "Refuted / parity," not "high," matching
      the `00-input-brief.md` re-weighting.
- [ ] No production code changed (AC10).

**Outcome (2026-09-07): test written, passed on J17, then removed on review.** Confirmed via `javap -c`
on `utilities-core-17.104.0.jar` that `JsonObjects.createObjectBuilder()` is a one-line delegation to
`getJsonBuilderFactory().createObjectBuilder()` — the `NullPointerException` throws inside glassfish's
`JsonObjectBuilderImpl.validateValue`, with no pcfdlrm class on the call stack. This is the same
objection that removed the earlier LIBRA-branch attempt at this exact test; it reproduces on
`team/25.104.x` too, not just on the LIBRA branch. Reverted BC-11 to 📝 (no test needed) rather than
🟢, consistent with that precedent — see `docs/j25-parity-checklist.md`'s BC-11 row for the full
evidence trail (provider/builder/throw-site) recorded there even without a kept test. **T8 should not
reopen this** — the disposition is settled.

---

## T4 — Close BC-20's rule-count gap; annotate BC-03

**As a** developer relying on the Drools deny-tests for access control,
**I want** a rule-count guard on the command-API knowledge base,
**so that** a J25 zero-rule load can't present as a passing deny test.

**Satisfies:** FR11, AC7.

**Scope (from `02-design.md` §C):**
- New test `CommandApiKnowledgeBaseRuleCountTest` in `pcfdlrm-command-api`, asserting a non-zero rule
  count for kbase `COMMAND_API` (single kbase in this repo's `kmodule.xml` — no multi-kbase trap to
  guard against).
- `ReceiveMigratedCaseRuleTest` — add a comment citing BC-03 (refuted, already coverage-complete via
  its existing allow + deny tests); no new test.

**Pre-work (blocking, not optional):** confirm the real rule-count accessor against the
`access-control-test-utils` dependency's actual API before writing the assertion — do **not** copy
the investigation report's snippet (design note #4). If `StatelessKieSession` doesn't expose the
`KieBase`, use `KieServices.get().getKieClasspathContainer().getKieBase("COMMAND_API")
.getKiePackages()` summed via `getRules().size()`, verified against this repo's actual dependency
version first.

**Definition of done:**
- [ ] Rule-count accessor confirmed against the real dependency (not assumed).
- [ ] `CommandApiKnowledgeBaseRuleCountTest` exists and passes on J17.
- [ ] `ReceiveMigratedCaseRuleTest` annotated with BC-03.
- [ ] No production code changed (AC10).

**Outcome (2026-09-07): rule-count test written, passed on J17, then removed on review.** Verified the
rule-count accessor independently against this repo's actual `kie-api 7.69.0.Final` (found and
recorded a genuine correction to `02-design.md` §C in the process — `StatelessKieSession` *does*
expose `getKieBase()` on this version). Then measured, rather than assumed, whether the new test adds
real detection: a throwaway zero-rule kbase experiment confirmed a 0-rule load already fails
`ReceiveMigratedCaseRuleTest` three independent ways, so the new test added no new detection
capability — the same conclusion the pre-existing checklist row and PR #26's own review already
reached. Reverted to match that precedent (and T3's), rather than keep it on the diagnostic-clarity
argument alone. **BC-03's annotation was also pulled back further than planned**: the source comment
originally added to `ReceiveMigratedCaseRuleTest` was removed too — that file has CR-only line endings
(dating to the original migration commit), so even a 2-line comment addition renders as a whole-file
single-line diff, an unnecessary PR footprint for a citation that belongs in docs. BC-03's citation
now lives only in `docs/j25-parity-checklist.md`'s row — the file itself is untouched. **T8 should not
reopen this** — the disposition is settled, and the `02-design.md` §C correction is recorded in
`docs/j25-parity-checklist.md`'s contradictions section regardless of the test's removal.

---

## T5 — Pin BC-07's `liquibase.properties` key inventory

**As a** developer who doesn't want a Liquibase config failure to surface at deploy time,
**I want** the current key set in `liquibase.properties` asserted in `mvn test`,
**so that** an unsupported key fails fast rather than in a K8s pre-install job.

**Satisfies:** FR12 (BC-07), AC1.

**Scope (from `02-design.md` §D):** new test in `pcfdlrm-viewstore-liquibase` loading
`liquibase.properties` from the classpath and asserting its key set equals exactly
`{changelogFile, liquibase.hub.mode, liquibase.headless}` — today's inventory, including
`liquibase.hub.mode`, the exact property the corrected guide flags as rejected by Liquibase 5 (its
removal is the **upgrade** story's job, not this one — FR16).

**Definition of done:**
- [ ] Test exists and passes on J17.
- [ ] No production code changed (AC10).

**Outcome (2026-09-07): test written, passed on J17, initially kept, then reverted for consistency.**
`LiquibasePropertiesKeyInventoryTest` in `pcfdlrm-viewstore-liquibase` (new `src/test` tree;
`junit-jupiter-api` + `hamcrest` added test-scope, parent-managed versions). The §D key-set-equality
assertion **is** the inverted change-detector the earlier LIBRA-branch attempt was rejected for, and
was labelled as such — but the rejection does not generalise to BC-07: liquibase-core is already a
compile-scope dependency here, so the test also invoked **Liquibase's own** defaults-file validator
(`DefaultsFileValueProvider.validate(new CommandScope("update"))` under `GlobalConfiguration.STRICT`)
over the real packaged file, making the pinned library the oracle, proven to discriminate in both
directions against real artefacts (bogus key → red; the exact code path against the unmodified file on
`liquibase-core-5.0.4` → throws `Strict check failed due to undefined key(s) … - 'liquibase.hub.mode'`,
since 5.0.4 ships zero `liquibase/hub/` classes and drops `HubConfiguration` from
`AutoloadedConfigurations`). Despite that, **reverted**: no new test, no new module dependency
footprint (this module had none before), matching PR #26's own disposition for BC-07 and the
precedent set by T3/T4 on this branch — the same call made for T4's rule-count test, which was also
technically non-redundant/diagnostically valuable but reverted for consistency. **One premise
correction survives the revert, recorded in the checklist's contradictions section (AC11) regardless
of the test's removal:** FR12/§D's "fails in a K8s pre-install job" does not hold — non-strict
validation only logs `WARNING: Potentially ignored key(s)`, on 4.10.0 *and* 5.0.4, and nothing in this
repo enables strict. So BC-07's real cost is stale config nobody is forced to notice, not a deploy-time
crash. **T8 should record BC-07 as 📝** (not 🟢) and carry that correction forward; removing
`liquibase.hub.mode` remains FR16's job. **T8 should not reopen this** — the disposition is settled.

---

## T6 — CUT: BC-13's `WholePayloadMatcher` verdict pin

**Original scope (from `02-design.md` §E):** a new `WholePayloadMatcherParityTest` in
`pcfdlrm-test-support` pinning the matcher's comparison verdict across representative value shapes.

**Why cut:** this was already the lowest-weight item in `00-input-brief.md`'s original Bucket A
table ("thin"), before BC-11 was even corrected down to a similar level. Unlike every other
remaining item, nothing in **production** is exposed here at all — the risk is test-fidelity only
(could this shared matcher silently change verdict for its consumers, including DD-43099's suites),
not a product-500 risk. Cut in favour of keeping T5 (BC-07 — cheap and a confirmed real exposure) and
T4 (BC-20 — one of only two catalogue-critical-rated hotspots) in scope.

**Disposition:** record BC-13 as 🔴 in `docs/j25-parity-checklist.md` (T8) with this reason cited —
not silently dropped from the checklist. FR10's classification (test-fidelity, not product risk)
still applies and should be stated alongside the cut reason.

---

## T7 — CUT: BC-12's `resteasy-multipart-provider` scope recording

**Original scope (from `02-design.md` §F):** a classpath-resolution test in
`pcfdlrm-integration-test`, marked 🟡 (authored, not executed).

**Why cut:** the test would not have run on this branch anyway — only 2 IT classes exist here and no
WildFly 40 image is confirmed available, so it would have landed 🟡 (authored-not-executed) even if
written. Between the two lowest-priority items, this one carries the least immediate value: it
protects against a repackaging risk the original weighting already called "low, IT tier," and — being
IT-tier — it can't be exercised to prove anything on J17 within this story regardless.

**Disposition:** record BC-12 as 🔴 in `docs/j25-parity-checklist.md` (T8) with this reason cited.

---

## T8 — BC-21 codegen type-inventory checks, and the checklist deliverable

**As a** developer whose 8 modules run code generators against JSON schemas,
**I want** each module's generated-type set derived from its own schema resources and asserted, and
the full parity checklist written up,
**so that** a `reflections` scanning-contract change can't silently drop or rename a generated type,
and the upgrade story has one document that tells it exactly what's covered.

**Satisfies:** FR12 (BC-21), FR15, AC1, AC2, AC8, AC9, AC11.

**Scope (from `02-design.md` §G):**
- One small JUnit test per module (8 total: `domain-aggregate`, `domain-event`,
  `domain-event-processor`, `domain-value-schema`, `event`, `event-processor`, `command-handler`,
  `command-api`), each scoped to **that module's own package prefix only** — not the whole
  generated-sources tree, which also contains unrelated vendored/RAML-referenced classes (confirmed
  in `domain-event`'s `target/generated-sources`, e.g. `uk.gov.justice.hearing.courts.*`). Derive the
  expected class set from that module's own `src/main/resources/json/schema/*.json` filenames via
  the generator's naming convention — **confirm the exact convention against the plugin's contract
  before hard-coding it** (design note #3's open item), rather than inferring it from one example.
- `docs/j25-parity-checklist.md` — write the full deliverable: one row per Bucket A **and** Bucket B
  item, `DLRM-01` marked ⬜ not-applicable-here, the exact `mvn` command and result for every 🟢, and
  an explicit gaps section (FR14, FR15) — **including BC-13 and BC-12 as 🔴 with the cut reasons from
  T6 and T7**, per AC1's explicit allowance for a named-reason 🔴 row in place of a test.

**Definition of done:**
- [ ] All 8 codegen tests exist and pass on J17.
- [ ] `docs/j25-parity-checklist.md` exists, covers BC-01…BC-24 with ADR-005's legend, and is
      internally consistent with T1–T5's actual outcomes plus T6/T7's recorded cuts (not
      aspirational).
- [ ] `mvn clean install -DskipITs` passes on `main`/J17 with every new test executing, none skipped
      (AC2).
- [ ] Any J17 run that contradicted the investigation report (BC-11 already is one — T3) is recorded
      with both the report's claim and the observed behaviour (AC11).
- [ ] `git diff main` for the whole PR contains no `src/main` change except generated code or docs,
      and no pom version change (AC10).

**Depends on:** T1–T5 (this is the roll-up; it can't be written accurately until the others are
done). T6/T7 contribute their cut rationale, not test results.

**Outcome (2026-09-07): checklist delivered; BC-21 verified per module with 0 tests written, against
§G's planned 8.** The first DoD box is therefore **not** met as written and deliberately so. BC-21's
"already covered by compile" claim was verified empirically per module rather than trusted, and is
**broadly right but was never measured and undercounts the surface**: `pcfdlrm-domain-event` has 9
own schemas (not 8), of which 8 are imported from main source in `pcfdlrm-domain-aggregate` /
`pcfdlrm-event-processor` and so genuinely fail `mvn install` at compile on a codegen break;
`pcfdlrm-command-handler` is 2-of-3 the same way. Three of §G's eight modules have no subject at all
(`domain-value-schema` and `datatypes-common` run `catalog-generation-plugin` only and generate zero
`.java`; `pcfdlrm-event` is a `packaging=pom` aggregator), and `domain-aggregate` has no schema
resources of its own, so §G's derive-from-own-schemas instrument has no input there. **One genuine,
non-redundant gap was found and still not tested, by choice:** `pcfdlrm-command-api`'s 10 and
`pcfdlrm-event-processor`'s 13 generated `uk.gov.justice.api*` classes (JAX-RS resource, JMS
listeners, remote senders, media-type↔schema-id mappers) are referenced from **no** main or test
source at all, so nothing in the build would notice if the generators emitted none of them. A test
there would be a hard-coded change-detector over a third-party generator's naming contract — the
BC-11/T3 objection plus §G's own standing-maintenance warning — so it was recorded in the checklist's
gaps section with an explicit instruction for the upgrade story to check the built WAR instead,
matching T3/T4/T5's precedent rather than adding novel test infrastructure. Four separate §G premise
failures are recorded under AC11. Also fixed two stale cross-references left by earlier reverts (a
present-tense reference to T5's removed test in the BC-07 contradictions entry, and the BC-13 row
listing BC-11 among items that "touch this repo's own artefacts", which contradicts the BC-11 and
BC-20 rows), added the missing legend marks required by AC8 (Bucket B → 📝, N/A section and DLRM-01 →
⬜), and recorded the AC2/AC10 evidence: `mvn -o clean install -DskipITs` → BUILD SUCCESS, 51 modules,
**557 tests, 0 failures, 0 skipped**, with `git status --porcelain | grep -E "src/main|pom.xml"`
empty.

---

## Sequencing summary

```
T1 (BC-08 carriers) → T2 (BC-08 boundary + FR7 annotation)
T3 (BC-11) — independent, can run any time after T1/T2
T4 (BC-03/BC-20) — independent
T5 (BC-07) — independent
T6 (BC-13) — CUT, see above
T7 (BC-12) — CUT, see above
T8 (BC-21 + checklist) — last, rolls up T1–T5's results and T6/T7's recorded cuts
```

T6 and T7 were the designated first cuts and have already been taken — see their entries above for
the rationale. If further trimming is needed, next in line is narrowing T8's BC-21 scope to the
`domain-event` module only (8 schemas, the module the risk description centres on), recording the
other 7 modules as 📝 (checked, not tested, reason given) rather than dropping BC-21 entirely — BC-08
(T1/T2) is never cut. T8 must always run last regardless of how many earlier tasks are cut or
narrowed, since the checklist has to reflect whatever actually got done.

## Out of scope for every task above

Per `01-requirements.md`: no `jakarta` rename, no pom/version bump, no CI/agent change, no fix for
any live J17 defect a test surfaces (raise it as its own ticket instead — FR13), no execution of
IT-tier items to green.

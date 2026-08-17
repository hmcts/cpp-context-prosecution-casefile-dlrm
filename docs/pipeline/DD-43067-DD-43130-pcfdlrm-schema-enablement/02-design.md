# Design — LIBRA enabler: PCFDLRM schema enablement

> Stage 2 artefact. Source: [`01-requirements.md`](./01-requirements.md).
> Split per the team workflow: **2a** cross-context impact, **2b** inside the service.
> Every claim about current behaviour was read from the working tree at `04c0b2d1`, and every claim
> about core was read from `criminal-court-public-model:17.104.4` — the version `pom.xml:25` pins.
>
> **Settled 2026-08-17 (architect) — there is no `X`.** The `X` in the LIBRA workbook and in
> DD-43081's impact CSV is a **transcription error for `R`** (Remittance). LIBRA's set is
> **`O, C, Q, J, R`**, `R` arrives as `R`, and **no translation layer exists or is needed**. FR12b
> stands, but **its recommendation is now open** — see *Validation dispatch* below: PCF turns out to
> exercise `R` on the fallback rather than special-casing it.

## 2a — Cross-context impact

**No other repo has to change for this story to ship.** Three boundaries are touched; two carry
obligations *out* of this repo that do not block it.

| Boundary | Impact | Action |
|---|---|---|
| **Progression** / `cpp.platform.core.domain` | None. All three onward fields already exist in the `courtReferral.json` closure — `ProsecutionCase.policeOfficerInCase`, `Defendant.numberOfPreviousConvictionsCited`, `Offence.convictionDate`. No core change, no version bump | Regression only |
| **stagingDLRM** (DD-43081) | Two amendments owed to ADR-003 (**G1**, **G2**); the Group B converter mapping stays compile-blocked until this releases | Raise now; neither blocks build here |
| **`cpp-apitests`** | Out of scope, as for DD-43099 | None |

**Deployment order.** PCFDLRM is deployable alone. Every schema change is additive and optional
(FR7), so an XHIBIT payload valid before this story is valid after it, and a LIBRA payload cannot
arrive until DD-43086 opens the gate. The one ordering constraint is internal: **if FR12b resolves to
Option B, its `R` entry must ship with FR12's dispatch**, or a LIBRA `R` case takes the
Crown-flavoured fallback in the window between. Under Option A there is no entry and no constraint.

**What this story hands back to DD-43081** — deliverables, not dependencies:

- **FR11's answer**: map Group A's `vehicleRegistrationMark` to the **flat** `migrated-offence.json`
  home. PCF resolves flat-first with `vehicleRelatedOffence` as fallback and back-fills both
  (`InitiateCCProsecutionApi:191-195`).
- **ADR-003 §5**: LIBRA's code set is `O, C, Q, J, R`. **There is no `X`** — architect-confirmed
  2026-08-17 as a transcription error for `R` — so no translation is needed and none should be built.
  DD-43081's `libra-schema-impact.csv` carries the same error (`C, J, O, Q, X`) and needs the same
  correction.
- **ADR-003 §3**: the officer block nests (G2).

## 2b — Design inside the service

### The shape of the problem

```text
FR1–FR5  five schema edits ──────► generated POJOs ──► converter targets exist
                                          │
FR12 source-system dispatch ──┐           │
FR12b  R's rule set (OPEN)   ─┤           ▼
FR12a  :368 only             ─┴──► CcProsecutionValidationRuleProvider + aggregate
                                          │
FR9/FR9a officer all-or-nothing ──────────┤
FR10 two fields + one rename  ────────────┴──► MigratedCaseToProsecutionCaseConverter
                                                 + …MigratedDefendantToCCDefendantConverter
FR16 five relaxed fields ──► tests only, no production change
FR17 fixture reconciliation ──► fixtures only
```

Three independent workstreams. Only the validation dispatch and the aggregate edits touch shared
code; everything else is additive or test-only.

**FR16 carries no production change** *(corrected 2026-08-14)*. DD-43081 relaxes **five** constraints,
not ten, and its `01-requirements.md` FR1 states that `durationMinutes` and `prosecutorOffenceId`
"are now required on **both** sides in LIBRA 0.13" — the impact CSV marks both `already_flowing`.
Their `02-design.md` schema table still shows `prosecutorOffenceId` being relaxed and is stale on
that point, as it is on "Group B's 20". All five genuinely relaxed fields were audited and are
already tolerated; the deliverable is the confirming tests.

### Schema changes, file by file

`pcfdlrm-domain/pcfdlrm-domain-value-schema/src/main/resources/json/schema/`

| File | Change | Closed? | Failure mode if skipped |
|---|---|---|---|
| `migrated/migrated-case-details.json` | **add** `officerInCase` → `$ref` `pcf-policeOfficerInCase.json` | **yes** | terminal 4xx on every LIBRA submission carrying an officer |
| `pcf-policeOfficerInCase.json` | **add** `policeWorkerReferenceNumber`, `policeWorkerLocationCode` | no | silent drop — and FR9 could never take its send branch |
| `contact-details.json` | **add** `fax` | **yes, and shared** | 4xx; widens every parent reaching it |
| `migrated/migrated-offence.json` | **add** `convictionDate` | no | silent drop |
| `migrated/migrated-defendant.json` | **add** `numPreviousConvictions` (**integer**) | no | silent drop |

**Type note.** `numPreviousConvictions` must be declared `integer` — core's
`Defendant.numberOfPreviousConvictionsCited` is `Integer`, and a string declaration here would make
FR10's rename a parse rather than a copy. `convictionDate` maps to core's `Offence.convictionDate`,
a `String` in the POJO against a `datePattern` `$ref` in schema.

**Three seams, three failure modes** — worth keeping distinct because they need different tests: the
closed container (`migrated-case-details.json`) is a 4xx, the open containers are silent drops, and
the shared closed schema (`contact-details.json`) is a widening that must be reviewed as a
shared-schema change rather than an officer-local one.

**No runtime entry-schema edit** (FR8): `pcfdlrm.command.receive-migrated-case-file.json` and
`pcfdlrm.receive-migrated-case-file.json` both `$ref` `migrated-case-details.json` rather than
restating it, so all five changes propagate to the inbound gate automatically. Re-confirmed at
`04c0b2d1` — DD-43081's F6 found the sibling repo's equivalent file *does* diverge, so this is
checked rather than assumed.

### Validation dispatch

Per [ADR-002](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/002-source-system-keyed-dispatch.md),
which names `CcProsecutionValidationRuleProvider` as the third instance of this problem: **a map
keyed by source system, not a conditional.**

The call sites make this cheap. Each is unique, and each already has what it needs in scope:

| Factory | Callers | Change |
|---|---|---|
| `getCaseValidationRules` | 1 — `MigratedCaseFileAggregate:219` | +1 param; `sourceSystemName` already in scope at `:161` |
| `getDefendantValidationRules` | 1 — `ProsecutionCaseFileHelper:97` | **none** — signature and body unchanged |
| `getMigratedHearingValidationRules` | 1 — `MigratedCaseFileAggregate:393` | none |

```java
// new — PCF-shaped: COMMON minus the three rules PCF does not have
private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>>
        COMMON_CASE_RULE_SET_LIBRA = List.of(
                new CaseInitiationValidationRule(),
                new ProsecutorReferenceDataValidationRule(),
                new CaseMarkersValidationAndEnricherRule(),
                new PoliceForceCodeValidationRule());

// new — PCF-shaped: SJP minus the two court rules
private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>>
        SJP_CASE_RULE_SET_LIBRA = List.of(
                new CaseInitiationValidationRule(),
                new SummonsCodeValidationRule(),
                new ProsecutorReferenceDataValidationRule(),
                new ProsecutorSJPValidationRule(),
                new ProsecutorAOCPValidationRule());

private static final Map<String, List<…>> caseValidationMapLibra = of(
        CHARGE.getCode(),      COMMON_CASE_RULE_SET_LIBRA,
        REQUISITION.getCode(), COMMON_CASE_RULE_SET_LIBRA,
        SJP.getCode(),         SJP_CASE_RULE_SET_LIBRA);

private static final Map<String, Map<String, List<…>>> caseRulesBySourceSystem = Map.of(
        XHIBIT, caseValidationMap,          // untouched — NFR1 holds structurally
        LIBRA,  caseValidationMapLibra);
```

**Existing constants and `caseValidationMap` are not edited.** XHIBIT resolves to exactly today's
list, so NFR1 is satisfied by construction rather than by testing.

**The map-miss fallback is a deliberate choice, not ADR-002's "programming error".** ADR-002 §6
assumes a validated enum key; PCFDLRM's `migrationSourceSystemName` is a plain `string` on
`migrated-migrationSourceSystem.json` and **is not in that schema's `required` list**, so an absent
or unrecognised value is a real runtime input here. An unknown source system resolves to the
**LIBRA/PCF-shaped** set — the neutral default — so a future source system inherits PCF behaviour
rather than Crown Court rules by accident.

**Defendant level: `O`, `C`, `Q` and `J` need nothing.** They are already keys in
`defendantValidationMapDlrm = defendantValidationMapSpi` (`:291`), PCF's magistrates-court map, so
LIBRA inherits the right defendant rules for four of its five codes with no edit. **Only `R` is in
question (FR12b), and that decision is open.**

**Option A — leave `R` on the fallback. PCF parity. Recommended.** PCF has no `R` in its validation
`CaseType`, no `R` key in any of its five defendant maps, and no `REMITTED` anywhere in Java, JSON or
RAML. It lets `R` take the same `DEFAULT_DEFENDANT_RULE_SET` fall-through PCFDLRM has today — and
**exercises it**: seven PCF integration fixtures carry `initiationCode: "R"`, including the primary
happy path, which has **no `feeStatus`** yet supplies **both `arrestDate` and `chargeDate`**. PCF's
position is that a remitted case carries its dates like any other. Unlike the officer block (F5),
**PCF is not silent here**, so the repo's "follow PCF" tie-breaker does reach this decision. Zero
code.

**Option B — map `R` alongside `O`** (the original FR12b recommendation). If chosen, two
implementation notes the shorthand hides:

- `defendantValidationMapDlrm` is a reference **alias** of `defendantValidationMapSpi`, not a copy,
  and both are `com.google.common.collect.ImmutableMap`s. The entry cannot be *added* — the map must
  be redeclared, and redeclaring `…Spi` would silently change the **SPI channel** too. Option B means
  declaring a genuinely separate `defendantValidationMapDlrm`, following the existing `…MCC` /
  `…ForGroupCivilCases` precedent.
- Every other value in that map is a **composed triple**, so `R` takes
  `COMMON + SPI + OTHER_DEFENDANT_RULE_SET` — the same value as `O` — not the bare
  `OTHER_DEFENDANT_RULE_SET`, which would strip `R` of almost all defendant validation.

**What decides it:** whether remitted LIBRA cases reliably carry `arrestDate`/`chargeDate`. The
impact CSV marks both `already_flowing` and "Identical on both sides", but `not_validated` at the
gate — so the contract permits absence and the schema cannot answer it. **Low urgency:** `:282` and
`:562` still discard defendant problems for LIBRA under FR12a, so either choice is invisible until
the deferred guards story lands.

**`getDlrmDefendantValidationRules` is deleted** (FR14) in the same change, before the dispatch is
written rather than after — it is private, returns `getOrDefault(code, null)`, carries a `// What
will fo here` comment and is called from nowhere. Leaving a second DLRM rule selector beside a new
one is how the wrong one gets wired up later.

### Aggregate changes

Three edits in `MigratedCaseFileAggregate`, two of them one line:

1. **`:219`** — pass `sourceSystemName` to `getCaseValidationRules`.
2. **`:368`** — the no-materials branch currently emits `MigratedCaseFileReceived` only
   `if (isXhibit(...))`. Relax it so every source system emits. This is the only guard in scope
   (FR12a); the other seven stay.
3. **`:525-526`** — `isXhibit` calls `.getMigrationSourceSystemName().equals(XHIBIT)` unguarded on a
   field that is not `required`, so `migrationSourceSystem: {}` NPEs. Reverse the comparison while
   the method is being touched.

**The seven guards left in place are an accepted, stated limitation**, not an oversight: a LIBRA case
with case-, hearing- or offence-level problems proceeds to Progression carrying them and raises no
`MigratedCaseValidatedWithWarnings`. The follow-up story works through them.

### Converter changes

`MigratedCaseToProsecutionCaseConverter` — the outbound seam at
`MigratedCaseReceivedProcessor:51-57`. The reachability root is **`courtReferral.json`**, not
`apiProsecutionCase.json`; core holds two parallel families whose closures are disjoint.

1. **`.withPoliceOfficerInCase(...)`** on the `prosecutionCase` builder (`:88-106`), built by a new
   private `buildPoliceOfficerInCase(...)` implementing FR9/FR9a:

   ```text
   send iff surname ∧ policeOfficerRank ∧ policeWorkerReferenceNumber ∧ policeWorkerLocationCode
        personDetails.lastName  ← officer surname
        personDetails.gender    ← NOT_KNOWN            (FR9a — the only synthesised value)
        personDetails.firstName ← forename
        personDetails.address   ← only if address1 present   (core address.json requires address1)
        personDetails.contact   ← emails / phones / fax      (contactNumber.json requires nothing)
   otherwise omit the block entirely — never partial
   ```

2. **`ProsecutionCaseFileMigratedDefendantToCCDefendantConverter`** — add
   `.withNumberOfPreviousConvictionsCited(...)` from `numPreviousConvictions` (FR10's rename, applied
   at this seam and nowhere else).

3. **`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter`** (reached from the defendant
   converter at `:73`) — add `.withConvictionDate(...)`.

Neither converter mentions officers, previous convictions or vehicles today, so all three are pure
additions.

**Cost depends on G2.** With canonical nesting (recommended), `buildPoliceOfficerInCase` is a
level-preserving rename — PCFDLRM's `personalInformation{names, contactDetails, address}` maps
straight onto core's `personDetails{…}`. If canonical stays flat, it becomes a re-nest, which is the
first in this repo's history and the reason FR2a recommends the other way.

### The unmatched-offence rule

FR16's single rule. Today a null `prosecutorOffenceId` never matches at `ProsecutionCaseFileHelper:177`,
so `:181` returns empty and `:164` collapses the list to `List.of()`, which silently empties
`listDefendantRequests` and drops **every hearing for that case** at
`…InitialHearingToCCHearingRequestConverter:255`.

The new rule raises a proper problem code when a listed offence cannot be matched. **It makes the
condition visible; it does not by itself prevent the loss** — `:554` stays `isXhibit`-gated per
FR12a, so the problem is still discarded for LIBRA. **Design decision required at build time:** if
the hearing loss must actually be *prevented* in this story, `:554` has to come in alongside `:368`.
Recorded here rather than discovered in test.

### Test layout

Extending DD-43099 (FR18), through the delivered `pcfdlrm-test-support` (`FixtureLoader`,
`WholePayloadMatcher`) per
[ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md).

Source system is already a **scenario parameter**: `ObjectBuilder` takes a required `SourceSystem`
record with no XHIBIT default and no defaulting overload, so adding LIBRA is a data change at the
call site. **No LIBRA-specific test class, and no `if` on source system inside a test.**

- **Unit/component — exhaustive.** Each of the **five** codes asserted against its chosen rule set
  (AC6), with `J`'s SJP routing asserted explicitly so it cannot drift back onto the common path
  (FR13) and **`R`'s rule set asserted explicitly** whichever FR12b option is chosen — the assertion
  is the deliverable either way, so the choice cannot change unnoticed. **No `X` fixture anywhere**
  (AC6a): `X` is not a real code, and one would encode the workbook's error into the suite. The
  officer block accepted whole and refused partial (AC4); `gender: NOT_KNOWN` (AC4a); each relaxed
  field accepted when absent (AC9); the no-materials path (AC9a).
- **Integration — one representative LIBRA journey**, at DD-43099's depth.
- **A whole-payload assertion at the `receive-migrated-case-file` boundary** is required, not a
  field-presence spot check — it is the only assertion that catches an ADR-003 name mismatch, which
  is otherwise invisible to both repos' unit suites.
- **XHIBIT fixtures must not be edited** (NFR1). If one moves, source-system scoping has leaked.

## FR → design traceability

| FR | Where |
|---|---|
| FR1–FR5, FR7, FR8 | *Schema changes, file by file* |
| FR2a | *Converter changes* cost note · **G2** |
| FR9, FR9a, FR10, FR11 | *Converter changes* · 2a hand-back |
| FR12, FR14 | *Validation dispatch* |
| FR12b | *Validation dispatch* — Option A vs B, **decision open** |
| AC6a, AC6b | *Test layout* — no `X` fixture · 2a hand-back (impact CSV + ADR-003 §5 correction) |
| FR12a | *Aggregate changes* |
| FR13, FR18, NFR1 | *Test layout* |
| FR15 | No design impact — `initiationCode` stays a plain `string` |
| FR16 | *The unmatched-offence rule* |
| FR17 | Fixture reconciliation — no design impact, but see **F3** |
| FR19 | Cross-repo obligation — no design impact |

## Findings

**F1 — `feeStatus` is never sent by LIBRA, so both date rules' absence half always fires.** Zero
occurrences in `dlrm-libra-0.13.json` and in canonical. `ArrestDateValidationRule` and
`ChargeDateValidationRule` each gate their absence check on `isNull(caseDetails.getFeeStatus())`, so
for LIBRA every offence lacking the date raises a problem. This is what makes FR12b's choice matter,
and it applies to the agreed `C` routing too — a LIBRA charge case will demand an arrest date on
every offence. Expected for a charge, but now explicit.
**Caveat added 2026-08-17:** the `feeStatus` argument does **not** on its own settle FR12b, because
PCF's own exercised `R` fixture also has no `feeStatus` and satisfies the rules by supplying the
dates. It shows the check is *live* for LIBRA, not that it is *wrong* for `R`.

**F2 — the problem code for a *missing* arrest date is `ARREST_DATE_IN_FUTURE`.** A pre-existing
misnomer that will make LIBRA problem output confusing to read. Renaming it is out of scope; knowing
it before triaging the first LIBRA batch is not.

**F3 — `numPreviousConvictions` appears in 24 fixtures while being declared in no schema in either
repo**, and canonical's `migrated-defendant.json` is `additionalProperties: false`. Those fixtures
therefore cannot be used as evidence of what the contract allows. When FR5 declares the field for
real, reconcile them against the schema rather than assuming they were already correct — and expect
the same pattern for the other four fields. This repo's RAML examples are not build-validated and
have drifted the same way.

**F4 — material validation is already inert for LIBRA, and hardcoded to Crown Court.**
`MigratedCaseFileAggregate:181` passes `CaseType.CC` to
`MaterialFileTypwWithCountValidationRuleProvider.getRejectionRules`, which returns only
`ExhibitFiileTypeValidationRule` — and that rule self-guards at `:32` (`if (!isXhibitSystem(input))
return VALID`). So no material work is needed for LIBRA. But `:348` also hardcodes `CC`, which means
`MaterialValidationRuleProvider:18`'s `SJP → SjpMaterialsValidationRuleProvider` arm is **unreachable
from the migration flow entirely**, including for LIBRA `J` cases. Out of scope; recorded because it
is the material-layer version of the `J`-is-SJP question.

**F6 — police-rank reference data exists in this repo and is consumed by nothing.**
`ReferenceDataQueryServiceImpl:76` declares `referencedata.query.police-ranks`, `RefDataHelper:73`
has `asPoliceRankRefData()`, `PoliceRankReferenceData` is generated, and
`referencedata.get-police-ranks.json` is already stubbed for the ITs — but nothing outside
`pcfdlrm-refdata` references any of it. It is a third orphan alongside
`pcf-policeOfficerInCase.json` and `getDlrmDefendantValidationRules`.
**Decision for T4: do not wire it.** PCF does not validate the officer at all, so `policeOfficerRank`
is carried through as the string LIBRA sends. Recorded because the plumbing is sitting there and the
next person to touch the officer block will find it and reasonably assume it was meant to be used.
Note the stub holds exactly one rank (`AXE`), so anyone who *did* wire it would need the LIBRA
fixture's rank to match — a trap worth knowing about rather than discovering in a red IT.

**F5 — PCF declares the officer block and never reads it.** `case-details.json:45` declares
`otherPartyOfficerInCase`; it flows into PCF's domain and public events and has **zero** references
in PCF main Java. `CCCaseToProsecutionCaseConverter` builds field-by-field with no whole-object copy.
This is why "follow PCF" does not reach FR9 — PCF is silent, not opposed — and why FR9 was decided
on the merits instead.

## Gates

| # | Question | Recommendation | Blocks |
|---|---|---|---|
| **G1** *(revised 2026-08-17)* | **Canonical's `initiationCode` is `enum: ["O"]` and DD-43081 FR1 has decided not to widen it** — reading LIBRA 0.13's workbook snapshot as `["O"]` too, and dropping "the whole initiation-code thread". LIBRA will in fact send new codes; `["O"]` is correct only because **XHIBIT** sends only `O` | Reinstate the thread: **widen** canonical to LIBRA's `O, C, Q, J, R`; **keep** the XHIBIT allowed-values rule at `["O"]`, now a real constraint; **add** a LIBRA rule at the five — the exact shape ADR-002 §4 anticipates. Amend ADR-003 §5 (`C, J, Q, S` → `O, C, Q, J, R`) and **correct the `X`** in §5 and in `libra-schema-impact.csv` — architect-confirmed as a transcription error for `R`, so no translation is needed and none should be built | Nothing here — PCFDLRM's `initiationCode` is a plain `string`. **Blocks any LIBRA case that is not `O`**, and so the LIBRA journey (AC15) |
| **G2** | ADR-003 §3 declares the officer block flat; PCFDLRM and core both nest | Canonical nests (FR2a option 2). `officer-in-case.json` had **not landed** in stagingDLRM at `55bf1721`, so this is a schema edit now and a three-party change later | Nothing here; sets FR9's converter cost |
| ~~G3~~ | ~~Must the `prosecutorOffenceId` hearing loss be *prevented* or only *detected*?~~ **Withdrawn 2026-08-14** | The field is **not relaxed** — DD-43081 `01-requirements.md` FR1 states LIBRA 0.13 requires it on both sides, and the impact CSV marks it `already_flowing`. The condition is unreachable from LIBRA | — |

Neither G1 nor G2 blocks work in this repo. Both get more expensive the longer they wait — G2
sharply, once the LIBRA extract is written against a flat officer block.

## Tasks

Five, split by **module ownership** rather than by requirement group — three of the five would
otherwise collide in `pcfdlrm-domain-aggregate`.

| Task | Owns | Covers | Depends on |
|---|---|---|---|
| **T1** Schemas | `pcfdlrm-domain-value-schema` | FR1–FR5, FR7, FR8, FR19 | — (gates T4) |
| **T2** Validation dispatch | `pcfdlrm-domain-aggregate` | FR12, FR12b, FR13, FR14 | — |
| **T3** Aggregate guards | `pcfdlrm-domain-aggregate` | FR12a | T2 (same module) |
| **T4** Converters | `pcfdlrm-event-processor` | FR9, FR9a, FR10 | T1 (generated POJOs) |
| **T5** Tolerance tests + fixture reconciliation | `pcfdlrm-domain-aggregate`, fixtures | FR16, FR17 | T3 (same module) |
| **T6** Integration — the LIBRA journey | `pcfdlrm-integration-test` | FR18 (integration half) | T1 (fixture), T1–T4 (to pass) |

```text
T1 ──────────────► T4 ──────┐
T2 ──► T3 ──► T5 ───────────┴──► T6
```

**T1 first.** It unblocks DD-43081's compile-blocked Group B converter mapping (FR19) and produces
the generated POJOs T4 depends on. Check what the POJO-generation plugin emits once
`pcf-policeOfficerInCase.json` is reachable before estimating T4 — FR2a's outcome changes that cost.

**T2, T3, T5 share `pcfdlrm-domain-aggregate` and must sequence.** They are not parallelisable; T4
runs alongside the whole chain because it owns a different module.

**Unit and component tests ship with their task**, on the DD-43099 DSL — each task carries the
scenarios for the behaviour it introduces. NFR1's XHIBIT regression run is a definition-of-done item
on every task, never a phase.

**The integration journey is its own task (T6), because no single task can make it pass.** FR18's
"one representative LIBRA journey" crosses all four preceding tasks: the schema declares the fields
(T1), the dispatch selects the rule set (T2), `:368` lets a no-materials case emit (T3), and the
converters build the payload (T4). `ReceiveMigratedCaseFileIT` carries 11 `@Test` methods plus an
eight-fixture parameterised source, so a LIBRA journey is a new fixture and new assertions, not a
line in someone else's task. This mirrors DD-43099, which also gave ITs their own task.
**T6 is not AC15** — AC15 is the cross-repo journey from a real LIBRA `case.json` through
stagingDLRM, and needs DD-43086 and a sample that does not yet exist.

**FR11 is already delivered** — the answer is recorded in `01-requirements.md` and communicated via
[`docs/analysis/dd-43130-to-dd-43081-handover.md`](../../analysis/dd-43130-to-dd-43081-handover.md).
It needs no task.

## Notes for stage 3
- **FR14's deletion belongs in the dispatch task, not a tidy-up task.** Doing it after means
  designing the routing twice.
- **The `:368` fix has no XHIBIT-visible behaviour change**, so it needs a LIBRA no-materials fixture
  to be provable at all. `stagingdlrm-testharness` carries a `fixeddatenomaterial` case as precedent.
- Open for build to settle: whether the officer converter lives in
  `MigratedCaseToProsecutionCaseConverter` or its own class — it is ~40 lines with an all-or-nothing
  guard, and that converter is already 200+ lines.

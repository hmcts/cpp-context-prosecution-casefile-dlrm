# Input brief — LIBRA enabler: PCFDLRM schema enablement

> Stage 0 artefact. Feeds [`01-requirements.md`](./01-requirements.md).
> **Self-contained** — everything an SDLC stage needs to run against this story is in this
> directory or reachable by the links below.
> Every claim in *Current state* was read from this repo's working tree at `04c0b2d1`, not quoted
> from the upstream analysis.

| | |
|---|---|
| Epic | [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler |
| Story | [DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) — PCFDLRM schema enablement |
| Repo | `cpp-context-prosecution-casefile-dlrm` — **this repo only** |
| Sibling story | [DD-43081](https://github.com/hmcts/cpp-context-stagingdlrm/tree/main/docs/pipeline/DD-43067-DD-43081-schema-enablement) — the stagingDLRM half |
| Payload contract | [ADR-003 — LIBRA payload contract](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/003-libra-payload-contract.md) (field names and nesting) |
| Test convention | [ADR-001 — DLRM scenario test DSL](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md) |
| Extends | [DD-43099](../DD-43067-DD-43099-pcfdlrm-test-hardening/) — pcfdlrm test hardening, **delivered** (`pcfdlrm-test-support`, `FixtureLoader`, `WholePayloadMatcher`) |

## The epic this story belongs to

**DD-43067 — LIBRA enabler.** Ingest magistrates' court case files from legacy system LIBRA through
the existing DLRM pipeline (Azure Blob → Function App → stagingDLRM → **PCFDLRM** → Progression),
reusing the XHIBIT path rather than forking it.

**Design decision already taken for the epic:** XHIBIT and LIBRA share **one** stagingDLRM endpoint
and **one** schema family. Source-system-specific behaviour is pluggable strategies inside the
shared path — not duplicated schemas, endpoints, or command/event types. The rejected
separate-schema alternative is in
[`libra-ingestion-analysis.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-ingestion-analysis.md)
§7.

PCFDLRM already works this way: one shared ingestion path, a map-based rule-set provider
(`CcProsecutionValidationRuleProvider`) and a small number of narrowly-scoped `XHIBIT`-guarded
branches.

**The accepted cost of the shared design is coupled blast radius** — a change made for LIBRA touches
code XHIBIT depends on in production. The agreed mitigation is the DD-43099 suite, which treats
source system as a scenario variable.

## Why this story exists — the boundary DD-43081 stopped at

DD-43081 relaxes and extends the **canonical** schema in `cpp-context-stagingdlrm`. Its field
additions split three ways, and only one of those groups is finished when DD-43081 merges:

| Group | Size | State after DD-43081 | This story |
|---|---|---|---|
| **A** | 12 | Declared in canonical **and mapped** by `MigratedCaseConvertor` | **Nothing to do** — PCFDLRM already has all 12 |
| **B** | 20 | Declared in canonical, **converter mapping compile-blocked** — the builder methods do not exist at `pcfdlrm.version 17.104.21` | **This story's payload.** Ship the fields so the mapping becomes writable |
| **C** | 3 | Declared in canonical, deliberately never propagated | Out of scope, by decision |

Group B is *write-only upstream* until PCFDLRM releases the receiving end. That is what DD-43130 is
for.

DD-43081 also relaxes 10 constraints in the shared schema. Every relaxation makes a field PCFDLRM
receives today **legitimately absent** for the first time.

## Scope boundaries

| In scope | Out of scope |
|---|---|
| `pcfdlrm-domain-value-schema` — receiving schemas for Group B | The canonical schema, the source-system rule engine, the stagingDLRM rejection path, `MigratedCaseConvertor` — **DD-43081** |
| `pcfdlrm-event-processor` — the two converters that build the Progression payload | `pcfdlrm.version` bump + Group B converter mapping + impact-CSV refresh in the sibling repo — **a separate third story** (see below) |
| `pcfdlrm-domain-aggregate` — LIBRA rule-set routing and relaxation tolerance | Group C (`dxAddress`, `forename3`, `uniquePropertyReferenceNumber`) — by decision |
| This repo's IT fixtures and RAML examples | `cpp-apitests`, as for DD-43099 |
| DD-43099 suite extension with LIBRA scenarios | PCFDLRM-side rejection visibility in reconciliation — flagged, not built |

## This story's request

Five change sets.

### Change set 1 — Receive Group B

Three schema declarations are genuinely missing; the other 14 Group B fields already have homes.

**The officer block — 17 fields, 14 already homed.** Group B's `officerInCase` container is 11
officer fields plus a 6-field address. Mapped onto this repo's existing schemas:

| stagingDLRM field | PCFDLRM home | State |
|---|---|---|
| `forename`, `forename2`, `surname` | `personal-information.json` → `firstName`, `givenName2`, `lastName` | exists (`lastName` is `required`) |
| `primaryEmail`, `secondaryEmail`, `workTelephoneNumber`, `mobileTelephoneNumber` | `contact-details.json` → `primaryEmail`, `secondaryEmail`, `work`, `mobile` | exists |
| `address1`–`address5`, `postcode` | `pcf-address.json` | exists (`address1` is `required`) |
| `policeOfficerRank` | `pcf-policeOfficerInCase.json` | exists |
| **`policeWorkerReferenceNumber`** | `pcf-policeOfficerInCase.json` | **absent** |
| **`policeWorkerLocationCode`** | `pcf-policeOfficerInCase.json` | **absent** |
| **`faxNumber`** | `contact-details.json` → `fax` | **absent** |

So the officer work is **structural, not volumetric**: `pcf-policeOfficerInCase.json` already exists
but is **referenced by nothing** — the only occurrence of `policeOfficerInCase` anywhere in the repo
is the `id` line inside the file itself. Wiring it in is the change; adding two properties to it is
the trivial part.

**The blocker that makes this non-optional:** `migrated/migrated-case-details.json` is
`additionalProperties: false` with exactly four properties (`caseDetails`, `hearings`, `defendants`,
`migrationSourceSystem`). `officerInCase` is a **new top-level container on the migrated case**. The
moment stagingDLRM starts sending it, an undeclared `officerInCase` is a **terminal 4xx on every
LIBRA submission carrying an officer** — not a silent drop.

**`fax` lands on a shared, closed schema.** `contact-details.json` is `additionalProperties: false`
and is `$ref`'d from `personal-information.json`, so adding `fax` widens every parent that reaches
it. Additive and safe, but it is not an officer-local change and should be reviewed as such.

**The three non-officer Group B fields.**

| Field | Container | State |
|---|---|---|
| `convictionDate` | `migrated/migrated-offence.json` | **absent** — add |
| `numPreviousConvictions` | `migrated/migrated-defendant.json` | **absent from every schema, present in 24 IT fixtures** — see change set 5 |
| `organisationTelephoneNumber` | `migrated/migrated-defendant.json` | **possibly already present as `telephoneNumberBusiness`** — decide before adding |

Both these containers are **open** (no `additionalProperties: false`), so today's failure mode there
is silent data loss rather than rejection. Declaring the fields is still required — the generated
POJOs are what the converters read, and an undeclared field never reaches one.

**The runtime entry schema needs no edit.**
`pcfdlrm-command-handler/src/raml/json/schema/pcfdlrm.command.receive-migrated-case-file.json`
`$ref`s `migrated-case-details.json` rather than restating it, so every change above propagates to
the inbound gate automatically. Same for `pcfdlrm-command-api`'s
`pcfdlrm.receive-migrated-case-file.json`. Verified, because the sibling repo's equivalent
(DD-43081 finding F6) is a second file that *does* diverge — the assumption is not free.

### Change set 2 — Carry Group B onward to Progression

The outbound seam is **not** the public event. `MigratedCaseReceivedProcessor:51-57` converts
`MigratedCaseFileReceived` to `InitiateCourtProceedings` and sends
`progression.initiate-court-proceedings`. So the Progression-side reachability root is
**`courtReferral.json`**, not `apiProsecutionCase.json` — core's `criminal-court-public-model` holds
two parallel families whose closures are disjoint, and measuring against the wrong one has already
produced one wrong ruling (impact §8).

Two classes do the work:

- `MigratedCaseToProsecutionCaseConverter` (205 lines) — builds `CourtReferral`
- `ProsecutionCaseFileMigratedDefendantToCCDefendantConverter` (218 lines) — builds the defendant,
  including a `buildContact` that already constructs core's `ContactNumber`

Neither mentions officers, vehicles or previous convictions today.

**The all-or-nothing officer constraint.** Per impact §5, core's `policeOfficerInCase.json` marks
`personDetails`, `policeOfficerRank`, `policeWorkerReferenceNumber` and `policeWorkerLocationCode`
as `required` **when the block is present**, plus officer `lastName` and `address1`. A partial
officer block is therefore rejected at Progression. Either populate all six or omit the block
entirely — **a partially-populated block must never be sent.** This claim is from the upstream
analysis and must be re-verified against the core jar at design stage before it is built on.

Note this repo's own `personal-information.json` already requires `lastName` and `pcf-address.json`
requires `address1`, so PCFDLRM's inbound gate enforces two of the six itself.

**`vehicleRegistrationMark` has two homes here, and DD-43081 is waiting on the answer.** It is
declared both flat on `migrated/migrated-offence.json:111` (as a core `$ref`) and on
`vehicle-related-offence.json:10` (as a plain string). DD-43081 T4 AC8 says *"confirm with the
PCFDLRM team before mapping"* — **this story is that team.** Determine which one reaches
`offenceFacts.vehicleRegistration` and answer it, even though the field itself is Group A and needs
no schema change.

### Change set 3 — LIBRA rule-set routing

**The silent-degradation path is the real risk, not a crash.** Rule sets are keyed on the raw
`initiationCode` string (`ProsecutionCaseFileHelper:97`), and an unmapped code **does not fail** —
`getValidationRules` falls back to `COMMON + SPI/NON_POLICE + DEFAULT_DEFENDANT_RULE_SET`, and
`getCaseValidationRules` to `getOrDefault(code, COMMON_CASE_RULE_SET)`. So an un-agreed LIBRA
`initiationCode` produces **quietly weaker validation on real migrated cases**, with no error
anywhere. That is worse than a failure, and it is why this story asks for an explicit decision *and*
an explicit assertion rather than "make it work".

Four facts that shape the design:

1. **The DLRM channel aliases the SPI rule sets** —
   `defendantValidationMapDlrm = defendantValidationMapSpi` (`CcProsecutionValidationRuleProvider:291`).
   Whatever is chosen for LIBRA is chosen *on top of SPI's rules*, which the map names do not suggest.
2. **A defendant-level `initiationCode` overrides the case-level one when valid**
   (`ProsecutionCaseFileHelper:93-94`), so LIBRA's value has to be considered at both levels.
3. **There is a half-built DLRM rule selector.** `getDlrmDefendantValidationRules`
   (`CcProsecutionValidationRuleProvider:332`) returns `getOrDefault(code, null)`, carries the
   comment `// What will fo here`, and is **called from nowhere**. Complete it or delete it —
   leaving a private, null-returning DLRM rule selector beside new LIBRA routing is how the wrong
   one gets wired up later.
4. **Two XHIBIT-guarded branches exist in main code** — `ProsecutionCaseFileHelper:118`
   (`applyRuleToDefendantFields` on a validation failure) and `MigratedCaseFileAggregate:526`
   (material file-type checks, `INVALID_FILE_TYPE_FOR_XHIBIT` / `_MIGRATION`). Each needs a
   deliberate LIBRA answer: equivalent, different, or none.

**`initiationCode` needs no schema change here.** DD-43081 keeps it a **typed enum**, widened to the
platform's seven codes (`Q,R,S,C,J,Z,O`) — and PCFDLRM's is already a plain `string` in both
`case-details.json:20` and `migrated/migrated-defendant.json:89`. The schema impact is nil; the
routing impact is everything.

### Change set 4 — Tolerate what stagingDLRM stops enforcing

DD-43081 relaxes 10 constraints. Seven of them make a field PCFDLRM receives today legitimately
absent: `caseDetails.dateReceived`, `receiptType`, `receivingCourt`, `retrialIndicator`, the
`dateOfCommittal`/`dateOfSending` pair, `hearings[*].durationMinutes` and
`offences[*].prosecutorOffenceId`.

`durationMinutes` is the sharpest: it is `required` today, so **every** existing XHIBIT fixture
carries it and no test has ever exercised its absence. Absence is now reachable.

Where PCFDLRM genuinely needs one of these, it belongs in a PCFDLRM validation rule producing a
proper problem code — **not an NPE, and not a silent default.**

### Change set 5 — Reconcile the fixtures and examples

`numPreviousConvictions` appears in **24 of this repo's JSON fixtures** (e.g.
`pcfdlrm.command.receive-migrated-case-file.json:50` → `"numPreviousConvictions": 3`) and in
stagingDLRM's `defendant-example.json` — while being declared in **no schema in either repo**, and
while canonical's `migrated-defendant.json` is `additionalProperties: false`.

Those fixtures encode a field the contract does not have. The consequence is not cosmetic: **the
fixtures cannot be used as evidence of what the contract allows.** When change set 1 declares the
field for real, reconcile them against the schema rather than assuming they were already right, and
expect the same pattern for the other five fields.

This repo's RAML example payloads are not validated at build time and have drifted the same way.

### Testing

**Extend the DD-43099 suites and the ADR-001 DSL — do not fork them, and do not touch DD-43078's**
(those are the sibling repo's). DD-43099 landed `pcfdlrm-test-support` with `FixtureLoader` and
`WholePayloadMatcher`; LIBRA is added as **scenario data**, with source system as a scenario
parameter — no LIBRA-specific test class and no `if` on source system inside a test.

**A whole-payload assertion at the `receive-migrated-case-file` boundary is required**, not a
field-presence spot check. It is the only assertion that catches an ADR-003 name mismatch, which is
otherwise invisible to both repos' unit suites.

## Decisions taken with the requester

| Question | Decision |
|---|---|
| How much of the officer block? | **Group B only — the full 17-field block**, declared and carried onward. Group C's `dxAddress`, `forename3` and `uniquePropertyReferenceNumber` are excluded, matching stagingDLRM's accepted-but-never-propagated stance |
| Wire the orphaned officer schema, or author a new one? | **Wire `pcf-policeOfficerInCase.json`.** It is already modelled; the gap is that nothing references it |
| Field names and nesting | **PCFDLRM's, not the workbook's** — [ADR-003](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/003-libra-payload-contract.md) |
| Who closes the cross-repo loop? | **A separate third story in `cpp-context-stagingdlrm`**, blocked on this one — see below |
| PCFDLRM-side rejection visibility? | **Out of scope, flagged** (R3) |
| Test approach | Extend DD-43099 + ADR-001 DSL |
| `cpp-apitests` | Out of scope, as for DD-43099 |

## The cross-repo coupling — and why it is a third story

Two things in `cpp-context-stagingdlrm` only become possible once this story releases:

1. **The Group B converter mapping**, compile-blocked at `pcfdlrm.version 17.104.21` (pinned in
   their parent POM) because the builder methods do not exist yet.
2. **The impact-CSV claims.** `tools/schema-gen/build-schema-impact.py` curates all six of this
   story's fields as `pcfdlrm_status: no_field` and **re-verifies every curated claim against this
   repo's checkout on each run**, hard-failing on a stale one:
   `sys.exit("error: curated downstream claims no longer hold: …")` — `build-schema-impact.py:1120`.

Per the one-repo-per-story rule in `CLAUDE.md`, all of that is **a separate stagingDLRM story**:
bump `pcfdlrm.version`, write the Group B mapping, refresh `MAPPING` and regenerate
`libra-schema-impact.csv`.

**The sequencing consequence must not be discovered as a red build.** The CSV claims go stale the
moment DD-43130 merges, whether or not the third story has started. So **raising that ticket is a
deliverable of this story**, and it should be raised at the start, not at the end.

## Deliverables

1. `officerInCase` wired into `migrated-case-details.json`, with `policeWorkerReferenceNumber`,
   `policeWorkerLocationCode` on the officer schema and `fax` on `contact-details.json`.
2. `convictionDate` and `numPreviousConvictions` declared; `organisationTelephoneNumber` resolved
   against the existing `telephoneNumberBusiness`.
3. Officer block and the three non-officer fields carried into the `CourtReferral` payload,
   all-or-nothing on the officer block.
4. LIBRA rule-set routing decided, implemented and **asserted**; `getDlrmDefendantValidationRules`
   completed or deleted.
5. Relaxation tolerance for the seven newly-optional fields, `durationMinutes` first.
6. Fixture and RAML-example reconciliation.
7. LIBRA scenarios on the DD-43099 DSL; every XHIBIT scenario unchanged.
8. **The third story raised** in `cpp-context-stagingdlrm` (version bump + Group B mapping + CSV
   refresh), blocked on this one.
9. **An answer to DD-43081 T4 AC8** — which `vehicleRegistrationMark` home reaches Progression.

## Register of what this story does *not* implement

Deliberate exclusions with reasons, for the Technical Architect.

### R1 — Group C's 3 officer fields

`dxAddress`, `forename3`, `uniquePropertyReferenceNumber`. Accepted by canonical and dropped there.
`forename3` in fact **has** a home here (`personal-information.json#givenName3`); the other two would
need new properties on the shared, closed `pcf-address.json`. Excluded by decision, consistent with
stagingDLRM never sending them. **Question for the TA:** confirm the data is genuinely not needed
before both repos build a pipeline that silently swallows it.

### R2 — The cross-repo closure

Version bump, Group B converter mapping and impact-CSV refresh, all in `cpp-context-stagingdlrm`.
Separate story per the one-repo rule; raising it is deliverable 8.

### R3 — PCFDLRM-side rejection visibility

DD-43081 found that a stagingDLRM rule rejection makes a case **vanish from the reconciliation
report entirely**, and built a rejection event plus report changes to fix it. **Whether PCFDLRM has
the same hole has not been examined.** PCFDLRM does emit `defendantValidationFailed` /
`defendantValidationPassed` events on the DLRM channel, so a mechanism exists — but nobody has
traced it to the report. Flagged for its own investigation.

### R4 — The 7 fields that reach PCFDLRM and die before Progression

`backDuty`, `backDutyDateFrom`, `backDutyDateTo`, `prosecutorOfferAOCP`,
`prosecutorCompensation`/`appliedCompensation`, `middleName2`/`givenName3`, officer `forename3`. No
schema reachable from `courtReferral.json` declares them. Storing them here is fine **if** PCFDLRM
is the intended consumer; it is silent data loss if Progression was. **Question for the TA:** get an
explicit answer.

`vehicleMake` is **not** in this set — it reaches Progression via `offence.offenceFacts`. An earlier
ruling measured reachability from `apiProsecutionCase.json` and got it wrong. Do not re-derive this
column without reading impact §8's two traps first.

### R5 — plea / verdict / allocationDecision remain code-vs-UUID

The workbook models these as reference-data codes; the schemas model them as resolved UUIDs. No
resolver exists anywhere in the pipeline, and it affects XHIBIT equally. Pre-existing, not
LIBRA-specific, needs its own ticket.

### R6 — Out of this repo

The canonical schema, the source-system rule engine, the stagingDLRM rejection path and
`MigratedCaseConvertor` (DD-43081) · the Function App LIBRA gate (DD-43086) · reconciliation tooling
`--source-system` · `cpp-apitests`.

## Known blockers and open questions

- **LIBRA's `initiationCode` value(s) are undecided.** Needs the reference-data team. This
  determines change set 3 entirely — if the answer is `S`/`Q`, LIBRA routes into the already-built
  `SUMMONS`/`REQUISITION` sets and the change set shrinks to an assertion. Shared with DD-43081; one
  decision, needed by both. **Start this conversation on day one.**
- **No real LIBRA `case.json` / `manifest.json` sample exists yet.** Unit and component coverage does
  not need one; the end-to-end criterion does.
- **`organisationTelephoneNumber` may be a duplicate of the existing `telephoneNumberBusiness`.**
  DD-43081's workbook-corrections item 5 raises the same doubt from the other side (row 63 vs row
  62). Resolve before declaring a second field — a duplicate declaration is worse than a missing one.
- **`numPreviousConvictions` → `numberOfPreviousConvictionsCited` is a semantic narrowing.**
  "Previous convictions" and "previous convictions *cited*" are not obviously the same count. The
  target name appears in this repo only inside an inbound
  `public.progression.prosecution-case-created.json:16` fixture — i.e. it is a **Progression** name,
  not a PCFDLRM one, so the rename belongs in change set 2, not change set 1.
- **`prosecutorCompensation` → `appliedCompensation` semantics unconfirmed.** Names are suggestive,
  not proof. Do not map on the name alone.
- **core's `phone` and `nino` patterns diverge from stagingDLRM's** — stagingDLRM allows a leading
  `+` on phone, core does not. This story adds `fax` and possibly `organisationTelephoneNumber`, both
  phone-shaped, so a value valid upstream can fail core's pattern. Not caused here; reachable here.
- **ADR-003 acceptance** gates the implementation stage, because both repos encode the same field
  names and a mismatch is invisible until integration.

## Supporting analysis

All in the stagingDLRM repo, regenerable from workbook V0.13 via `tools/schema-gen/`, and **linked
rather than copied** so there is one source of record:

- [`libra-schema-impact.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-schema-impact.md)
  — §3 the work per schema, §5 the downstream tier triage (this story's scope), §8 the core-type
  divergences and the two reachability traps.
- [`libra-ingestion-analysis.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-ingestion-analysis.md)
  — §3.4 the XHIBIT-only behaviours PCFDLRM guards, §5 open questions.
- [`libra-schema-impact.csv`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-schema-impact.csv)
  — 165 rows. Filter `pcfdlrm_status=no_field` for this story's additions,
  `progression_status=exists_mandatory` for the officer-block constraint.
- [`libra-workbook-corrections.md`](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/analysis/libra-ingestion/libra-workbook-corrections.md)
  — DD-43081 T6's deliverable; items 5 and 11 bear directly on this story.

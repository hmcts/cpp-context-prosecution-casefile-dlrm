# Requirements — LIBRA enabler: PCFDLRM schema enablement

> Stage 1 artefact. Source: [`00-input-brief.md`](./00-input-brief.md).
> Requirements altitude — nothing here prescribes a class layout. Implementation **tasks** come from
> the design and story stages.
>
> **Updated for LIBRA 0.13.1** (`dlrm-libra-0.13.1.json` vs `dlrm-libra-0.13.json`,
> `cpp-context-stagingdlrm/docs/analysis/libra-ingestion/schema/libra/`): 0.13.1 no longer sends
> `officerInCase` (17 fields — 11 officer + a 6-field address) or `offence.convictionDate`. Both were
> live in the 0.13 workbook this story was written against; the field-key diff between the two schema
> files confirms both are absent from 0.13.1. **FR1, FR2, FR2a, FR3, FR4, FR9 and FR9a are void.**
> FR10 is revised to drop `convictionDate`. Group B for this story is now **one field** —
> `numPreviousConvictions` (FR5) — plus the rule-set routing (§C), tolerance (§D) and test (§F) work,
> none of which depended on the officer block or `convictionDate`. Voided items are struck through
> and retained below for the decision record.

## Story

**[DD-43130](https://tools.hmcts.net/jira/browse/DD-43130) — Receive and carry onward the LIBRA
Group B field set in PCFDLRM**

| | |
|---|---|
| Epic | [DD-43067](https://tools.hmcts.net/jira/browse/DD-43067) — LIBRA enabler |
| Size | **M** — becomes S if LIBRA's `initiationCode` turns out to be `S`/`Q`, L if it needs a new rule-set axis |
| Repo | `cpp-context-prosecution-casefile-dlrm` |
| Sibling story | [DD-43081](https://github.com/hmcts/cpp-context-stagingdlrm/tree/main/docs/pipeline/DD-43067-DD-43081-schema-enablement) — the stagingDLRM half |
| Blocks | the stagingDLRM close-out PR (version bump + Group B mapping + CSV refresh) — FR19. *(Revised 2026-08-18: tracked under this same ticket, not a separate story — see FR19.)* |
| Extends | [DD-43099](../DD-43067-DD-43099-pcfdlrm-test-hardening/) suites and the [ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md) DSL |
| Gated by | [ADR-003](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/003-libra-payload-contract.md) — **Accepted 2026-08-11**, extract team's confirmation still open. One amendment now owed to it: **§5** must become `O,C,Q,J,R` (FR12(1)). *(§3's officer-nesting amendment (FR2a) is void under LIBRA 0.13.1 — no officer block is sent.)* |
| Decided 2026-08-14 | LIBRA codes `O,C,Q,J,R`; `X`→`R` upstream; `J` validates as CPP SJP; ~~officer mapped onward with `gender: NOT_KNOWN`~~ *(void under LIBRA 0.13.1)*; `:368` fixed, other seven guards deferred; `prosecutorOffenceId` rule in scope |
| **Settled 2026-08-17** *(architect)* | **There is no `X` — it was a transcription error for `R`.** LIBRA's set is **`O, C, Q, J, R`**, `R` arrives as `R`, and **no translation layer exists or is needed**. FR12b stands. DD-43081's impact CSV and ADR-003 §5 both need correcting — see FR12(2), G1 |
| Follows | [ADR-002](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/002-source-system-keyed-dispatch.md) — names `CcProsecutionValidationRuleProvider` as precedent, does not bind it (FR12(5)) |
| Reviewed against | `cpp-context-stagingdlrm` @ `55bf1721` (2026-08-14). DD-43081 **T1 has landed** (`9681b155`) — rejection path + source-system rule engine, empty rule map |

### Summary (JIRA summary line)

~~`[LIBRA enabler] PCFDLRM: wire the orphaned officer-in-case schema into the closed migrated-case container, add the 3 missing Group B fields, carry them to Progression, and decide LIBRA's rule-set routing`~~

**Revised for LIBRA 0.13.1:** `[LIBRA enabler] PCFDLRM: declare and carry numPreviousConvictions, and
decide LIBRA's rule-set routing` — the officer-in-case schema and `convictionDate` are void; LIBRA
0.13.1 does not send either.

### User story

As a **service owner migrating magistrates' court cases from LIBRA**,
I want **PCFDLRM to accept the Group B field set stagingDLRM will send, carry it into the Progression
payload, and route LIBRA cases to a deliberately chosen rule set**,
so that **those fields are not write-only upstream, ~~LIBRA submissions carrying an officer are not
rejected outright~~ *(void — LIBRA 0.13.1 sends no officer)*, and LIBRA cases are not validated more
weakly than XHIBIT ones by accident**.

## Requirements

### A. Receive Group B

- **~~FR1 — Declare `officerInCase` on the migrated-case container.~~ VOID under LIBRA 0.13.1.**
  LIBRA 0.13.1 sends no `officerInCase` — the field-key diff between `dlrm-libra-0.13.1.json` and
  `dlrm-libra-0.13.json` shows the 17-field container gone entirely. There is no 4xx to prevent and
  nothing to reference `pcf-policeOfficerInCase.json` for. *(Original reasoning, against the 0.13
  workbook, retained below.)*
  `migrated/migrated-case-details.json` is `additionalProperties: false` with four properties. An
  `officerInCase` block sent by stagingDLRM against that schema is a **terminal 4xx**, so declaring
  it is not optional once DD-43081's Group B mapping ships. Reference the existing
  `pcf-policeOfficerInCase.json`, which is currently referenced by nothing.
  Treat the block as a **unit**, not as 17 independent fields — see FR9.

- **~~FR2 — Add the two missing officer properties.~~ VOID under LIBRA 0.13.1.** With FR1 void,
  `policeWorkerReferenceNumber` and `policeWorkerLocationCode` have no container to sit in, and
  `pcf-policeOfficerInCase.json` stays as it is — authored, unreferenced. *(Original reasoning
  retained below.)*
  `policeWorkerReferenceNumber` and
  `policeWorkerLocationCode` on `pcf-policeOfficerInCase.json`. Both are absent from every schema in
  this repo. The remaining officer fields already have homes — `forename`/`forename2`/`surname` on
  `personal-information.json`, the four contact fields on `contact-details.json`, the six address
  fields on `pcf-address.json`, and `policeOfficerRank` on the officer schema itself.
  Note PCF has neither of the two new fields, so "follow `cpp-context-prosecution-casefile`" does not
  apply here — PCF is not authoritative for fields it never receives.
  **These two are not optional additions.** Core's `policeOfficerInCase.json` marks both `required`
  (FR9), so without them PCFDLRM cannot build a valid officer block at all and FR9 would always take
  its omit branch. `pcf-policeOfficerInCase.json` is an *open* schema, so skipping them would drop
  them silently rather than 4xx — the quiet failure mode, and the reason to be explicit here.

- **~~FR2a — Settle the officer block's *shape*, not just its fields.~~ VOID under LIBRA 0.13.1.**
  There is no officer block to shape-check against canonical — G2 (the officer-nesting gate to
  DD-43081) is moot with it. *(Original reasoning retained below.)*
  ADR-003 §3 and the impact CSV declare the canonical officer block **flat**:
  `officerInCase.forename`, `.surname`, `.primaryEmail`, `.workTelephoneNumber`,
  `.policeWorkerReferenceNumber`, with `address` the only child object
  (`officerInCase.address.address1`).
  PCFDLRM's `pcf-policeOfficerInCase.json` is **nested**: `personalInformation` →
  `{firstName, lastName, givenName2, contactDetails{…}, address{…}}`.
  Those are different levels for the same fields, which breaks ADR-003's own compliance note 1
  ("no field is declared in canonical at a level PCFDLRM does not hold it at") — the rule ADR-003
  exists to enforce. The impact CSV already records the consequence field by field as
  `exists_different_name`.
  Two ways out — **this story recommends option 2** (decided 2026-08-14, once FR9 settled that the
  block is mapped onward):
  1. **PCFDLRM flattens** its officer schema to match canonical. Cheap in isolation — the schema is
     referenced by nothing — but it puts PCFDLRM's shape *furthest* from core's
     `personDetails{lastName, gender, address{}, contact{}}`, so the FR9 converter would re-nest from
     flat, and PCFDLRM would diverge from PCF's nested shape at the same time.
  2. **Canonical nests** to match PCFDLRM. ✅ **Recommended.** PCFDLRM's existing nesting
     (`personalInformation` → names + `contactDetails` + `address`) is already close to core's target
     shape, so the FR9 converter stays a level-preserving rename — the convention ADR-003 §"Context"
     cites as the reason canonical follows PCFDLRM in the first place. It is also what ADR-003's own
     compliance note 1 requires, and it matches PCF. The cost is amending an Accepted ADR and
     DD-43081's planned `officer-in-case.json`.
  Raise it as a *consistency* fix, not a preference: option 1 makes canonical the only schema in the
  chain whose officer shape matches neither PCFDLRM nor core.
  **`officer-in-case.json` has not landed in `cpp-context-stagingdlrm` yet** (verified 2026-08-14 at
  `55bf1721`), so settling this now costs a schema edit and settling it later costs a coordinated
  three-party change. Raise immediately; this is the same "confirm it before the extract is written"
  argument ADR-003 makes for itself.

- **~~FR3 — Add `fax` to `contact-details.json`.~~ VOID under LIBRA 0.13.1.** `fax` was the
  receiving home for the officer's `faxNumber` (FR1/FR2). With the officer block gone, nothing sends
  `fax`, so widening the shared, closed `contact-details.json` has no purpose. *(Original reasoning
  retained below.)*
  The receiving home for stagingDLRM's `faxNumber`.
  This schema is `additionalProperties: false` **and shared** — `$ref`'d from
  `personal-information.json` — so the addition widens every parent that reaches it. Additive and
  safe, but it must be reviewed as a shared-schema change rather than an officer-local one.
  **Corroborated end-to-end (2026-08-14):** core's `global/contactNumber.json` — the target of
  `person.json#contact`, and so of the FR9 officer mapping — already carries a `fax` property with no
  `required` list. The field therefore has a home at both ends of the hop, not just at our inbound
  gate, and `fax` is the right name to use here.

- **~~FR4 — Add `convictionDate` to `migrated/migrated-offence.json`.~~ VOID under LIBRA 0.13.1.**
  The field-key diff between `dlrm-libra-0.13.1.json` and `dlrm-libra-0.13.json` shows `convictionDate`
  dropped alongside `officerInCase`. Nothing sends it; no schema change is needed.

- **FR5 — Add `numPreviousConvictions` to `migrated/migrated-defendant.json`.** Declared under the
  ADR-003 name it arrives with. The rename to Progression's
  `numberOfPreviousConvictionsCited` belongs at the outbound seam (FR10), not here — that name
  appears in this repo only inside an inbound `public.progression.prosecution-case-created.json`
  fixture, i.e. it is a Progression name.

- **~~FR6 — Resolve `organisationTelephoneNumber` before declaring it.~~ RESOLVED — no work.**
  *Closed 2026-08-14 against DD-43081.* The field is **not in LIBRA 0.13** (zero occurrences in
  `dlrm-libra-0.13.json` and in `staging-dlrm-canonical-flattened.json`), and DD-43081 FR8 records it
  as dropped from Group B on that basis. What LIBRA does send is `companyTelephoneNumber`, at
  `defendants[*].individual.parentGuardianInformation` — the impact CSV marks it
  `exists_same_constraint` / `already_flowing` / `change_required=no`. `telephoneNumberBusiness` is
  `not_in_libra` / `already_flowing`, also no change.
  So there is no duplicate to resolve and no field to declare. **Group B is 19 fields, not 20** —
  DD-43081's `02-design.md:13` still says 20 and is stale on this point.
  **Further update, LIBRA 0.13.1:** with FR1/FR2 (officer, 17 fields) and FR4 (`convictionDate`, 1
  field) also void, **Group B is 1 field for this story — `numPreviousConvictions` (FR5)**. The other
  18 of the original 20 are accounted for: 17 void (officer), 1 void (`convictionDate`), 1 already
  resolved with no work (this FR6, `organisationTelephoneNumber`).

- **FR7 — Every addition is optional, and no existing constraint changes.** An XHIBIT payload valid
  before this story is valid after it. No `maxLength`, `pattern`, `minimum`, `maximum`, `type` or
  `required` on any existing field is touched.

- **FR8 — Confirm the runtime entry schemas still need no edit.**
  `pcfdlrm-command-handler`'s `pcfdlrm.command.receive-migrated-case-file.json` and
  `pcfdlrm-command-api`'s `pcfdlrm.receive-migrated-case-file.json` both `$ref`
  `migrated-case-details.json` rather than restating it, so FR1–FR6 propagate to the inbound gate
  automatically. Verified at `04c0b2d1` — re-confirm rather than assume, because the sibling repo's
  equivalent file *does* diverge from canonical (DD-43081 finding F6).

### B. Carry Group B onward to Progression

The outbound seam is `MigratedCaseReceivedProcessor:51-57`, which converts to
`InitiateCourtProceedings` and sends `progression.initiate-court-proceedings`. The reachability root
is therefore **`courtReferral.json`**, not `apiProsecutionCase.json`.

- **~~FR9 — Carry the officer block to Progression, all-or-nothing.~~ VOID under LIBRA 0.13.1.**
  With FR1/FR2 void, there is no officer block reaching PCFDLRM to carry onward. The converter work
  this FR specified (`buildPoliceOfficerInCase`, FR9a's `gender: NOT_KNOWN` default) does not need
  building. *(Original reasoning, verified against the core jar on 2026-08-14, retained below — it
  is still evidence for the day the officer block returns in a future LIBRA revision.)*
  *The "re-verify against the core jar" instruction is discharged.* Read from
  `criminal-court-public-model:17.104.4` — the version `pom.xml:25` pins — not from analysis:

  | Core schema | `required` |
  |---|---|
  | `global/policeOfficerInCase.json` | `personDetails`, `policeOfficerRank`, `policeWorkerReferenceNumber`, `policeWorkerLocationCode` |
  | `global/person.json` | `lastName`, **`gender`** |
  | `global/address.json` | `address1` — *conditional, only when the address object is sent* |
  | `global/contactNumber.json` | none — and it carries a **`fax`** property |

  The block is reachable and optional at the target: `courtReferral.json` → `prosecutionCases[]` →
  `prosecutionCase.json:32` declares `policeOfficerInCase`, and `prosecutionCase`'s own `required` is
  `id, prosecutionCaseIdentifier, initiationCode, defendants`. So omitting is safe and sending a
  complete block is safe; only a **partial** block fails.
  **Correction to the inherited claim:** it said "plus officer `lastName` and `address1`". `address1`
  is required only *conditionally*; the unconditional second field is **`gender`**.

  **~~FR9a — `gender` is defaulted to `NOT_KNOWN`.~~ VOID under LIBRA 0.13.1 — no officer block to
  default a gender on.** Core requires a gender on
  `personDetails`; LIBRA's Group B officer set has no gender field and no way to derive one. Core's
  enum is `MALE, FEMALE, NOT_KNOWN, NOT_SPECIFIED`, and `NOT_KNOWN` is the value that means exactly
  what is true here. **Precedent in this repo:** `ProsecutionCaseFileHelper:270,280` already defaults
  defendant and parent/guardian gender to `NOT_KNOWN`. This is recording absence, not inventing data,
  and it is the only synthesised value in the officer mapping — everything else comes from LIBRA.

  **The rule.** Send the officer block **iff** `surname`, `policeOfficerRank`,
  `policeWorkerReferenceNumber` and `policeWorkerLocationCode` are all present; set
  `personDetails.gender` to `NOT_KNOWN`; include `address` only if `address1` is present; otherwise
  **omit the block entirely**. A partially-populated block must not be sent.
  This is a **live path, not a theoretical one**: every Group B addition is optional in canonical
  (DD-43081 FR11), so a LIBRA case can legitimately send an officer without the two police-worker
  fields.

  **Why we map it when PCF does not.** `cpp-context-prosecution-casefile` declares
  `caseDetails.otherPartyOfficerInCase` (`case-details.json:45`), carries it in its domain and public
  events, and **never reads it** — no getter reference, no string-literal access, and
  `CCCaseToProsecutionCaseConverter` builds the payload field-by-field with no whole-object copy. So
  PCF accepts the block and drops it.
  That is **PCF being silent, not PCF being opposed** — there is no officer mapping to copy, so the
  "follow PCF" rule does not reach this decision, exactly as it does not reach `prosecutorOffenceId`
  or the `isXhibit` guards. Deciding on the merits:
  1. This story's user story exists so these fields "are not write-only upstream". Accepting the
     officer into PCFDLRM's event stream and dropping it before Progression reproduces that same
     complaint one hop downstream.
  2. DD-43081 classifies the officer as **Group B** — "fields Progression models but PCFDLRM does
     not", i.e. carry-onward. Group C is the accept-and-drop bucket (`dxAddress`, `forename3`,
     `uniquePropertyReferenceNumber`). Demoting the officer to Group C would contradict the epic's
     own classification and needs DD-43081's agreement.
  3. Every core-required field has a LIBRA source once FR9a supplies the gender.
  PCF's non-population reads as a gap in PCF, not a statement about the contract.

- **FR10 — Carry `numPreviousConvictions` onward, applying the rename at this seam.**
  *(Revised twice: was three fields; FR6 closed, dropping `organisationTelephoneNumber`. Now under
  LIBRA 0.13.1, FR4's `convictionDate` is also void, so this is **one field, not two**.)*
  `numPreviousConvictions` → `numberOfPreviousConvictionsCited`, in
  `ProsecutionCaseFileMigratedDefendantToCCDefendantConverter`, which today mentions neither previous
  convictions nor vehicles. The officer block, previously the third piece of work at this seam, is
  void — see FR9.

- **FR11 — Answer DD-43081 T4 AC8: which `vehicleRegistrationMark` reaches Progression?** The field
  is declared **twice** in this repo — flat on `migrated/migrated-offence.json:111` as a core `$ref`,
  and on `vehicle-related-offence.json:10` as a plain string. DD-43081 T4 AC8 says *"confirm with the
  PCFDLRM team before mapping"*; **this story is that team.** Determine which home reaches
  `offenceFacts.vehicleRegistration` and record the answer, so the sibling story can map Group A
  correctly. No schema change here — `vehicleRegistrationMark` is Group A and already present.
  **ANSWERED 2026-08-14 from PCF — map the flat one.** `InitiateCCProsecutionApi:191-195` and its
  SJP twin `InitiateSjpProsecutionApi:216-220` both do:
  ```java
  String vehicleRegistrationMark = offence.getVehicleRegistrationMark();
  if (isBlank(offence.getVehicleRegistrationMark())
          && nonNull(offence.getVehicleRelatedOffence())
          && isNotBlank(offence.getVehicleRelatedOffence().getVehicleRegistrationMark())) {
      vehicleRegistrationMark = offence.getVehicleRelatedOffence().getVehicleRegistrationMark();
  }
  ```
  So **flat wins and `vehicleRelatedOffence` is the fallback**, and PCF then back-fills the other
  direction so both stay in sync, emitting `.withVehicleRegistrationMark(…)` *and*
  `.withVehicleRelatedOffence(…)`. DD-43081 T4 should map Group A to the **flat**
  `migrated-offence.json` home. Communicate to the DD-43081 owner (AC13).

### C. Route LIBRA to a chosen rule set

- **FR12 — Decide and implement LIBRA's rule-set routing; do not let it default.** Rule sets are
  keyed on the raw `initiationCode` string (`ProsecutionCaseFileHelper:97`), and an unmapped code
  **does not fail**: `getValidationRules` falls back to `COMMON + SPI/NON_POLICE +
  DEFAULT_DEFENDANT_RULE_SET`, `getCaseValidationRules` to `getOrDefault(code,
  COMMON_CASE_RULE_SET)`.
  **Correction (2026-08-14): the fallback is not uniformly weaker.** `DEFAULT_DEFENDANT_RULE_SET`
  (`ArrestDate`, `ChargeDate`, `CustodyStatus`) is a **superset** of `OTHER_DEFENDANT_RULE_SET`
  (`CustodyStatus` alone), so a Remitted case takes *more* rules than an Other case — including
  arrest-date checks unlikely to be meaningful on a remitted mag case. The real failure mode is
  **validation nobody chose**, in either direction.
  **This is live and load-bearing** — `R` is confirmed in LIBRA's set (see (2)), so remitted cases do
  reach PCFDLRM and the asymmetry above is exactly what FR12b has to resolve.
  Required:
  1. **LIBRA's `initiationCode` set is `O, C, Q, J, R`. DECIDED 2026-08-14, CONFIRMED 2026-08-17.**
     `O` Other, `C` Charge, `Q` Postal Charge, `J` Single Justice Notice, `R` Remittance. All five sit
     inside the platform's seven (`Q,R,S,C,J,Z,O`), so PCFDLRM needs no schema change — its
     `initiationCode` is a plain `string`. *(The set briefly read `O, C, Q, J` on 2026-08-17 while `X`
     was believed to be a distinct LIBRA-only code; the architect has since confirmed `X` was a
     transcription error for `R` — see (2).)*
     **Upstream, this is a blocker, and DD-43081 currently plans the opposite.**
     `case-details.json` in canonical declares `enum: ["O"]` — correct today, because **XHIBIT sends
     only `O`**. DD-43081's `01-requirements.md` FR1 reads the LIBRA 0.13 workbook as also `["O"]` and
     concludes "no widening, so the whole initiation-code thread (schema change, allowed-values rules,
     reference-data decision) is **gone**". **The workbook snapshot is out of date — LIBRA will send
     new codes**, so canonical as it stands rejects `C`, `Q`, `J` and `R` outright, before PCFDLRM
     sees anything.
     Three things DD-43081 must reinstate: **widen** the canonical enum to cover LIBRA's five;
     **keep** the XHIBIT allowed-values rule pinned at `["O"]` — now a real constraint rather than a
     formality; and **add** a LIBRA allowed-values rule at `O, C, Q, J, R`. That is precisely the
     shape [ADR-002](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/002-source-system-keyed-dispatch.md)
     §4 already anticipates — "`InitiationCodeValidationRule.withAllowedValues(…)` registered under
     `XHIBIT` with one code set and under `LIBRA` with another".
     **ADR-003 §5 must be updated too**: it says "LIBRA is expected to use `C, J, Q, S`", which omits
     `O` and `R` and wrongly adds `S`. **Cross-repo action on DD-43081 — see G1.**
  2. **There is no `X`. LIBRA sends `R` (Remittance) directly. CONFIRMED WITH THE ARCHITECT
     2026-08-17.** The `X` that appears in the LIBRA workbook and in DD-43081's impact CSV is a
     **transcription error for `R`** — the code has always been `R`, CPP's own Remitted/Remittance
     code. There is therefore **no second code, no translation layer, and nothing to exclude**:
     `R` arrives as `R`, is already in the platform's seven, and PCFDLRM needs no change to receive it.
     **Cross-repo action:** DD-43081's `libra-schema-impact.csv` records LIBRA's set as
     `C, J, O, Q, X` and ADR-003 §5 as `C, J, Q, S`. **Both are wrong and both should say
     `O, C, Q, J, R`** — see G1. The CSV matters because it is the shared artefact other stories read.
     **Why this is worth stating rather than just deleting the `X` thread.** `X` exists nowhere in
     CPP — not in core (`criminal-court-public-model/.../global/initiationCode.json` is
     `J,Q,S,C,R,O,Z`, and a grep for `"X"` across every core schema returns zero hits), not in PCF's
     identical `plea/initiation-code.json`, and not in the generated
     `uk.gov.justice.core.courts.InitiationCode`. That absence was the first clue the workbook was
     wrong, and it is the reason a stray `X` must never be allowed through: PCFDLRM's
     `case-details.json` declares `initiationCode` as a plain `"type": "string"` with no enum, so an
     `X` would be accepted at the inbound gate, take the fallback rule set, be discarded at defendant
     level by `isValidInitiationCode` (the seven-value enum has no `X`), and finally reach
     `MigratedCaseToProsecutionCaseConverter:92` where `valueFor("X")` returns `Optional.empty()` and
     writes `null` into `initiationCode` — a field core's `prosecutionCase.json` marks **required**.
     That is a late failure at the outbound Progression boundary, after the case has been accepted and
     the domain event emitted. **So canonical's LIBRA enum must be exactly `O, C, Q, J, R`**: `R`
     admitted, `X` not silently tolerated if the extract is ever built from the uncorrected workbook.
     *Decision history: `X`→`R` translation (08-14) → drop `X` (08-17, morning) → **no `X` exists**
     (08-17, architect). The first two both rested on the workbook's `X` being real.*
  3. **Route each of the five codes deliberately.** `O`→`OTHER`, `C`→`CHARGE`, `Q`→`REQUISITION`
     are map hits. **`J` validates as CPP SJP — CONFIRMED 2026-08-14**, so it takes
     `SJP_CASE_RULE_SET` at case level and `SPI_DEFENDANT_RULE_SET_FOR_INITIATION_CODE` at defendant
     level, which drops `PleaValidationRule`, `VerdictValidationRule` and
     `VehicleCodeValidationAndEnricherRule`. That is now intended behaviour, not an accident, and
     FR13 must assert it so a later change cannot quietly move `J` back onto the common path.
     **`R` is in the set and has no map entry — see FR12b**, which is **reinstated** now that `R` is
     confirmed as LIBRA's real Remittance code.
  4. **Decide the eight `isXhibit` guards** — see FR12a.
  5. **Dispatch by a source-system-keyed map, not a conditional.**
     [ADR-002](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/002-source-system-keyed-dispatch.md)
     names `CcProsecutionValidationRuleProvider` explicitly as the third instance of this problem and
     offers its shape as precedent. Its compliance notes forbid `if`/`switch` on source system in a
     shared path and require the map be the only place a source system is named in control flow.
     **One caveat before adopting it wholesale:** ADR-002 §6 relies on the key being a validated
     enum. PCFDLRM's `migrationSourceSystemName` is a **plain `string`** on
     `migrated-migrationSourceSystem.json` and is **not in that schema's `required` list**, so an
     absent or unrecognised value is a real runtime input here, and a map miss needs a chosen
     fallback rather than being treated as a programming error.
  Two facts the design must account for: the DLRM channel **aliases the SPI rule sets**
  (`defendantValidationMapDlrm = defendantValidationMapSpi`, `CcProsecutionValidationRuleProvider:291`),
  which is PCF's magistrates-court map — so LIBRA already inherits the right defendant rules and the
  **defendant layer needs no change**; and a defendant-level `initiationCode` **overrides** the
  case-level one when valid (`ProsecutionCaseFileHelper:93-94`), so LIBRA's value matters at both
  levels. Note `isValidInitiationCode` tests against the seven-value enum, so a raw `X` at defendant
  level would be silently discarded in favour of the case-level code — a third argument for keeping
  `X` out of the pipeline entirely, per (2).

- **FR12b — Decide Remitted's rule set. IN SCOPE. Recommendation revised 2026-08-17 — DECISION
  OPEN.** *(Withdrawn briefly on 2026-08-17 when `X` was thought to be a separate code; reinstated
  the same day once the architect confirmed `X` **is** `R`.)*
  `R` is in LIBRA's set and is **not** a key in `defendantValidationMapDlrm`, so it falls through to
  `COMMON + SPI + DEFAULT_DEFENDANT_RULE_SET`. Two candidate answers, and the evidence now points
  away from the original recommendation:

  **Option A — leave it on the fallback (`DEFAULT_DEFENDANT_RULE_SET`). PCF parity. Now recommended.**
  PCF has no `R` in its validation `CaseType`, no `R` key in any of its five defendant maps, and no
  occurrence of `REMITTED` anywhere in Java, JSON or RAML — it lets `R` take the same fall-through
  PCFDLRM has today. That is **not dormant code**: seven PCF integration fixtures carry
  `initiationCode: "R"`, including the primary happy path
  (`prosecutioncasefile.command.initiate-cc-prosecution-without-error.json`, channel `SPI`, **no**
  `feeStatus`, and **both `arrestDate` and `chargeDate` supplied**). So PCF's position is that a
  remitted case carries its dates like any other, and the `feeStatus` argument below does not
  distinguish `R` from PCF's own exercised behaviour. **This repo's "follow PCF" tie-breaker reaches
  this decision** — PCF is not silent here, unlike the officer block (FR9/F5). Costs nothing and
  touches no map.

  **Option B — map `R` to `OTHER_DEFENDANT_RULE_SET`** (the 2026-08-14 recommendation, retained in
  full below). If chosen, note the implementation hazard: `defendantValidationMapDlrm =
  defendantValidationMapSpi` (`CcProsecutionValidationRuleProvider:291`) is a reference **alias**, not
  a copy, and both are built with `com.google.common.collect.ImmutableMap.of` — so the entry cannot be
  "added", the map must be redeclared, and doing it in the obvious place would **also change the SPI
  channel**. Option B therefore means declaring a genuinely separate `defendantValidationMapDlrm`
  (following the existing `…MCC` / `…ForGroupCivilCases` precedent). Note also that the map's other
  values are *composed triples*, so `R` would take `COMMON + SPI + OTHER_DEFENDANT_RULE_SET` — the
  same value as `O` — not the bare `OTHER_DEFENDANT_RULE_SET` the shorthand suggests.

  **Open question, for the architect or the LIBRA extract owner:** do remitted LIBRA cases reliably
  carry `arrestDate` and `chargeDate`? Both fields exist in LIBRA 0.13 and DD-43081's impact CSV marks
  them `exists_same_constraint` / `already_flowing` ("Identical on both sides"), but they are
  `not_validated` at the gate — so the contract permits their absence and the schema cannot answer it.
  **If yes → Option A.** If they are routinely absent, Option B's reasoning holds and B is correct.
  **Low urgency either way:** defendant-level problems for LIBRA are still discarded by the `isXhibit`
  guards at `:282` and `:562`, which FR12a leaves in place, so a wrong choice here is invisible until
  the deferred guards story lands and is a one-line correction.

  ---
  *(Original analysis and Option B rationale, 2026-08-14. Retained in full — it is the argument for
  Option B and the record of why the asymmetry matters.)*
  `R` is not a key in `defendantValidationMap`, so today it falls through to
  `COMMON + SPI + DEFAULT_DEFENDANT_RULE_SET` (`ArrestDate`, `ChargeDate`, `CustodyStatus`) — a
  *stricter* set than `O`'s (`CustodyStatus` alone) and as strict as `C`'s on dates.
  **Why this bites for LIBRA specifically.** `ArrestDateValidationRule` and `ChargeDateValidationRule`
  each have an absence half gated on `isNull(caseDetails.getFeeStatus())`, which raises a problem for
  every offence lacking the date. **`feeStatus` has zero occurrences in LIBRA 0.13 and in canonical**
  — LIBRA never sends it — so that half always fires for LIBRA. The future-date half is harmless and
  applies sensibly everywhere.
  **Recommended: map `R` to `OTHER_DEFENDANT_RULE_SET`, the same as `O`.** Remission is a procedural
  event — a case moving between courts — and says nothing about how the case was initiated; a
  remitted case may have begun as a postal charge or summons with no arrest at all. `O` is the
  not-otherwise-classified bucket, which is what `R` is from a validation standpoint. Taking the
  fallback instead would validate remitted cases as strictly as `C`, the one code where an arrest
  date is genuinely expected.
  Two related facts to carry into design, **live regardless of which option is chosen**: the agreed
  `C` routing **will** demand an arrest date on every offence for the same `feeStatus` reason —
  expected for a charge, but now explicit rather than incidental; and the problem code raised for a
  *missing* arrest date is `ARREST_DATE_IN_FUTURE`, a pre-existing misnomer that will make problem
  output confusing. Renaming it is out of scope.

- **FR12a — Decide each of the eight `isXhibit` guards. NEW.** The original FR12.2 said "the two
  XHIBIT-guarded branches", which undercounted: `MigratedCaseFileAggregate:526` is the *method*, and
  it has six call sites. The full set is `:221` (case-problem rejections), `:282` (case warnings),
  `:313` (offence warnings), `:368` (**no-materials → `MigratedCaseFileReceived`**), `:423` (hearing
  warnings), `:433` (offence/plea/verdict rejections), `:554` (no-matching-defendants rejection), and
  `ProsecutionCaseFileHelper:118` (defendant field auto-correction).
  Seven of the eight degrade quietly — LIBRA simply gets no warning events and no rejections.
  **`:368` is lossy**: with no materials and a non-XHIBIT source, no event is emitted at all, so the
  case never reaches Progression and produces no outcome. stagingDLRM's test harness carries a
  `fixeddatenomaterial` case, so the shape is real.
  **DECIDED 2026-08-14 — fix `:368` only; the other seven stay as they are.** Scope for this story
  is the single guard whose failure is lossy. The remaining seven mean LIBRA raises no
  `MigratedCaseValidatedWithWarnings` events and takes no rejection branch — **accepted for a first
  stab, and it must be stated in the story rather than discovered**: a LIBRA case with case-level,
  hearing-level or offence-level problems will proceed to Progression carrying them, silently. Raise
  the follow-up story that works through the remaining seven before LIBRA volume ramps.
  Related one-line defect on the same path: `isXhibit` at `:526` calls
  `.getMigrationSourceSystemName().equals(XHIBIT)` unguarded, and that field is not `required`, so a
  payload carrying `migrationSourceSystem: {}` NPEs. Reverse the comparison while touching it.

- **FR13 — Assert the chosen routing.** A test must fail if a future change silently drops LIBRA back
  to the default rule set. **This requirement survives even if the answer to FR12(1) is "`S`, so
  nothing changes"** — the assertion is the deliverable, not the routing code.

- **FR14 — Resolve `getDlrmDefendantValidationRules`.** `CcProsecutionValidationRuleProvider:332`
  returns `getOrDefault(code, null)`, carries the comment `// What will fo here`, and is **called from
  nowhere**. Either complete it as part of FR12's routing or delete it. Leaving a private,
  null-returning DLRM rule selector in place while adding LIBRA routing beside it is how the wrong one
  gets wired up later. **Decide this before FR12's design**, not after — FR12 either uses that seam or
  removes it, and doing them in the wrong order means designing the routing twice.

- **FR15 — `initiationCode` needs no schema change *in this repo*.** *(Revised 2026-08-14 — an
  earlier version said DD-43081 "keeps it a typed enum, widened to `Q,R,S,C,J,Z,O`". It does not:
  their FR1 leaves canonical at `enum: ["O"]` and drops the widening. That is the G1 blocker, not a
  PCFDLRM issue.)* Canonical is a **typed enum** and must widen upstream; PCFDLRM's is already a
  plain `string` in both
  `case-details.json:21` and `migrated/migrated-defendant.json:89`, so any of the five LIBRA values
  deserializes here. **The "confirm no PCFDLRM code assumes `O`" check is CLOSED — it does not.**
  The only initiation-code literals in main code are `validation/CaseType.java:4-8` (the rule-map key
  definitions themselves) and `INITIATION_CODE_J` in `DefendantInitiationCodeValidationRule`, which is
  a deliberate case-vs-defendant `J` consistency rule, not an `O` assumption.
  `isValidInitiationCode` (`ProsecutionCaseFileHelper:204`) tests against `InitiationCode.values()` —
  the seven-value enum, which already includes `R`.
  **So the entire PCFDLRM-side widening is at most one map entry.** `caseValidationMap` is keyed on
  `S, C, Q, J`, so `O` and `R` both take the `getOrDefault` fallback and are treated alike — no gap.
  `defendantValidationMap` is keyed on `S, C, Q, O, J` — `O` has an entry, `R` does not, so `R` alone
  falls to the stricter `DEFAULT_DEFENDANT_RULE_SET`. That asymmetry is the whole of FR12b, and
  whether it needs correcting is FR12b's open question.
  **Resolved 2026-08-14, re-confirmed 2026-08-17 — "expected impact is nil" holds, and now needs no
  upstream translation to hold.** With `X` confirmed as a transcription error rather than a real
  code, PCFDLRM receives `O, C, Q, J, R` directly — all five already in every enum in the chain. No
  schema change here.
  Also note the runtime gate is refdata, not the enum: `CaseInitiationValidationRule`
  validates against `referenceDataVO.getInitiationTypes()` in both this repo and PCF (the two classes
  are functionally identical), and the shared IT stub carries `J,Q,S,C,R,O` — no `Z`, no `X`. So the
  schema enum is not evidence of what is accepted at runtime.

### D. Tolerate what stagingDLRM stops enforcing

- **FR16 — Tolerate the newly-optional fields. AUDITED 2026-08-14, CORRECTED 2026-08-14 — all five
  are already clean; no code change needed.**

  > **Correction.** An earlier revision of this requirement said DD-43081 relaxes **10** constraints
  > affecting **seven** fields, and identified `prosecutorOffenceId` as a silent-data-loss defect. That
  > was read from DD-43081's `02-design.md` schema table, which is **stale**. Its `01-requirements.md`
  > FR1 — revised against `schema-diff_3.html` and authoritative — relaxes **five** constraints, and
  > states explicitly that `hearings[*].durationMinutes` and `defendants[*].offences[*].prosecutorOffenceId`
  > "are now required on **both** sides in LIBRA 0.13, so LIBRA satisfies them and no relaxation is
  > needed." The impact CSV agrees: both are `already_flowing` / `change_required=no`. Neither field is
  > newly absent, and neither is in scope.

  The five relaxations are all on `caseDetails`, covering six fields. Traced through main code:

  | Field | Read at | Handling | Verdict |
  |---|---|---|---|
  | `dateReceived` | **nowhere** — declared at `case-details.json:36`, never read in Java | n/a | no-op |
  | `receiptType` | `ReceiptTypeValidationRule:31` | problem on null | moot — rule is XHIBIT-scoped by FR12 |
  | `receivingCourt` | `ReceivingCourtValidationRules:27` | `isNull → VALID` | safe, and moot for the same reason |
  | `retrialIndicator` | `MigratedCaseToProsecutionCaseConverter:103` | straight copy, not `required` at the target | safe |
  | `dateOfCommittal` / `dateOfSending` (the `anyOf`) | same converter `:102` / `:106` | `Optional.ofNullable(…).orElse(null)` | safe |

  **No production change is required by this requirement.** Its deliverable is the confirming tests —
  each field accepted when absent — because absence becomes newly reachable and no existing test
  exercises it.

  **Latent, not in scope: the `prosecutorOffenceId` matching gap.** A null never matches at
  `ProsecutionCaseFileHelper:177`, so `:181` returns empty and `:164` collapses the list to
  `List.of()`, which empties `listDefendantRequests` and drops every hearing for that case at
  `…InitialHearingToCCHearingRequestConverter:255` — silently, since `:554` is `isXhibit`-gated.
  This repo's `migrated-offence.json` does not list the field as `required`, so the path exists. But
  **canonical does require it and LIBRA supplies it**, so nothing reaching PCFDLRM can trigger it.
  Recorded as a robustness gap for the deferred `isXhibit` guards story, **not** as LIBRA work.

### E. Contract hygiene

- **FR17 — Reconcile the fixtures and RAML examples against the schemas.**
  `numPreviousConvictions` appears in **24** of this repo's JSON fixtures while being declared in **no
  schema in either repo**, and canonical's `migrated-defendant.json` is `additionalProperties: false`.
  Those fixtures encode a field the contract does not have, which means **they cannot be used as
  evidence of what the contract allows**. When FR5 declares the field for real, reconcile them against
  the schema rather than assuming they were already correct, and expect the same pattern for the other
  fields. This repo's RAML examples are not build-validated and have drifted the same way.
  The **workbook**-correction list is DD-43081 T6's deliverable and is not duplicated here.

### F. Tests

- **FR18 — Extend the DD-43099 suites; do not fork them and do not touch DD-43078's.** LIBRA
  scenarios are added as scenario data on the existing DSL
  ([ADR-001](https://github.com/hmcts/cpp-context-stagingdlrm/blob/main/docs/pipeline/adrs/001-dlrm-scenario-test-dsl.md)),
  through the delivered `pcfdlrm-test-support` (`FixtureLoader`, `WholePayloadMatcher`). Source system
  is a **scenario parameter** — no LIBRA-specific test class, no `if` on source system inside a test.
  Unit/component: exhaustive — every new field carried through, ~~the officer block accepted whole and
  refused partial~~ *(void under LIBRA 0.13.1 — no officer block)*, the chosen rule routing asserted,
  each relaxed field accepted when absent.
  Integration: **one representative LIBRA journey**, at the depth DD-43099 established.
  A **whole-payload assertion at the `receive-migrated-case-file` boundary** is required, not a
  field-presence spot check — it is the only assertion that catches an ADR-003 name mismatch, which is
  otherwise invisible to both repos' unit suites.

- **NFR1 — No XHIBIT regression.** Every XHIBIT scenario from DD-43099 passes unchanged, with no
  XHIBIT fixture edited. This is the regression signal that story was built to provide and the exit
  criterion for this one.

### G. Cross-repo obligation

- **~~FR19 — Raise the stagingDLRM close-out story, at the start of this one.~~ REVISED 2026-08-18 —
  no new ticket.** Two things become possible in `cpp-context-stagingdlrm` only once this story
  releases: the Group B converter mapping, compile-blocked at `pcfdlrm.version 17.104.21`; and the
  impact-CSV claims, which `tools/schema-gen/build-schema-impact.py` re-verifies against this repo's
  checkout on every run and **hard-fails** on a stale one
  (`sys.exit("error: curated downstream claims no longer hold: …")`, line 1120).
  **Decision (2026-08-18):** the one-repo-per-story rule would normally make that a separate Jira
  ticket, blocked by this one. It is not this time — **the stagingDLRM close-out work (version bump +
  Group B mapping + CSV refresh) is tracked under DD-43130 itself**, as a PR in
  `cpp-context-stagingdlrm` referencing this ticket, not a new one. The obligation is unchanged: the
  CSV claims go stale the moment DD-43130 **merges**, so that PR still needs to land promptly — it
  just isn't its own ticket. This story's own deliverable does **not** contain the fix.

## Acceptance criteria

Provable **in this repo alone** unless marked otherwise.

- **~~AC1~~ VOID under LIBRA 0.13.1** — *Was:* Given a migrated case file carrying a complete
  `officerInCase` block, when PCFDLRM processes it, then the block is accepted, every field
  deserializes, and none is silently dropped. LIBRA 0.13.1 sends no `officerInCase`; FR1 is void.
- **~~AC2~~ VOID under LIBRA 0.13.1** — *Was:* Given the same payload **without** FR1's declaration,
  when validated against `migrated-case-details.json`, then it is rejected — demonstrating the 4xx
  this story prevents. *(A test that pins why FR1 exists; delete it only with the reason recorded.)*
  With FR1 void there is no declaration to pin.
- **AC3** Given a case carrying `numPreviousConvictions`, when PCFDLRM processes it, then it is
  persisted under its ADR-003 name. *(Revised twice: organisation telephone number removed — FR6
  resolved, LIBRA 0.13 does not carry it. `convictionDate` removed — FR4 void, LIBRA 0.13.1 does not
  carry it either.)*
- **~~AC3a~~ VOID under LIBRA 0.13.1** — *Was:* Given the officer block, when its shape is compared
  against canonical, then every officer field sits at the same level on both sides, and the FR2a
  decision is recorded with its date and owner. There is no officer block to shape-check; FR2a and
  G2 are void with it.
- **~~AC4~~ VOID under LIBRA 0.13.1** — *Was:* Given a LIBRA case whose `officerInCase` block is
  missing any of `surname`, `policeOfficerRank`, `policeWorkerReferenceNumber` or
  `policeWorkerLocationCode`, when the Progression payload is built, then the block is **omitted
  entirely** — never sent partial. No officer block reaches PCFDLRM to be partial or complete.
- **~~AC4a~~ VOID under LIBRA 0.13.1** — *Was:* Given a complete `officerInCase` block, when the
  Progression payload is built, then `personDetails.gender` is `NOT_KNOWN`, and `address` is present
  only when `address1` is. FR9a's `gender: NOT_KNOWN` default has nothing to apply to.
- **AC5** Given a case carrying `numPreviousConvictions`, when the
  `progression.initiate-court-proceedings` payload is built, then the field appears under its
  Progression name, asserted as a **whole payload**, not by field-presence spot check. *(Revised —
  was "the officer and the two non-officer fields"; the officer and `convictionDate` are void.)*
- **AC6** Given **each of LIBRA's five `initiationCode` values** — `O`, `C`, `Q`, `J`, `R` — when a
  LIBRA case is validated, then the rule sets applied are the ones FR12(3) and FR12b chose, asserted
  explicitly per code. In particular `J` applies the SJP sets, and **`R`'s rule set is asserted
  explicitly** — whichever of FR12b's two options is chosen — so it can never change unnoticed.
- **AC6a** *(rewritten 2026-08-17)* Given PCFDLRM's suites and fixtures, when reviewed, then **no
  fixture carries `initiationCode: "X"`**. `X` is not a CPP code and does not exist in LIBRA either —
  it was a transcription error for `R` (FR12(2)). An `X` fixture would encode the error into the tests
  and, because `case-details.json` leaves `initiationCode` an unconstrained `string`, would fail late
  at the outbound Progression boundary rather than at the gate.
- **AC6b** *(new 2026-08-17)* Given DD-43081's `libra-schema-impact.csv` and ADR-003 §5, when this
  story completes, then both record LIBRA's set as `O, C, Q, J, R` with **no `X`**. *(Cross-repo;
  recorded here because the CSV is the shared artefact other stories read, and it currently carries
  the error.)*
- **AC7** Given a case whose defendant-level `initiationCode` differs from the case-level one, when it
  is validated, then the documented precedence holds.
- **AC8** Given `getDlrmDefendantValidationRules`, when the story completes, then it is either wired
  and covered by a test, or gone.
- **AC9** Given a LIBRA case omitting each of the **five** fields DD-43081 relaxes — `dateReceived`,
  `receiptType`, `receivingCourt`, `retrialIndicator`, and both of `dateOfCommittal`/`dateOfSending`
  — when PCFDLRM processes it, then it completes with no NPE and no silent default.
  *(Corrected 2026-08-14: `durationMinutes` and `prosecutorOffenceId` are **not** relaxed — LIBRA 0.13
  requires both. See FR16.)*
- **AC9a** Given a LIBRA case with **no materials**, when PCFDLRM processes it, then
  `MigratedCaseFileReceived` is emitted and the case reaches Progression. *(Pins
  `MigratedCaseFileAggregate:368`; fails today.)*
- **AC10** Given every XHIBIT scenario from DD-43099, when the suites run after every change here,
  then all pass unchanged and no XHIBIT fixture is edited.
- **AC11** Given this repo's fixtures and RAML examples, when validated against the post-FR1–FR5
  schemas, then they conform, and the `numPreviousConvictions` drift across 24 fixtures is resolved.
- **AC12** Given `mvn clean install` in this repo, when it completes, then all suites pass with no
  hand-edits to generated sources.
- **AC13** *(recorded answer, not code)* Given FR11, when the story completes, then the story records
  which `vehicleRegistrationMark` home reaches `offenceFacts.vehicleRegistration`, and the answer is
  communicated to the DD-43081 owner.
- **~~AC14~~ REVISED 2026-08-18** *(cross-repo, tracked not fixed here)* — *was:* Given FR19, when
  this story is raised, then the stagingDLRM close-out ticket exists and is linked as blocked by
  DD-43130. **No separate ticket is raised.** Given FR19, when the stagingDLRM close-out PR (version
  bump + Group B mapping + CSV refresh) is opened in `cpp-context-stagingdlrm`, then it references
  DD-43130 directly rather than a new Jira ticket.
- **AC15** *(joint — shared with DD-43081; owned by whichever story lands last)* Given a real LIBRA
  `case.json` + `manifest.json`, when submitted, then it is accepted by stagingDLRM, processed by
  PCFDLRM, forwarded to Progression, and a success outcome is written to Blob Storage. Also requires
  [DD-43086](https://github.com/hmcts/cpp-context-stagingdlrm/tree/main/docs/pipeline/DD-43067-DD-43086-funcapp-libra-ingest)
  and a real LIBRA sample.

## Out of scope

- **The canonical schema, the source-system rule engine, the stagingDLRM rejection path and
  `MigratedCaseConvertor`** — DD-43081.
- **The version bump, Group B converter mapping and impact-CSV refresh in `cpp-context-stagingdlrm`**
  — not built by this story, but *(revised 2026-08-18)* tracked under this same ticket (FR19) rather
  than a separate one — so, unlike the line above, this repo does now expect a PR in
  `cpp-context-stagingdlrm` that cites DD-43130.
- **Group C** — `dxAddress`, `forename3`, `uniquePropertyReferenceNumber`. stagingDLRM accepts and
  drops them; PCFDLRM does not receive them. Note `forename3` **does** have a home here
  (`personal-information.json#givenName3`), so this is a decision, not a limitation.
- **PCFDLRM-side rejection visibility in reconciliation.** DD-43081 found the equivalent hole
  upstream and fixed it; whether PCFDLRM has one has **not been examined**. Flagged for its own
  investigation (brief R3).
- **The 7 fields that reach PCFDLRM and die before Progression** — `backDuty`, `backDutyDateFrom`,
  `backDutyDateTo`, `prosecutorOfferAOCP`, `prosecutorCompensation`/`appliedCompensation`,
  `middleName2`/`givenName3`, officer `forename3`. This story asks whether PCFDLRM is the intended
  terminal consumer; it cannot answer it alone (brief R4).
  **~~Correction (2026-08-18) — the brief's list is now 8, not 7: `vehicleMake` belongs in this set.~~
  FIXED 2026-08-18.** The brief's closing note on R4 said *"`vehicleMake` is **not** in this set — it
  reaches Progression via `offence.offenceFacts`."* That was wrong at the time it was written:
  `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.buildOffenceFacts(...)` built
  `OffenceFacts` from `vehicleCode`, `vehicleRegistration`, and the two alcohol-reading fields only,
  never calling `offence.getVehicleMake()`. **Fixed by TDD, ad hoc, during this story**: a red test
  (`shouldSetVehicleMakeOnOffenceFacts`) proved the drop, then a one-line addition —
  `.withVehicleMake(offence.getVehicleMake())` in `buildOffenceFacts(...)` — made it green. Full
  module (176 tests) and full `mvn clean install` (25/25 modules) both pass. The brief's claim is now
  **true again**, just not for the reason originally given. Pre-existing since this repo's initial
  commit, so this also fixes the same drop for XHIBIT, not only LIBRA — a genuine side benefit of this
  story, not a scope expansion it asked for.
- **plea / verdict / allocationDecision code-vs-UUID** — no resolver exists anywhere in the pipeline
  and it affects XHIBIT equally. Its own ticket.
- **Progression and `cpp.platform.core.domain` changes** — none identified, to be confirmed by AC15
  rather than assumed.
- **`cpp-apitests`**, as for DD-43099. **The Function App and EventGrid path filter** — DD-43086.

## Risks and notes

- **Silent loss, not silent weakening, is the most likely way this story ships broken.** *(Revised
  2026-08-14.)* The original framing — LIBRA validated by a rule set nobody chose — is still true and
  FR13 still guards it. But the audit found **two paths where a LIBRA case loses data with no error
  at all**: `MigratedCaseFileAggregate:368` (no materials → no event → never reaches Progression) and
  the `prosecutorOffenceId` chain (unmatched offence → every hearing dropped from the Progression
  payload). Both share one cause — `isXhibit` guards that leave the non-XHIBIT path with *no*
  behaviour rather than *different* behaviour. Everything compiles, every test passes, XHIBIT is
  unaffected, and LIBRA cases arrive at Progression looking valid and incomplete.
- **~~The officer block shape is the highest-leverage open item.~~ VOID under LIBRA 0.13.1.** FR2a
  and G2 are void — there is no officer block whose shape needs settling. *(Retained as the record of
  why FR2a existed, in case a future LIBRA revision reintroduces the block.)*
- **~~`gender: NOT_KNOWN` is the one synthesised value in this story, and it is deliberate.~~ VOID
  under LIBRA 0.13.1** — FR9a is void; there is no officer `gender` to default. *(Reasoning retained:
  fabricating `MALE`/`FEMALE` would have corrupted migrated data, and dropping the block would have
  made the officer write-only one hop downstream — the problem this story existed to fix. Useful if
  the officer block returns in a future LIBRA revision.)*
- **Coupled blast radius** is the accepted cost of the shared design. NFR1 is the mitigation and must
  stay deliberate: test both source systems on every change to shared validation.
- **~~`vehicleMake` is silently dropped before Progression — pre-existing, not fixed here.~~ FIXED
  2026-08-18.** Found while verifying Group A's "nothing to do" claim: `buildOffenceFacts(...)` never
  read `offence.getVehicleMake()`, despite core having a home for it (`offenceFacts.vehicleMake`) and
  PCFDLRM having declared the field since the initial commit. Pre-existing, affecting XHIBIT and
  LIBRA alike — outside this story's stated Group A scope, but fixed anyway on request, by TDD: a red
  test (`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest#shouldSetVehicleMakeOnOffenceFacts`)
  followed by a one-line addition to `buildOffenceFacts(...)`. **Deliberately not fixed alongside
  it:** the same method's early-return guard doesn't check `vehicleMake` either, so a case with
  *only* `vehicleMake` set (no `vehicleCode`/`vehicleRegistration`/alcohol data) still gets a null
  `offenceFacts` — left alone on the reasoning that a vehicle make with no vehicle identified isn't a
  meaningful case worth guarding against.
- **~~`fax` on a shared closed schema.~~ VOID under LIBRA 0.13.1** — FR3 is void; nothing widens
  `contact-details.json` in this story.
- **~~core's `phone` and `nino` patterns diverge from stagingDLRM's~~ — narrowed.** stagingDLRM allows
  a leading `+` on phone, core does not. This no longer bites here: FR3 (`fax`) is void, and FR6
  (`organisationTelephoneNumber`) was already closed with no field added. No phone-shaped field is
  added by this story.
- **`numPreviousConvictions` → `numberOfPreviousConvictionsCited` is a semantic narrowing.** Confirm
  the meaning before mapping; the name is suggestive, not proof. *(The `prosecutorCompensation` →
  `appliedCompensation` note that used to sit alongside this was about a field outside this story's
  scope and has been removed as noise.)*
- **Two traps if anyone re-derives the Progression column.** (a) The payload PCFDLRM sends is
  `progression.initiate-court-proceedings` carrying `InitiateCourtProceedings = {id, CourtReferral}`,
  so the root is **`courtReferral.json`**, *not* `apiProsecutionCase.json`; core's
  `criminal-court-public-model` holds two parallel families whose reachability closures are disjoint.
  (b) Matching a field by name alone picks whichever schema sorts first — which is how officer
  `forename`/`surname` once resolved to `judicialRole.json` and came out mandatory for the wrong
  reason. Both documented in impact §8.

## Notes for the design stage

1. **Sequencing against DD-43081.** No hard code dependency in either direction; both are gated on
   ADR-003 acceptance. This story can be built and unit-tested against a hand-written payload
   carrying the ADR-003 field set before DD-43081 emits one — that is the point of fixing names in an
   ADR rather than discovering them at integration.
2. **~~FR12 is a conversation before it is code.~~ SETTLED 2026-08-17.** Codes are **`O,C,Q,J,R`**,
   **there is no `X`** (transcription error — architect-confirmed), and `J` validates as CPP SJP.
   No translation layer is needed anywhere. What remains is **two amendments owed to ADR-003** —
   §5's code set and §3's officer nesting — plus a correction to DD-43081's impact CSV; all in
   `cpp-context-stagingdlrm`, all cheaper the sooner they are raised, and none blocking work here.
   **One open item: FR12b's choice between Option A (PCF parity — leave `R` on the fallback) and
   Option B (map `R` alongside `O`).** It turns on whether remitted LIBRA cases reliably carry
   `arrestDate`/`chargeDate`, which the schema cannot answer. Low urgency — the `isXhibit` guards
   discard defendant problems for LIBRA until the deferred guards story lands, so it is a one-line
   correction if the first answer is wrong. **Owner: architect / the LIBRA extract contact.**
2a. **The live-code delta is small, and worth protecting.** Call-site analysis:
   `getCaseValidationRules` has **one** caller (`MigratedCaseFileAggregate:219`, with
   `sourceSystemName` already in scope at `:161`); `getDefendantValidationRules` has **one**
   (`ProsecutionCaseFileHelper:97`, source system already a parameter); `DefendantWithReferenceData`
   has **one** construction site (`Helper:92`). So the whole change is a source-system-keyed map plus
   a LIBRA case-rule list in `CcProsecutionValidationRuleProvider`, two one-line edits in the
   aggregate, and the FR14 deletion — with **no rule class and no serialized type touched**.
   That last point is load-bearing: the three rules to scope for LIBRA (`ReceiptType`,
   `SendingCourt`, `ReceivingCourt` — none of which exist in PCF) take `ProsecutionWithReferenceData`,
   which is carried inside the `MigratedCaseValidatedCreationPending` domain event. Self-guarding the
   rules would mean adding a field to a persisted event payload; keeping dispatch in the provider
   avoids it. `ExhibitFiileTypeValidationRule:32` is the in-repo precedent for the self-guarding
   shape and is *already* inert for LIBRA, so no work is needed there either way.
3. **FR14 must be decided before FR12's design**, not alongside it.
4. **~~FR1 is likely smaller than it looks.~~ VOID under LIBRA 0.13.1** — FR1 is void; there is no
   officer schema to wire.
5. **~~FR9's core constraint is an inherited claim.~~ DISCHARGED 2026-08-14, then VOIDED under
   LIBRA 0.13.1.** FR9 is void — no officer block to build. *(The core-jar verification is retained
   as evidence for the day the officer block returns in a future LIBRA revision.)*
5a. **~~The officer mapping is the only genuinely new converter work in this story.~~ VOID under
   LIBRA 0.13.1** — with FR9 void, the only converter work left is FR10's single rename
   (`numPreviousConvictions` → `numberOfPreviousConvictionsCited`), a straight copy with no re-nest.
6. **~~The three schema seams have three different failure modes~~ — narrowed to one.** Two of the
   three examples are void: the closed-container 4xx (FR1, `officerInCase`) and the shared-schema
   widening (FR3, `fax`). What remains is the open-container silent drop (FR5, `numPreviousConvictions`)
   — still worth its own test, just no longer three distinct failure modes to design around.

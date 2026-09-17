# 00 — Input Brief

- **Epic:** [DD-34850](https://tools.hmcts.net/jira/browse/DD-34850)
- **Story:** [DD-34852](https://tools.hmcts.net/jira/browse/DD-34852)
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)

## Acceptance criteria (DD-34852)

**AC1 — Display allocation decision value and allocation decision date for Charge case**
GIVEN a migrated **Charge** case is created on Common Platform
WHEN the case detail has an allocation decision value and allocation decision date against any
offence on the case
THEN persist this allocation decision value and allocation decision date in CP
AND display both on CAAG as is done now for any other case.

**AC2 — Display allocation decision value and allocation decision date for Summons case**
Same as AC1, for a migrated **Summons** case.

**AC3 — Display allocation decision value and allocation decision date for Postal Charge case**
Same as AC1, for a migrated **Postal Charge** case.

## Working framing

`pcfdlrm` migrates the **allocation decision** (mode-of-trial outcome) for an offence from the
legacy estate and must carry it through to `progression` on the courts `Offence` model, for
persistence and eventual display on CAAG, across (at least) three source case types: Charge,
Summons, and Postal Charge. Direct inspection shows the migrated
`allocationDecision.allocationDecisionDate` is **read from the input but never written to the
outbound courts object** — a silent field drop of the same shape as the indicated-plea gap closed
in DD-34567/DD-34568 — while "allocation decision value" (motReasonId/Code/Description) already
has a mapping in place today, of unconfirmed correctness/completeness. The wider allocation-decision
mapping also has an unresolved code-vs-UUID gap flagged elsewhere as needing its own ticket.

## Where this ask likely comes from

`docs/pipeline/DD-43067-DD-43130-pcfdlrm-schema-enablement/01-requirements.md:647` (open item,
carried from `00-input-brief.md:311`, R5):

> plea / verdict / allocationDecision remain code-vs-UUID — no resolver exists anywhere in the
> pipeline and it affects XHIBIT equally. **Its own ticket.**

DD-34850/DD-34852 is the most plausible candidate for that "own ticket," scoped to the
allocation-decision half of R5 (the slug names "date and value", not plea/verdict).

## Where this happens (from investigation)

Converter: `pcfdlrm-event/pcfdlrm-event-processor/src/main/java/uk/gov/moj/cpp/pcfdlrm/event/processor/convertor/ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.java`

- `buildAllocationDecision(offence, paramsVO)` (line 276) resolves the mode-of-trial reason from
  reference data and populates `offenceId`, `motReasonId`, `motReasonCode`,
  `motReasonDescription`, `sequenceNumber`, `courtIndicatedSentence` — but **never calls
  `.withAllocationDecisionDate(...)`**.
- `MigratedAllocationDecision.getAllocationDecisionDate()` is never called anywhere in the
  codebase (`grep -rn "allocationDecisionDate" --include=*.java` returns zero hits outside
  generated code) — confirmed the value is read from neither the migrated payload nor written
  to the courts payload.
- `retrieveModeOfTrialReason` / `validateAllocationDecisionWhenExist` (lines 255–274) resolve
  `motReasonId` by matching it against `ModeOfTrialReasonsReferenceData` — this is the
  code-vs-UUID resolution R5 says has "no resolver anywhere in the pipeline"; it exists here for
  MOT reason specifically, so R5's claim may be partially stale and needs checking against
  whatever `plea`/`verdict` do instead.

## Key reference facts

- Migrated schema: `pcfdlrm-domain/pcfdlrm-domain-value-schema/src/main/resources/json/schema/migrated/migrated-allocation-decision.json`
  — `originatingHearingId`, `offenceId`, `motReasonId` (required), `motReasonDescription`,
  `motReasonCode`, `allocationDecisionDate`, `courtIndicatedSentence`, `sequenceNumber`.
- Courts schema (`criminal-court-public-model` 17.104.4, `json/schema/global/allocationDecision.json`)
  — same field set; requires `offenceId`, `motReasonId`, `motReasonDescription`, `motReasonCode`,
  `sequenceNumber` (not `allocationDecisionDate`, which is optional on both sides).
- **There is no `allocationDecisionValue` (or similarly named "value") field on either schema.**
  If the real ticket asks for a new field, that's a schema change on top of the date fix, not
  just a converter fix — see open questions.
- Refdata enrichers touching this object: `pcfdlrm-refdata/.../ModeOfTrialRefDataEnricher.java:31-52`,
  `OffenceDataRefDataEnricher.java:104` (`withAllocationDecision(offence.getAllocationDecision())`).

## Additional investigation — case-type scoping (prompted by AC1–AC3)

The three ACs enumerate Charge, Summons, and Postal Charge explicitly, which raised the question
of whether allocation-decision handling is currently gated by case type anywhere. Checked, not
assumed:

- `pcfdlrm-domain/pcfdlrm-domain-aggregate/.../validation/CaseType.java` defines
  `CHARGE("C")`, `REQUISITION("Q")`, `SJP("J")`, `SUMMONS("S")`, `OTHER("O")` — there is no enum
  constant literally named `POSTAL_CHARGE`. **Confirmed (2026-09-17): "Postal Charge" is
  `CaseType.REQUISITION` (initiation code `Q`)** — a naming mismatch between the business term and
  the enum constant, not a missing case type or an unmodelled concept.
- `buildAllocationDecision(...)`, `ModeOfTrialRefDataEnricher`, and `OffenceDataRefDataEnricher`
  contain **no case-type or channel conditional** — the mapping runs unconditionally for every
  offence regardless of case type. Functionally, a fix to `allocationDecisionDate` in the
  converter would apply uniformly to all three case types without per-type branching.
- There is **no existing test coverage** tying allocation decision to case type — no hits for
  `allocationDecision` combined with `caseType`/`CaseType` in
  `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest.java` or
  `ReceiveMigratedCaseFileHelper.java`. Coverage for AC1–AC3 would be new, not extended.

## Open questions to reconcile against the real tickets

1. Is "allocation decision value" in the ACs the `motReasonId`/`motReasonCode`/`motReasonDescription`
   mapping already implemented in `buildAllocationDecision`, and if so, is today's mapping already
   correct/complete, or does this story also need to fix defects in it (not just add the date)?
   The schema comparison rules out a literal, separate `allocationDecisionValue` field — "value"
   most likely names the existing motReason mapping, but needs confirming against what CAAG
   actually renders.
2. Should `allocationDecisionDate` map straight through (`offence.getAllocationDecision().getAllocationDecisionDate()`
   → `.withAllocationDecisionDate(...)`), or does it need a fallback/derivation like
   `convictionDate` has (lines 148–153) when the migrated value is absent?
3. "Persist ... in CP" and "display ... on CAAG" both name systems downstream of the
   `public.pcfdlrm.migrated-case-file-processed` / `progression.initiate-court-proceedings`
   boundary. Confirm this story's scope stops at emitting a correct payload to `progression`
   (as DD-34567/DD-34568 did for indicated plea), with persistence/CAAG display being
   `progression`'s and CAAG's own responsibility, not new work in `pcfdlrm`.
4. Does this story also need to address the plea/verdict half of the R5 code-vs-UUID gap, or is
   that explicitly a separate ticket?

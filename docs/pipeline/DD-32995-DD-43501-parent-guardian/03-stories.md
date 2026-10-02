# 03 — User Stories

- **Story:** [DD-43501](https://tools.hmcts.net/jira/browse/DD-43501) (epic
  [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`), base `team/libra1`
- **Inputs:** `00-input-brief.md`, `01-requirements.md`, `02-design.md` (authoritative where it
  corrects the requirements — Requisition is initiation code `Q`, not `R`; FR-003 wins over
  FR-021 on the surname code), ADR
  [`ADR-DD-43501-libra-defendant-validation-outcomes.md`](../adrs/ADR-DD-43501-libra-defendant-validation-outcomes.md).
- **Jira.** DD-43501 is the single Jira story for this epic slice. No new Jira keys are created
  here and no Jira ticket is created or modified by this document (Jira is SSO-gated and was not
  reached). The slices below (`DD-43501-S1` … `S5`) are delivery sub-units of the one ticket, for
  sequencing PRs and reviews — they are **not** separate Jira items. If the team wants them as
  separate Jira sub-tasks, create those manually from this file.
- **ADR.** One ADR already covers the architectural pattern for every slice below (source-aware
  rule selection, one outcome table, per-defendant rejection collection, existing event
  contract). No further ADR is needed for DD-43501. A slice that deviates from the ADR's pattern
  must say so in its own "Notes" section.

## How to read this file

Each slice is independently reviewable and, where noted, independently releasable: it compiles,
its own tests pass, and `mvn clean install` is green on its own even if a later slice in the
sequence has not landed yet. Only **release** (merge to a branch that ships to an environment
receiving real LIBRA traffic) must respect the blocking note on S4. All slices touch only
`pcfdlrm-domain/pcfdlrm-domain-aggregate` (plus, where stated, `pcfdlrm-integration-test`
fixtures) — no RAML, JSON schema, subscriptions descriptor, public-publications descriptor or
event-listener/processor change anywhere in this story (see `02-design.md` "Contracts").

Example values throughout are synthetic. People referred to in examples use **they/them**.

## Slice summary

| Slice | Title | FRs | Depends on | Size | Status |
|---|---|---|---|---|---|
| DD-43501-S1 | Individual guardian contact & identity sanitising | FR-002 (shape calc), FR-004, FR-005, FR-006, FR-007, FR-008, FR-009 | — | M | Ready |
| DD-43501-S2 | Organisation guardian optional fields | FR-002 (org shape), FR-013, FR-014, FR-016, FR-017 | S1 | S | Ready |
| DD-43501-S3 | Individual guardian mandatory address & case-level rejection | FR-010, FR-011, FR-012, FR-018 | S1 | L | Ready |
| DD-43501-S4 | Organisation guardian mandatory address (reject) | FR-015 | S2, S3 | S | Ready (Q18 resolved: reject) |
| DD-43501-S5 | No-regression audit & problem-code/privacy sign-off | FR-020, FR-021 | S1, S2, S3 (re-run after S4) | S | Ready |

Suggested PR order: **S1 → S2 → S3 → S4 → S5**. Q18 is resolved (REJECT), so S4 is no longer
held. S1 and S2 could in principle be reordered or combined into one PR
since neither introduces rejection, but S1 must land first because S2 reuses its shape
classifier, sanitiser scaffold and redaction convention.

---

## DD-43501-S1 — Individual guardian contact & identity sanitising

### User story
As a **migration operations analyst reconciling LIBRA cases on Common Platform**,
I want **an individual parent/guardian's invalid telephone numbers, email addresses, date of
birth, gender and ethnicity values to be removed or defaulted — with a visible warning — instead
of being kept as-is or silently warned about**,
so that **`progression` never receives a value that failed its own business rule, and a case is
not rejected for a field the DLRM schema marks "Accept"**.

### Background
Today every one of these checks already exists as a rule, but for LIBRA they only raise a warning
— the invalid value is still passed downstream unchanged (`01-requirements.md` "Current
behaviour", items 2–4; `02-design.md` C5). This slice adds the first LIBRA-only rule set, the
guardian shape classifier, the redaction convention and the sanitiser that removes or defaults
the value. It introduces **no rejection** — every outcome in this slice is ACCEPT, NULL-AND-WARN
or DEFAULT-AND-WARN, so it is safe to release on its own.

### Scope (from `02-design.md`)
- `ParentGuardianShape` (enum `INDIVIDUAL` / `ORGANISATION` / `ABSENT`) and `.of(...)`
  classification (FR-002) — the **full** classifier, including organisation detection, even
  though this slice only wires individual-shape rules. `ParentGuardianShapeGate` decorator.
- `LibraParentGuardianScope.applies(channel, migrationSourceSystemName, initiationCode)` — true
  only for `DLRM_MIGRATION` + `LIBRA` + resolved code in `{S, C, Q}` (Requisition is `Q`, per
  `02-design.md` F-1; `R` = Remittance is excluded, Q17).
- `RedactingValidationRule` decorator and the new-rule problem factory (`value = key`, never the
  data) — the privacy convention every later slice must reuse (NFR-001, Q15 default).
- `LibraGenderCode.normalise(String)` — `"0"→NOT_KNOWN`, `"1"→MALE`, `"2"→FEMALE`,
  `"9"→NOT_SPECIFIED` (Q6 default: accept both numeric codes and CP enum names).
- New rules, each gated to `INDIVIDUAL` shape only:
  - `LibraParentGuardianTelephoneValidationRule` (individual mode) — `work`/`home`/`mobile`
    against `Constants.CP_TELEPHONE` (`^[0-9+ \-]{10,}$`).
  - `LibraParentGuardianEmailValidationRule` — `primaryEmail`/`secondaryEmail` against
    `Constants.LIBRA_GUARDIAN_EMAIL` (`^[0-9A-Za-z'._-]{1,127}@[0-9A-Za-z'._-]{1,127}$`, Q3
    default: this regex is LIBRA-guardian-only, defendant email is unchanged).
  - `LibraParentGuardianGenderValidationRule` — DEFAULT `NOT_KNOWN` via `LibraGenderCode`,
    individual shape only (Q13: never runs for an organisation guardian).
  - Reused, wrapped in `RedactingValidationRule`: `ParentGuardianDateOfBirthValidationRule`,
    `ParentGuardianObservedEthnicityValidationAndEnricherRule`,
    `ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule`.
- `LibraParentGuardianOutcomes` — the single code→outcome table (NULL/DEFAULT entries only in
  this slice; REJECT entries arrive in S3/S4).
- `LibraParentGuardianSanitiser.sanitise(...)` — gender mapping (individual only) plus nulling for
  every NULL-outcome field this slice owns.
- Provider: `CcProsecutionValidationRuleProvider` 4-arg `getDefendantValidationRules(code,
  channel, isGroupCase, sourceSystemName)` (3-arg overload delegates with `null`, unchanged) and
  `defendantValidationMapDlrmLibra` for `S`/`C`/`Q`, built by filtering `COMMON_DEFENDANT_RULE_SET`
  (remove the five generic guardian rules, replace with the gated set above) — `SPI_DEFENDANT_RULE_SET`
  and the per-code set are **not** touched in this slice (individual guardian postcode stays on
  the existing warn-only `PostCodeValidationRule`, unchanged until S3).
- `ProsecutionCaseFileHelper`: `validateDefendants(...)` returning `DefendantValidationOutcome`
  (empty rejection list in this slice); `validateDefendantErrors(...)` delegates unchanged;
  `validateGenderAndLanguage` gets the `checkParentGuardianGender` flag, `false` for any
  LIBRA-in-scope defendant (both individual and organisation — this is why AC-020, below, is
  satisfied by this slice even though organisation rules ship in S2).
- New `ProblemCode`, `FieldName`, `Constants` entries for telephones, emails (reused codes),
  gender (reused code), ethnicities (reused codes).

### Dependencies / ordering
None. First slice. S2, S3 and S4 all depend on the shape classifier, sanitiser scaffold and
redaction convention introduced here.

### Acceptance criteria
Traceability: FR-002 (AC-004, AC-005, AC-020), FR-004 (AC-008–010), FR-005 (AC-011–014), FR-006
(AC-015, AC-016), FR-007 (AC-017–020), FR-008/FR-009 (AC-021–023).

- AC-S1-001 (FR-002 / AC-004): Given a LIBRA Charge defendant with no `parentGuardianInformation`,
  when the case is received, then no guardian problem is raised — in particular no
  `PARENT_GUARDIAN_GENDER_INVALID` — and the case is accepted unchanged.
- AC-S1-002 (FR-004 / AC-008–010): Given an individual guardian with `work` =
  `+44 20 7946 0000`, `home` = `012345678` (9 characters) and `mobile` = `07700-ABC-123`, when the
  LIBRA Summons case is received, then `work` is kept, `home` and `mobile` are each removed, one
  warning is raised per removed number (`PARENT_GUARDIAN_HOME_TELEPHONE_INVALID`,
  `PARENT_GUARDIAN_MOBILE_TELEPHONE_INVALID`), and the case is accepted.
- AC-S1-003 (FR-005 / AC-011–014): Given `primaryEmail` = `taylor.parent@example.org` (valid) and
  `secondaryEmail` = `first+tag@example.org` (contains `+`, invalid under the ticket regex), when
  validated, then `primaryEmail` is kept, `secondaryEmail` is removed with
  `DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID`, and the case is accepted. A
  127-character local part is kept; a 128-character local part is removed with a warning.
- AC-S1-004 (FR-006 / AC-015, AC-016): Given a guardian `dateOfBirth` of tomorrow
  (Europe/London), when validated, then `dateOfBirth` is removed with
  `DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE` and the case is accepted; a `dateOfBirth` of
  today is kept.
- AC-S1-005 (FR-007 / AC-017–019): Given individual guardian `gender` values `0`, `1`, `2`, `9`,
  absent, and `X`, when validated, then the downstream gender is `NOT_KNOWN`, `MALE`, `FEMALE`,
  `NOT_SPECIFIED`, `NOT_KNOWN` and `NOT_KNOWN` respectively; the numeric and absent/invalid cases
  each carry a `PARENT_GUARDIAN_GENDER_INVALID` warning except the four valid numeric mappings
  (`0/1/2/9`), which carry none (Q6 default). The case is always accepted — gender never rejects.
- AC-S1-006 (FR-007, Q13 / AC-020): Given an organisation guardian (`organisationName` set, no
  individual fields), when validated, then no gender problem is raised and no gender is added or
  read, even though organisation-shape field rules do not ship until S2.
- AC-S1-007 (FR-008/FR-009 / AC-021–023): Given `observedEthnicity` not found in CP Observed
  Ethnicity reference data, when validated, then it is removed with
  `DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID` and the case is accepted; same pattern
  for `selfDefinedEthnicity` against CP Ethnicity reference data.
- AC-S1-008 (NFR-001 / Q15): Given any of the problems above, when the resulting
  `MigratedCaseValidatedWithWarnings` / `DefendantValidationFailed` is inspected, then its
  `ProblemValue.value` equals the field key (for example
  `individual_parentGuardianInformation_personalInformation_contactDetails_home`), never the
  offending phone number, email, date or ethnicity code.
- AC-S1-009 (FR-020 / regression): Given an XHIBIT case or a LIBRA SJP/`R`/`O` case with any of
  the above conditions, when received, then behaviour is byte-for-byte as it is today (warning
  only, value kept, generic guardian-gender check still runs).

### NFR links
- NFR-001 (Privacy): guardian values never appear in `ProblemValue`/warning/event text — see
  AC-S1-008.
- NFR-003 (Determinism): date-of-birth check uses the existing Europe/London clock only.
- NFR-004 (Performance): `CP_TELEPHONE` and `LIBRA_GUARDIAN_EMAIL` are bounded, non-backtracking
  regexes; no new reference-data calls beyond the existing ethnicity lookups.
- NFR-005 (Testability): one unit test class per new rule, covering absent/valid/invalid/boundary
  (9 vs 10 characters, 127 vs 128 local-part characters, today vs tomorrow, each gender code).

### Definition of done
- [ ] All ACs above covered by unit tests in `pcfdlrm-domain-aggregate/src/test/.../validation/rules/defendant/parentguardian/`.
- [ ] `CcProsecutionValidationRuleProviderTest` updated: LIBRA S/C/Q lists assert (COMMON minus
      five guardian rules) + gated individual rules; existing pinned assertions for XHIBIT, SPI,
      MCC, CIVIL and LIBRA `J`/`R`/`O` (including the existing `R`-fallback test) pass unmodified.
- [ ] `ProsecutionCaseFileHelperTest` covers the sanitised output and the organisation
      gender-skip (AC-S1-006), and confirms the existing XHIBIT tests are untouched.
- [ ] No wildcard imports (CLAUDE.md).
- [ ] `mvn clean install` green for the whole repo.
- [ ] No RAML, JSON schema, subscriptions-descriptor or public-publications-descriptor change.
- [ ] Code reviewed and approved.

### Out of scope for this slice
- Individual `address1`/`address2`–`address5`/`postcode` (S3).
- Any organisation-shape field rule (S2, S4).
- Any case rejection (S3, S4).

### Notes / open questions
- Q3 (email regex scope), Q6 (gender codes), Q13 (skip org gender), Q15 (redact values) are all
  implemented here per their proposed defaults in `01-requirements.md`; none blocks this slice.
- Q17 (new, from `02-design.md`): Requisition is `Q`; `R` (Remittance) is deliberately excluded
  from scope. Flagged for business confirmation but does not block this slice — reversible by
  adding `R` to the scope predicate's code set later.

---

## DD-43501-S2 — Organisation guardian optional fields

### User story
As a **migration operations analyst**,
I want **an organisation guardian's invalid company telephone number, address lines and postcode
to be nulled with a warning, and `organisationName` to keep today's schema-only behaviour**,
so that **organisation-guardian cases are sanitised to the same standard as individual-guardian
cases, with no youth-defendant special case (resolved, Q2) and no spurious gender problem**.

### Background
No rule exists today for any organisation-shape field except the JSON schema's `maxLength`
caps on `organisationName` and `companyTelephoneNumber` (`01-requirements.md` item 2). This slice
reuses S1's shape classifier and sanitiser scaffold and adds the organisation-shape rules. It
introduces no rejection, so — like S1 — it is independently releasable.

### Scope (from `02-design.md`)
- `LibraParentGuardianTelephoneValidationRule` (organisation mode) — `companyTelephoneNumber`
  against `Constants.CP_TELEPHONE`, gated to `ORGANISATION` shape.
- `LibraOrganisationParentGuardianAddressValidationRule` — **this slice ships only its
  `address2`–`address5` (NULL, over 35 characters) and `postcode` (NULL, not matching
  `Constants.POST_CODE_REGEX`) checks.** Its `address1` check is added in S4 (blocked) — see the
  implementation note below.
- `LibraParentGuardianOutcomes` gains the NULL entries for
  `PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID`,
  `PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID`,
  `INVALID_GUARDIAN_ORGANISATION_POST_CODE`.
- `LibraParentGuardianSanitiser` extended to null these organisation fields. (Gender is already
  skipped for organisation guardians by S1's scope-wide flag — nothing further to do here.)
- `organisationName` (FR-013): **no new rule.** Confirm by test that the existing
  `parent-guardian-information.json` `maxLength: 255` behaviour is unchanged and that no
  youth-defendant condition is introduced anywhere (Q2 resolved).
- New `ProblemCode`/`FieldName`/`Constants` entries for company telephone, organisation address
  lines and organisation postcode.

### Dependencies / ordering
Depends on S1 (`ParentGuardianShape`, `ParentGuardianShapeGate`, `RedactingValidationRule`
convention, `LibraParentGuardianOutcomes`, `LibraParentGuardianSanitiser`, the LIBRA provider
map). Independent of S3 — can land before or after it, but must land before S4 (S4 extends the
same address rule class this slice creates).

### Acceptance criteria
Traceability: FR-013 (AC-031–033), FR-014 (AC-034, AC-035), FR-016 (AC-038), FR-017 (AC-039).

- AC-S2-001 (FR-013 / AC-031): Given an organisation guardian with no `organisationName` (and,
  once S4 ships, a valid `address1`), when the LIBRA case is received, then the case is accepted
  and no `organisationName` problem is raised, whatever the defendant's age (there is no youth
  condition, Q2).
- AC-S2-002 (FR-013 / AC-032, AC-033): Given `organisationName` of 255 characters, when
  validated, then it is kept; given 256 characters, when the command is received, then it fails
  JSON schema validation exactly as today, and no FR-018 rejection event is emitted (this slice
  adds no business rule for this field).
- AC-S2-003 (FR-014 / AC-034, AC-035): Given `companyTelephoneNumber` = `0161 496 0000`, when
  validated, then it is kept; given `companyTelephoneNumber` = `ext 22`, when validated, then it
  is removed with `PARENT_GUARDIAN_COMPANY_TELEPHONE_INVALID` and the case is accepted.
- AC-S2-004 (FR-016 / AC-038, subject to Q7): Given organisation `address5` of 36 characters,
  when validated, then it is removed with `PARENT_GUARDIAN_ORGANISATION_ADDRESS_LINE_INVALID` and
  the case is accepted.
- AC-S2-005 (FR-017 / AC-039): Given an organisation guardian with postcode
  `NOT A POSTCODE`, when received, then the postcode is removed with
  `INVALID_GUARDIAN_ORGANISATION_POST_CODE`, a warning is raised, and the case is accepted
  (unlike the individual postcode in S3, this never rejects — Q1 asymmetry, confirmed by design).
- AC-S2-006 (NFR-001): Given any problem above, then its `ProblemValue.value` is the field key,
  never the phone number, address line or postcode.
- AC-S2-007 (FR-020 / regression): Given an XHIBIT case with the same organisation field
  conditions, when received, then behaviour is unchanged (no organisation-field rule exists for
  XHIBIT today, and this slice does not add one).

### NFR links
- NFR-001 (Privacy): as S1.
- NFR-004 (Performance): reuses `Constants.CP_TELEPHONE`; no new reference-data calls.
- NFR-005 (Testability): one unit test class per new/extended rule.

### Definition of done
- [ ] Unit tests for all ACs above, including the organisation equivalents of the S1 boundary
      cases (length, blank).
- [ ] `CcProsecutionValidationRuleProviderTest` and `ProsecutionCaseFileHelperTest` extended for
      the organisation path; existing XHIBIT/SPI/MCC/CIVIL pinned tests unchanged.
- [ ] No wildcard imports (CLAUDE.md).
- [ ] `mvn clean install` green.
- [ ] Code reviewed and approved.

### Out of scope for this slice
- Organisation `address1` mandatoriness / rejection (FR-015 — S4).
- Any individual-shape field (S1, S3).

### Notes / open questions
- **Implementation note on sequencing within one class.** `02-design.md` models organisation
  address1/address2–5/postcode as one rule class
  (`LibraOrganisationParentGuardianAddressValidationRule`). This slice ships that class with only
  the address-lines and postcode checks active; S4 adds the `address1` branch to the same class
  rather than creating a new one. Reviewers of this slice's PR should expect that class to still
  be extended, not a design deviation.
- Q2 is resolved (no youth condition) and does not block this slice.

---

## DD-43501-S3 — Individual guardian mandatory address & case-level rejection

### User story
As a **migration operations analyst**,
I want **a LIBRA case to be rejected — not silently accepted — when an individual guardian's
address is missing or their postcode is invalid**,
so that **a case Common Platform cannot safely proceed with stops at `pcfdlrm` for correction,
instead of reaching `progression` with an incomplete or wrong guardian address**.

### Background
No LIBRA defendant-level rejection path exists today — every defendant problem that is not an
offence problem becomes a warning and the case is accepted (`01-requirements.md` item 6). This is
the first slice that changes that invariant, so it is the highest-risk slice in this story: it can
flip existing LIBRA S/C/Q test fixtures (and, in principle, in-flight real cases) from "accepted
with a warning" to "rejected". The fixture updates below **must** ship in the same PR as the rule
change, or CI goes red.

### Scope (from `02-design.md`)
- `DefendantValidationOutcome` (new hand-written value class): the existing
  `MigratedDefendantWithProblem` plus `List<Problem> libraGuardianRejections`, in defendant order.
  `validateDefendants(...)` returns it; `validateDefendantErrors(...)` keeps delegating.
- `LibraIndividualParentGuardianAddressValidationRule`:
  - `address1` (`personalInformation.address.address1`) missing (no `personalInformation`, no
    `address` object), blank, or over 35 characters → **REJECT**
    (`PARENT_GUARDIAN_ADDRESS1_MISSING_OR_INVALID`, FR-010). The 3-character no-fixed-abode code
    (`NFA`, case-insensitive, Q5 default) is valid and is **not** rejected.
  - `address2`–`address5` over 35 characters → NULL-AND-WARN
    (`PARENT_GUARDIAN_ADDRESS_LINE_INVALID`, FR-011, subject to Q7).
  - `postcode` non-blank and not matching `Constants.POST_CODE_REGEX` or
    `Constants.NO_FIXED_ABODE_POST_CODE` (`^[zZ][zZ]99 ?[0-9][a-zA-Z]{2}$`, Q14 default) →
    **REJECT** (`INVALID_GUARDIAN_POST_CODE`, FR-012). An absent or blank postcode is ACCEPT (Q1
    default: absent individual postcode never rejects).
- `PostCodeValidationRule(boolean validateParentGuardianPostCode)` new constructor; no-arg
  constructor stays `true` (unchanged for XHIBIT/SPI/MCC/LIBRA `J`/`R`/`O`). LIBRA S/C/Q map
  replaces it with `new PostCodeValidationRule(false)` so the generic rule stops warning on the
  guardian postcode — the new rule above owns that outcome exclusively for LIBRA S/C/Q, which
  also fixes the "same code is a warning in one scope and a rejection in another" hazard the ADR
  calls out.
- `LibraParentGuardianOutcomes` gains the two REJECT entries above (address1, postcode) and the
  NULL entry for address lines 2–5.
- `MigratedCaseFileAggregate.hasLibraParentGuardianRejections(outcome, ...)`, inserted after the
  XHIBIT-gated `hasOffenceProblems` and before the XHIBIT case warnings (FR-018). On a non-empty
  `libraGuardianRejections` list: emit one `MigratedCaseFileProcessed(processingIsSuccessful=false)`
  with `caseId`, `caseUrn`, `submissionId` and description
  `"Parent guardian validation failed: " + <distinct codes, defendant then rule order>`; no
  `MigratedCaseFileReceived` / `MigratedCaseValidatedCreationPending` / `MaterialAdded` /
  `MigratedCaseValidatedWithWarnings` is emitted for that case.
- **Description constraint.** The prefix must not contain, and must not be contained in, any of
  `stagingdlrm`'s `stagingContextErrors` markers (`JSON_SCHEMA`, `DUPLICATE_SUBMISSION_ID`,
  `CASE_ALREADY_EXISTS_IN_PROGRESSION`, `VALIDATION_FAILED`) — add a unit test that pins the
  prefix text.
- **Existing-fixture updates (must ship in this PR):**
  - `pcfdlrm-domain/pcfdlrm-domain-aggregate/src/test/resources/json/aggregate/migrated-case-file-received-no-materials-libra.json`
    — guardian has no `personalInformation`; if its scenario's initiation code is S/C/Q it will
    now reject. Add a guardian `address` (with `address1`) so it keeps exercising the
    sanitised-accept path it was written for.
  - `pcfdlrm-integration-test/src/test/resources/command-json/pcfdlrm.command.receive-multiple-hearing-migrated-case-file.json`,
    `pcfdlrm.command.receive-multiple-hearing-wc-migrated-case-file.json` and
    `pcfdlrm.command.receive-with-no-hearing-migrated-case-file.json` — all LIBRA `Q` cases with
    an individual guardian that has no address, used by `ReceiveMigratedCaseFileIT` (the first two
    at the multi-hearing test, the no-hearing one also by the no-material test). Add a guardian
    `address` to each. This brings the fixtures in line with what the real upstream feed sends
    (address is required there) and changes no other expected output.
  - The three LIBRA `J` (SJP) fixtures are out of scope for this change and are unaffected
    (AC-002).

### Dependencies / ordering
Depends on S1 (shape classifier, redaction convention, sanitiser scaffold, provider map).
Independent of S2. Must land before S4, which extends the same
`DefendantValidationOutcome`/`hasLibraParentGuardianRejections` mechanism for the organisation
address.

### Acceptance criteria
Traceability: FR-001 (AC-001–003), FR-010/FR-011 (AC-024–027), FR-012 (AC-028–030), FR-018
(AC-040, AC-041).

- AC-S3-001 (FR-001 / AC-001): Given an XHIBIT case whose guardian has an invalid individual
  postcode, when received, then it is accepted with an `INVALID_GUARDIAN_POST_CODE` warning
  exactly as today, and no FR-018 rejection is emitted.
- AC-S3-002 (FR-001 / AC-002): Given a LIBRA case with initiation code `J` (SJP) and a guardian
  missing `address1`, when received, then no guardian REJECT is applied.
- AC-S3-003 (FR-001 / AC-003): Given LIBRA cases with initiation codes `S`, `C` and `Q`, each with
  a guardian missing `address1`, when each is received, then each is rejected.
- AC-S3-004 (FR-010 / AC-024): Given an individual guardian with `personalInformation` but no
  `address` object, when the LIBRA case is received, then the case is rejected with a reason
  identifying the missing guardian `address1`.
- AC-S3-005 (FR-010 / AC-025): Given an individual guardian with `address1` = `NFA` (Q5), when
  validated, then `address1` is accepted and no problem is raised.
- AC-S3-006 (FR-010 / AC-026): Given an individual guardian with `address1` = `"   "` (whitespace
  only), when received, then the case is rejected.
- AC-S3-007 (FR-011 / AC-027, subject to Q7): Given `address3` of 36 characters, when validated,
  then it is removed with `PARENT_GUARDIAN_ADDRESS_LINE_INVALID` and the case is accepted.
- AC-S3-008 (FR-012 / AC-028–030): Given an individual guardian with no postcode, when validated,
  then no postcode problem is raised and the case is accepted (Q1). Given postcode `SW1A 1AA`, it
  is kept. Given postcode `NOT A POSTCODE`, the case is rejected identifying
  `INVALID_GUARDIAN_POST_CODE`. Given postcode `ZZ99 9ZZ` (no-fixed-abode, Q14), it is kept and
  the case is accepted.
- AC-S3-009 (FR-018 / AC-040, AC-041): Given a LIBRA case with two defendants — defendant 1 has a
  valid guardian, defendant 2's guardian has an invalid postcode and no `address1` — when
  received, then exactly one `MigratedCaseFileProcessed(processingIsSuccessful=false)` is emitted,
  its description lists both reasons (deterministic order), no
  `MigratedCaseFileReceived`/`MigratedCaseValidatedCreationPending` is emitted, and
  `public.pcfdlrm.migrated-case-file-processed` carries the same `caseId`, `caseUrn` and
  `submissionId`.
- AC-S3-010 (FR-019 / AC-042, AC-043, cumulative with S1/S2): Given a LIBRA case whose guardian
  has an invalid `home` number, an invalid `primaryEmail`, a future `dateOfBirth` and no `gender`,
  and nothing that triggers REJECT, when received, then the downstream defendant has no `home`,
  `primaryEmail` or `dateOfBirth`, has gender `NOT_KNOWN`, exactly four guardian warnings are
  raised, and every other guardian and defendant field is byte-for-byte unchanged.
- AC-S3-011 (fixture regression): Given the three updated IT fixtures and the updated aggregate
  fixture, when the existing IT and unit suites are run, then they pass unmodified in their
  expected outcome (still accepted), because each now carries a guardian address.

### NFR links
- NFR-002 (Contract compatibility): confirmed no schema/event-shape change — only new
  `description` text on an existing event.
- NFR-003 (Determinism): rejection reason ordering is `LinkedHashSet` in defendant-then-rule
  order, pinned by test.
- NFR-006 (Observability): a rejected case is traceable by `caseUrn` and problem code from the
  emitted events alone (AC-S3-009).
- NFR-005 (Testability): includes IT coverage (reject journey and sanitised-accept journey), not
  just unit tests.

### Definition of done
- [ ] All ACs above covered by unit tests (`PostCodeValidationRule`,
      `LibraIndividualParentGuardianAddressValidationRule`, `MigratedCaseFileAggregateTest`,
      `ProsecutionCaseFileHelperTest`).
- [ ] `MigratedCaseFileAggregateTest` pins the description prefix and checks it against the
      `stagingdlrm` marker strings (listed above).
- [ ] New `ReceiveMigratedCaseFileIT` fixture pair for the reject journey (assert
      `processingIsSuccessful=false` and the description via the existing `verifyCaseProcessed`).
- [ ] New `ReceiveMigratedCaseFileIT` fixture pair for the sanitised-accept journey (assert the
      guardian payload reaching `initiate-court-proceedings`).
- [ ] The three existing IT fixtures and the aggregate fixture listed above are updated with a
      guardian address in the **same PR**, and `ReceiveMigratedCaseFileIT` still passes.
- [ ] `./runIntegrationTests.sh` green (full `pcfdlrm-integration-test` module).
- [ ] No wildcard imports (CLAUDE.md).
- [ ] `mvn clean install` green.
- [ ] Code reviewed and approved.

### Out of scope for this slice
- Organisation guardian `address1` rejection (FR-015 — S4, blocked).
- Any change to `cpp-context-stagingdlrm` or `progression` — including anything to make a
  `pcfdlrm` JSON-schema failure (HTTP 400) visible as a failed-migration outcome to migration
  operations (F-8, Q20; separate `stagingdlrm` story if wanted).

### Notes / open questions
- Q1 (postcode asymmetry — absent individual postcode accepts, present-invalid rejects) and Q14
  (no-fixed-abode postcode allowed) are implemented per their proposed defaults; neither blocks
  this slice, but both should be confirmed with the DLRM business analyst before the behaviour is
  relied on in production reconciliation.
- Q5 (what makes `address1` invalid, and the no-fixed-abode code spelling) is implemented as
  "blank or over 35 characters invalid; `NFA` case-insensitive valid" — a proposed default, not
  confirmed.
- Q7 (does a length breach count as "invalid" for fields the schema does not cap) is implemented
  as "yes" for address lines 2–5 and postcode length; not confirmed.

---

## DD-43501-S4 — Organisation guardian mandatory address (reject)

> **Q18 resolved 2026-10-02 by the requester: reject the case.** This slice is no longer blocked.
> Because of F-2 (below), every LIBRA case with an organisation guardian will be rejected until
> the LIBRA schema can carry an organisation address. Tell migration operations before release.

### User story
As a **migration operations analyst**,
I want **a LIBRA case to be rejected when an organisation guardian's address is missing or
invalid, in the same way an individual guardian's address is**,
so that **the parent/guardian rule set treats both guardian shapes consistently, per the DD-43501
ticket's table**.

### Background — F-2 and the Q18 decision
`02-design.md` finding F-2: the LIBRA Function App's own schema for the organisation-guardian
shape (`libra.case-submission.json` `definitions.parentGuardianInformation.oneOf[1]`) has only
`organisationName` (required) and `companyTelephoneNumber`, with `additionalProperties: false`.
**It has no `address` field at all.** Every LIBRA organisation guardian therefore arrives at
`stagingdlrm` — and so at `pcfdlrm` — with no address, by construction of the upstream schema.

If this slice is implemented exactly as the ticket specifies (organisation `address1` missing →
REJECT) and released, **every LIBRA case with an organisation guardian would be rejected**,
because the upstream feed structurally cannot supply the address FR-015 requires. The requester
confirmed on 2026-10-02 that this is the intended outcome (Q18). It remains the highest operational
impact item in this story (`02-design.md` Risk 1).

### Scope (from `02-design.md`, as specified)
- Extend `LibraOrganisationParentGuardianAddressValidationRule` (created in S2) with the
  `address1` branch: missing (no `address` object), blank, or over 35 characters →
  **REJECT** (`PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID`, FR-015).
- `LibraParentGuardianOutcomes` gains this REJECT entry.
- No other change — `hasLibraParentGuardianRejections` (S3) already collects REJECT problems
  regardless of which rule raised them.

### Dependencies / ordering
Depends on S2 (the organisation address rule class exists) and S3 (the case-level rejection
mechanism, `DefendantValidationOutcome`, exists). Build after both.

### Acceptance criteria
Traceability: FR-015 (AC-036–037a).

- AC-S4-001 (FR-015 / AC-036): Given an organisation guardian with no `address` object, when
  received, then the case is rejected with a reason identifying the missing organisation
  `address1`.
- AC-S4-002 (FR-015 / AC-037): Given an organisation guardian with `address1` blank, when
  received, then the case is rejected.
- AC-S4-003 (FR-015 / AC-037a): Given an organisation guardian whose `address` object has no
  `address1` property, when the command is received, then it fails JSON schema validation exactly
  as it does today (`pcf-address.json` `required: address1`), and no FR-018 rejection event is
  emitted — this path is unreachable from the real LIBRA feed per F-2, but must stay true for
  direct API callers.
- AC-S4-004 (Q18, confirmed): Given a LIBRA organisation-guardian case shaped exactly as the
  real upstream feed sends it (no `address` at all), when received, then the case is rejected with
  `PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID`. The requester confirmed this outcome
  on 2026-10-02.

### NFR links
- NFR-002 (Contract compatibility): unchanged, as S3.
- NFR-006 (Observability): the rejection is identifiable by
  `PARENT_GUARDIAN_ORGANISATION_ADDRESS1_MISSING_OR_INVALID` alone.

### Definition of done
- [ ] Unit tests for AC-S4-001 to AC-S4-003.
- [ ] `MigratedCaseFileAggregateTest` case combining an individual REJECT and an organisation
      REJECT on different defendants of the same case (both reasons in one description, as
      AC-S3-009 but with a mixed-shape pair).
- [ ] PR description records the Q18 decision (REJECT, 2026-10-02) and its consequence for
      LIBRA organisation-guardian cases (F-2).
- [ ] Migration operations told, before release, that LIBRA cases with organisation guardians
      will be rejected.
- [ ] No wildcard imports (CLAUDE.md).
- [ ] `mvn clean install` green.
- [ ] Code reviewed and approved.

### Out of scope for this slice
- Adding an `address` field to the LIBRA Function App's organisation-guardian schema — that is a
  separate `cpp-context-stagingdlrm` story (one repo per story, CLAUDE.md).

### Notes / open questions
- **Q18 resolved (2026-10-02): REJECT.** Path (a) was chosen: LIBRA cases with organisation
  guardians are rejected until the LIBRA schema carries an organisation address.
- Follow-up (not part of DD-43501): a `cpp-context-stagingdlrm` story to add organisation
  `address` to `libra.case-submission.json`, so these cases can pass. Changing the outcome later is
  still a one-entry change in `LibraParentGuardianOutcomes`.

---

## DD-43501-S5 — No-regression audit & problem-code / privacy sign-off

### User story
As the **`pcfdlrm` delivery team closing out DD-43501**,
I want **a consolidated regression run and an audit of every new problem code, field name and
redaction point introduced across S1–S4**,
so that **the team can confirm FR-020 (no regression) and FR-021 (problem-code identifiability)
hold across the whole story before it is marked done, not just within each slice**.

### Background
Each of S1–S4 pins its own regression tests, but the ADR and design flag cross-cutting risks that
are easiest to verify once the whole rule set is in place: scope leakage into XHIBIT/SPI/MCC, a
problem code missing from the outcome table, and a stray raw value in an event. This slice is the
single place that checks the whole story, not a single field.

### Scope
- Run the full unit and integration suite (`mvn clean install`, then `./runIntegrationTests.sh`)
  with S1–S3 (and S4 if unblocked) all present, and confirm no expectation changed for any
  XHIBIT, SJP or non-DLRM scenario (FR-020).
- Audit `LibraParentGuardianOutcomes`: every `ProblemCode` introduced by this story is classified
  exactly once (REJECT / NULL / DEFAULT), and every NULL code has a nulling function registered
  (FR-021).
- Audit every new rule's problem factory and every `RedactingValidationRule` wrap: no
  `ProblemValue.value` in any LIBRA guardian problem equals offending input data (grep-style test
  across all new rule test classes) — closing out NFR-001 for the whole story, not slice by
  slice.
- Confirm the JSON-schema-failure behaviour recorded in `02-design.md` ("Current behaviour when
  the receive payload fails JSON schema validation") is still accurate after S1–S4: AC-007,
  AC-026a, AC-033, AC-037a still produce HTTP 400 with no `pcfdlrm` event, and are unreachable
  from the real LIBRA feed but true for direct API callers (F-8).
- Confirm `CcProsecutionValidationRuleProviderTest` still pins XHIBIT, SPI, MCC, CIVIL and LIBRA
  `J`/`R`/`O` rule-class lists unchanged, across the final combined rule-set code (FR-020).
- Re-run (or extend) this slice once S4 ships, so the audit covers the organisation-reject path
  too.

### Dependencies / ordering
Depends on S1, S2 and S3 at minimum. Should be re-run (not re-created) after S4 lands, since S4
adds another REJECT entry to the same outcome table this slice audits.

### Acceptance criteria
Traceability: FR-020 (AC-044), FR-021 (AC-045), NFR-001, NFR-004.

- AC-S5-001 (FR-020 / AC-044): Given the existing unit and integration test suites, when run
  after S1–S3 (and S4, once landed), then all pass with no expectation changes for XHIBIT or
  non-guardian scenarios.
- AC-S5-002 (FR-021 / AC-045): Given each failure introduced in AC-S1-002 (telephone), AC-S3-004
  (individual address1), AC-S3-008 (individual postcode), AC-S2-003 (company telephone), AC-S4-001
  (organisation address1, once landed) and AC-S2-005 (organisation postcode), when raised, then
  each carries its own problem code and a field name that identifies the guardian field.
- AC-S5-003 (NFR-004 / regex safety): Given the three new `Constants` regexes
  (`CP_TELEPHONE`, `LIBRA_GUARDIAN_EMAIL`, `NO_FIXED_ABODE_POST_CODE`), when reviewed, then each
  uses only bounded or simple quantifiers with no nested quantifiers that could backtrack
  catastrophically.
- AC-S5-004 (NFR-001, consolidated): Given every new `ProblemCode` from S1–S4, when its
  corresponding problem is raised in a test, then its `ProblemValue.value` is asserted to equal
  the field key, not the input.
- AC-S5-005 (F-8 recorded behaviour, AC-007/AC-026a/AC-033/AC-037a): Given a surname of 36
  characters, an `address` object with no `address1`, an `organisationName` of 256 characters, and
  an organisation `address` with no `address1`, when each command is received directly (bypassing
  the LIBRA Function App), then each returns HTTP 400 and no `pcfdlrm` event is stored or
  published.

### NFR links
- NFR-001, NFR-004, NFR-005, NFR-006 — consolidated, cross-slice.

### Definition of done
- [ ] Full `mvn clean install` and `./runIntegrationTests.sh` green with every merged slice
      present.
- [ ] `LibraParentGuardianOutcomes` table has one test asserting every `ProblemCode` this story
      introduces is present and classified.
- [ ] A single test (or small suite) asserting no new `ProblemValue` carries raw guardian data.
- [ ] `CcProsecutionValidationRuleProviderTest` reviewed end-to-end against `02-design.md`'s
      component table for drift.
- [ ] Residual-privacy items from `02-design.md` Cross-cutting (`DefendantValidationFailed`
      storing the unsanitised defendant; `MigratedCaseValidatedCreationPending` storing the raw
      `Prosecution`) are **not** fixed here — recorded as out of scope and flagged to the DPO, per
      design.
- [ ] Code reviewed and approved; story-level sign-off recorded against DD-43501.

### Out of scope for this slice
- Fixing the residual-privacy items above (existing behaviour for every DLRM source, a possible
  DLRM-wide follow-up story).
- The three follow-up stories `02-design.md` identifies for `stagingdlrm` (organisation guardian
  address in the LIBRA schema, F-2/Q18; guardian observed-ethnicity mapping, F-4/Q19; an outcome
  for `pcfdlrm` 400s, F-8/Q20) — each is a separate story in a separate repo (CLAUDE.md "one repo
  per story").

### Notes / open questions
- Q19 (guardian observed ethnicity is sent by LIBRA at `personalInformation.observedEthnicity`,
  but FR-008 and the processor both read the top-level `observedEthnicity`, which LIBRA never
  populates) is **not** fixed by this story — FR-008 is implemented literally (top-level) per
  `02-design.md` F-4. Carried forward as a follow-up story, most likely in `stagingdlrm` or as a
  mapping fix. Flag this explicitly in sign-off so the gap is visible, not silently inherited.
- Q20 (whether migration operations need a reported outcome for a `pcfdlrm` JSON-schema 400) is
  out of scope; AC-S5-005 only confirms the current behaviour is unchanged, not that it is
  sufficient.

---

## Cross-story open questions carried forward (not resolved by any slice)

| # | Question | Where it bites | Status |
|---|---|---|---|
| Q1 | Individual postcode asymmetry (absent accepts, present-invalid rejects; organisation invalid nulls) | S3, S2 | Default implemented, not confirmed |
| Q3 | LIBRA-guardian-only email regex vs defendant email | S1 | Default implemented, not confirmed |
| Q4 | A guardian block whose populated fields are all blank counts as absent | S1 (shape classifier) | Default implemented, not confirmed |
| Q5 | What makes `address1` invalid; no-fixed-abode code spelling (`NFA`) | S3 | Default implemented, not confirmed |
| Q6 | LIBRA gender code form; `9` → `NOT_SPECIFIED` | S1 | Default implemented, not confirmed |
| Q7 | Does a length breach count as "invalid" for fields the schema does not cap | S2, S3 | Default implemented, not confirmed |
| Q9 | Rejection description wording for downstream reporting | S3 | Default implemented (description text), not confirmed |
| Q10 | Warn on every nulled/defaulted field | S1, S2, S3 | Default implemented, not confirmed |
| Q14 | No-fixed-abode postcode (`ZZ99 9ZZ`) allowed | S3 | Default implemented, not confirmed |
| Q15 | Redact values in events (field key only, no data) | S1 | Default implemented |
| Q17 | Requisition is `Q`; `R` (Remittance) excluded from scope | S1 (scope predicate) | Default implemented, not confirmed |
| Q18 | Organisation `address1` REJECT vs the LIBRA feed's inability to carry an organisation address at all | S4 | Resolved 2026-10-02: REJECT |
| Q19 | Guardian observed-ethnicity field-path mismatch between LIBRA and `pcfdlrm`/processor | S5 (flagged, not fixed) | Out of scope, follow-up story |
| Q20 | Reported outcome for `pcfdlrm` JSON-schema 400s | S5 (flagged, not fixed) | Out of scope, follow-up story |

None of Q1, Q3–Q7, Q9, Q10, Q14, Q15 or Q17 blocks design or release of the slice that implements
it — each is shippable on its stated default and correctable later by a small, localised change
(mostly a single entry in `LibraParentGuardianOutcomes` or `Constants`). Q18 is resolved
(REJECT), so no open question blocks release.

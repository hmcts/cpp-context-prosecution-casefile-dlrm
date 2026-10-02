# 01 — Requirements

- **Story:** [DD-43501](https://tools.hmcts.net/jira/browse/DD-43501) (epic
  [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`), base `team/libra1` @ `f5c3f4d`
- **Status:** Draft. The design-blocking questions (Q2, Q11) were resolved by the requester on
  2026-10-02. The other open questions proceed on their stated defaults, pending business
  confirmation. Stage 2 (design) can start.

> **Provenance caveat.** Jira and Confluence sit behind SSO and could not be read. AC 1 and the
> field tables in `00-input-brief.md` are the requester's verbatim paste of the DD-43501 ticket.
> Every FR below traces to one of those rows (see [Traceability](#traceability)). **Before
> sign-off, check against the live ticket and against _DLRM – CP Migration Data Schema
> V0.13.xlsx_.** All "current behaviour" statements come from reading the code on `team/libra1`
> and give source locations.

## Context

LIBRA cases migrated through DLRM must pass business validation before `pcfdlrm` accepts them
and hands them to `progression`. This story adds the rules for the **parent / guardian** block of
each defendant (`defendant.individual.parentGuardianInformation`). Each rule ends in one of the
outcomes below. Today no LIBRA defendant-level problem can reject a case, and nothing is nulled
for LIBRA.

## Actors

| Actor | Description |
|-------|-------------|
| LIBRA migration feed (`stagingdlrm`) | Upstream system. Sends `pcfdlrm.command.receive-migrated-case-file` with `migrationSourceSystemName` = LIBRA. Not changed by this story. |
| `pcfdlrm` | This context. Validates the guardian block, sanitises or rejects, and emits domain and public events. |
| `progression` | Downstream consumer of `public.pcfdlrm.migrated-case-file-processed` and of the accepted case. Must not receive invalid guardian values. Not changed by this story. |
| Migration operations / reconciliation team | Read rejection descriptions and warnings to fix or re-submit cases. They need each rejection and each nulled field to be identifiable. |

## Outcome vocabulary

Each FR below uses these outcome names.

| Outcome | Meaning |
|---|---|
| **ACCEPT** | The value is valid or absent. It is kept as sent and no problem is raised. |
| **NULL-AND-WARN** | The value is present but breaks its rule. It is removed from the defendant passed downstream, a `MigratedCaseValidatedWithWarnings` (type "Defendant validation") is raised, and the case is still accepted. This is the AC's "nulled … follow the required behaviour for missing fields" where the missing behaviour is Accept. |
| **DEFAULT-AND-WARN** | The value is missing or invalid and is replaced by a defined default. A warning is raised and the case is accepted. |
| **REJECT** | The **whole case** is rejected (FR-018). No defendant on the case goes to `progression`. |

## Applicability

The rules apply only when **all** of these are true (FR-001):

- channel = `DLRM_MIGRATION`
- `migrationSourceSystemName` = `LIBRA` (Q8)
- case or defendant initiation code is Summons (`S`), Charge (`C`) or Requisition (`Q`). SJP is N/A on every ticket row. (`R` is Remittance, which is out of scope; see `02-design.md` F-1 / Q17.)
- the defendant has a `parentGuardianInformation` block (Q4)

## Current behaviour (verified in code)

Paths are relative to `pcfdlrm-domain/pcfdlrm-domain-aggregate/src/main/java/uk/gov/moj/cpp/pcfdlrm/`
unless stated otherwise.

1. **Rule selection.** `validation/provider/CcProsecutionValidationRuleProvider.getDefendantValidationRules`
   sends `DLRM_MIGRATION` to `defendantValidationMapDlrm`, which is the same object as
   `defendantValidationMapSpi`. It has keys for `S`, `C`, `R`, `O` and `SJP`, so **LIBRA Summons
   defendants do reach defendant validation**. Case-level `caseValidationMapLibra` has no `S` key,
   but `getCaseValidationRules` uses `getOrDefault(..., COMMON_CASE_RULE_SET_LIBRA)`, so Summons
   falls back to the common LIBRA case set. This answers the code half of Q8.
2. **Existing guardian rules** (in `COMMON_DEFENDANT_RULE_SET`, plus `PostCodeValidationRule` in
   `SPI_DEFENDANT_RULE_SET`):
   `ParentGuardianDateOfBirthValidationRule` (future date, Europe/London clock),
   `ParentGuardianObservedEthnicityValidationAndEnricherRule`,
   `ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule`,
   `ParentGuardian{Primary,Secondary}EmailAddressValidationRule` (`validation/Constants.EMAIL`,
   an RFC-style regex that needs a dotted domain and allows `+`), and `PostCodeValidationRule`
   (`INVALID_GUARDIAN_POST_CODE`, individual shape only, using `Constants.POST_CODE_REGEX`).
   There are no rules for the telephones, surname, `address1`, `organisationName`,
   `companyTelephoneNumber`, the organisation address or the organisation postcode.
3. **Gender.** `ProsecutionCaseFileHelper.validateGenderAndLanguage` raises
   `PARENT_GUARDIAN_GENDER_INVALID` when a guardian block exists and `gender` is null or is not
   a `uk.gov.justice.core.courts.Gender` enum name (case-insensitive). So **missing** gender and the
   numeric codes `0/1/2/9` are all flagged. **Correction to the brief:** the `NOT_KNOWN` default
   (`applyDefendantAndParentGenderRule`) runs only inside `applyRuleToDefendantFields`, which is
   gated on `"XHIBIT".equals(migrationSourceSystemName)` (`ProsecutionCaseFileHelper` ~line 118).
   **For LIBRA, guardian gender is warned on but never defaulted.**
4. **Organisation guardian and gender.** The organisation shape has no `gender`, so the same
   check raises `PARENT_GUARDIAN_GENDER_INVALID` for every organisation guardian today. The
   generated `ParentGuardianInformation` class flattens both `oneOf` shapes into one class.
5. **Nulling.** The only field sanitising in `pcfdlrm` is `applyRuleToDefendantFields`. It is
   XHIBIT-only and covers defendant (not guardian) nationality, ethnicity, custody status,
   languages and gender. **Nothing is nulled or defaulted for LIBRA.**
6. **Outcome handling.** `aggregate/MigratedCaseFileAggregate` turns every defendant problem that
   is not in `offenceProblems` into a `MigratedCaseValidatedWithWarnings` and accepts the case.
   Defendant-driven rejection (`hasOffenceProblems`) is gated on `isXhibit(...)`. **No LIBRA
   defendant-level rejection path exists.** Existing rejections emit `MigratedCaseFileProcessed`
   with `processingIsSuccessful=false` and a `description`.
7. **JSON schema constraints**, applied before business rules
   (`pcfdlrm-domain/pcfdlrm-domain-value-schema/src/main/resources/json/schema/`):
   - `parent-guardian-information.json` is a `oneOf` of the individual and organisation shapes,
     both with `additionalProperties: false`. A block that mixes fields from both shapes matches
     neither. An empty object `{}` matches both and therefore fails `oneOf`.
   - `pcf-address.json` (id `.../address.json`) has `required: ["address1"]`, so an `address`
     object without `address1` fails the schema. An absent `address` object passes.
   - `personal-information.json` has `required: ["lastName"]` with `maxLength: 35`. So
     **surname is effectively mandatory whenever `personalInformation` is present**, which
     conflicts with the ticket's O. Q11 resolved: the schema is kept and decides this.
   - Other max lengths: `contact-details.json` work/home/mobile 35 and primaryEmail/secondaryEmail
     255. Organisation shape: `organisationName` 255 and `companyTelephoneNumber` 35. `address1`–`5`
     and `postcode` have **no** `maxLength`.
8. **Postcode regex and no-fixed-abode.** `Constants.POST_CODE_REGEX` does **not** match the
   conventional CJS no-fixed-abode postcode `ZZ99 9ZZ`, because `Z` is not allowed as the second
   letter (Q14).
9. **Youth status.** No youth or juvenile derivation exists anywhere in `pcfdlrm` `src/main`. None is needed: Q2 is resolved, and `organisationName` has no youth condition.

## Functional requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-001 | Apply FR-002 to FR-019 only when the [applicability](#applicability) conditions hold. XHIBIT, SJP and non-DLRM channels keep their current behaviour (FR-020). | Must |
| FR-002 | Classify the guardian block as **individual** (has any of `dateOfBirth`, `gender`, `personalInformation`, `selfDefinedEthnicity`, `observedEthnicity`), **organisation** (has any of `organisationName`, `companyTelephoneNumber`, `address`) or **absent**. If absent, no guardian rule runs and no guardian problem is raised. Treat blank strings as absent (Q4). | Must |
| FR-003 | **Surname** (`personalInformation.lastName`), A35. Follows `personal-information.json` unchanged (Q11, resolved). With no `personalInformation` the surname is absent → ACCEPT. When `personalInformation` is present, `lastName` is `required` with `maxLength: 35`, so a missing or over-length surname fails JSON schema validation as it does today. No new business rule. | Should |
| FR-004 | **Individual telephones** (`personalInformation.contactDetails.work`, `.home`, `.mobile`), each validated on its own against `^[0-9+\ \-]{10,}$` (10 or more characters, each a digit, `+`, `-` or space). Absent → ACCEPT. Present and non-matching → NULL-AND-WARN for that number only. | Must |
| FR-005 | **Individual emails** (`personalInformation.contactDetails.primaryEmail`, `.secondaryEmail`), each validated on its own against the ticket rule: 1–127 characters from `0-9 A-Z a-z ' . - _`, then `@`, then 1–127 characters from the same set (Q3). Absent → ACCEPT. Present and non-matching → NULL-AND-WARN for that address only. | Must |
| FR-006 | **Guardian date of birth** (`dateOfBirth`), D10. Absent → ACCEPT. Later than today (Europe/London) → NULL-AND-WARN. No default is applied unless Q12 says otherwise. | Must |
| FR-007 | **Guardian gender** (`gender`), N1, mandatory for an individual guardian. Map LIBRA codes `0`→`NOT_KNOWN`, `1`→`MALE`, `2`→`FEMALE`, `9`→`NOT_SPECIFIED` (Q6). Also accept valid CP enum names (case-insensitive) unchanged. Absent, blank or unmappable → DEFAULT-AND-WARN with `NOT_KNOWN`. Never reject. Do not run this rule for an organisation guardian (Q13). | Must |
| FR-008 | **Observed ethnicity** (`observedEthnicity`), A1, optional, checked against CP Observed Ethnicity reference data. Absent → ACCEPT. Present and not found → NULL-AND-WARN. | Must |
| FR-009 | **Self-defined ethnicity** (`selfDefinedEthnicity`), A2, optional, checked against CP Ethnicity reference data ("16+1"). Absent → ACCEPT. Present and not found → NULL-AND-WARN. | Must |
| FR-010 | **Individual `address1`** (`personalInformation.address.address1`), A35, mandatory when the guardian is an individual. An `address` object without `address1` fails `pcf-address.json` (`required: address1`) as today (Q11, resolved). The schema cannot catch the remaining cases, so a business rule REJECTs them: no `personalInformation`, no `address` object, a blank `address1`, or an invalid one (Q5). The 3-character no-fixed-abode code is valid (Q5). | Must |
| FR-011 | **Individual `address2`–`address5`**, A35, optional. Absent → ACCEPT. Present and invalid (over 35 characters, Q7) → NULL-AND-WARN for that line. | Should |
| FR-012 | **Individual postcode** (`personalInformation.address.postcode`), A8, optional. Absent or blank → ACCEPT. Present and not matching `Constants.POST_CODE_REGEX` → REJECT (Q1, Q14). | Must |
| FR-013 | **`organisationName`**, A255. Validated as `parent-guardian-information.json` defines it: the property is supported on the organisation shape, is not `required`, and has `maxLength: 255`. No youth-defendant condition applies, because an organisation guardian is a legal entity (Q2, resolved). Absent or blank → ACCEPT. Over 255 characters → the existing JSON schema failure, unchanged. No new business rule, and never a FR-018 rejection. | Must |
| FR-014 | **`companyTelephoneNumber`**, A35, optional, same regex as FR-004. Absent → ACCEPT. Present and non-matching → NULL-AND-WARN. | Must |
| FR-015 | **Organisation `address1`** (`address.address1`), A35, mandatory when the guardian is an organisation. An `address` object without `address1` fails `pcf-address.json` as today (Q11, resolved). A business rule REJECTs the cases the schema cannot catch: no `address` object, a blank `address1`, or an invalid one (Q5, Q12). | Must |
| FR-016 | **Organisation `address2`–`address5`**, A35, optional. Absent → ACCEPT. Present and invalid (Q7) → NULL-AND-WARN for that line. | Should |
| FR-017 | **Organisation postcode** (`address.postcode`), A8, optional. Absent → ACCEPT. Present and not matching `Constants.POST_CODE_REGEX` → NULL-AND-WARN. The case is accepted (unlike FR-012, Q1). | Must |
| FR-018 | **Rejection.** When any defendant on a LIBRA case hits a REJECT outcome (FR-010, FR-012, FR-015), the case is not accepted. `pcfdlrm` emits `MigratedCaseFileProcessed` with `processingIsSuccessful=false`, carrying `caseId`, `caseUrn`, `submissionId` and a description naming every guardian rejection reason found across all defendants (Q9). This is published as `public.pcfdlrm.migrated-case-file-processed`. Nothing is sent on to `progression` for that case. | Must |
| FR-019 | **Sanitised acceptance.** When no REJECT outcome applies, the accepted defendant passed downstream (on `MigratedCaseFileReceived` / `MigratedCaseValidatedCreationPending`) has every NULL-AND-WARN field removed and every DEFAULT-AND-WARN field replaced. One warning is raised per affected field, naming the problem code and the field (Q10, Q15). | Must |
| FR-020 | **No regression.** Behaviour stays unchanged for XHIBIT cases, SJP, non-DLRM channels, defendants without a guardian block, and all non-guardian defendant, offence, hearing, case and material rules. | Must |
| FR-021 | **Problem codes.** Each guardian failure has its own `ProblemCode` and field name. Existing codes (`DEFENDANT_PARENT_GUARDIAN_*`, `PARENT_GUARDIAN_GENDER_INVALID`, `INVALID_GUARDIAN_POST_CODE`) are reused where they already describe the failure. New codes cover the telephones, surname, address lines, organisation fields and organisation postcode. | Should |

## Non-functional requirements

| ID | Category | Requirement | Threshold |
|----|----------|-------------|-----------|
| NFR-001 | Security / privacy | Guardian values (names, phone numbers, emails, addresses, dates of birth) are never written to application logs. Today warnings and `defendantValidationFailed` copy the offending value into events. Whether that continues for guardian fields is Q15. | Zero guardian values in logs |
| NFR-002 | Contract compatibility | No breaking change to `public.pcfdlrm.migrated-case-file-processed` or to the `receive-migrated-case-file` command schema. Any new event or field is additive, with both the subscription and the JSON schema updated together (CLAUDE.md "Critical gotcha"). | No consumer change needed in `progression` |
| NFR-003 | Determinism | Outcomes depend only on the payload, reference data and the Europe/London date at receive time. Replaying stored events must not re-run validation or change a recorded outcome. | Same input + same date → same outcome |
| NFR-004 | Performance | Add no reference-data calls beyond the existing ethnicity (and any gender) lookups. Regex checks are linear-time patterns (no catastrophic backtracking). | No measurable change to receive-command latency |
| NFR-005 | Testability | Each FR has unit tests covering absent, valid, invalid and boundary cases (for example, a 9- vs 10-character phone number, a 127- vs 128-character email local part, a date of today vs tomorrow). Both the reject journey and the sanitised-accept journey are covered at aggregate level (`MigratedCaseFileAggregateTest`). No new ITs: the requester asked for unit-level coverage only, and the processor that republishes the outcome is unchanged. | 100% of FR-003 to FR-019 covered by unit tests |
| NFR-006 | Observability | A rejected or sanitised case can be traced by `caseUrn` and problem code from the emitted events alone. | Every REJECT and every NULL/DEFAULT is visible in events |
| NFR-007 | Accessibility | N/A. No user interface. | — |
| NFR-008 | Data retention | Unchanged. No new data store. Event-store retention follows the existing `pcfdlrm` policy. | — |

## Acceptance criteria

Example values are synthetic. "LIBRA case" means DLRM_MIGRATION channel, `migrationSourceSystemName`
= LIBRA and initiation code S, C or Q, unless stated otherwise.

### FR-001 — Applicability
- AC-001: Given an XHIBIT case whose guardian has an invalid individual postcode, when it is received, then it is accepted with an `INVALID_GUARDIAN_POST_CODE` warning, exactly as today, and no FR-018 rejection is emitted.
- AC-002: Given a LIBRA case with initiation code SJP and a guardian missing `address1`, when it is received, then no guardian REJECT is applied.
- AC-003: Given LIBRA cases with initiation codes S, C and Q, each with a guardian missing `address1`, when each is received, then each is rejected (FR-018).

### FR-002 — Guardian presence
- AC-004: Given a LIBRA defendant with no `parentGuardianInformation`, when validated, then no guardian problem is raised, and in particular no `PARENT_GUARDIAN_GENDER_INVALID`.
- AC-005: Given a LIBRA defendant whose guardian has only `organisationName` and `address`, when validated, then only the organisation rules (FR-013 to FR-017) run.

### FR-003 — Surname
- AC-006: Given a guardian `lastName` of 35 characters, when validated, then it is kept and no problem is raised.
- AC-007: Given a guardian with `personalInformation` whose `lastName` is missing or 36 characters long, when the command is received, then it fails JSON schema validation exactly as it does today, and no FR-018 rejection event is emitted.

### FR-004 — Individual telephones
- AC-008: Given `work` = `+44 20 7946 0000`, when validated, then it is kept.
- AC-009: Given `home` = `012345678` (9 characters), when validated, then `home` is removed, one warning is raised for `home`, and the case is accepted.
- AC-010: Given `mobile` = `07700-ABC-123`, when validated, then `mobile` is removed and a warning is raised, while a valid `work` on the same guardian is kept.

### FR-005 — Individual emails
- AC-011: Given `primaryEmail` = `parent.guardian@example.org`, when validated, then it is kept.
- AC-012: Given `primaryEmail` = `first+tag@example.org`, when validated, then it is removed with a `DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID` warning and the case is accepted.
- AC-013: Given `secondaryEmail` = `nobody` (no `@`), when validated, then it is removed with a `DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID` warning, and a valid `primaryEmail` is kept.
- AC-014: Given an email whose local part is 128 characters, when validated, then it is removed with a warning. With a 127-character local part it is kept.

### FR-006 — Date of birth
- AC-015: Given a guardian `dateOfBirth` of today (Europe/London), when validated, then it is kept.
- AC-016: Given a guardian `dateOfBirth` of tomorrow, when validated, then `dateOfBirth` is removed, a `DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE` warning is raised, and the case is accepted.

### FR-007 — Gender
- AC-017: Given guardian `gender` values `0`, `1`, `2` and `9`, when validated, then the downstream gender is `NOT_KNOWN`, `MALE`, `FEMALE` and `NOT_SPECIFIED` respectively, and no warning is raised (Q6).
- AC-018: Given an individual guardian with no `gender`, when validated, then the downstream gender is `NOT_KNOWN`, a `PARENT_GUARDIAN_GENDER_INVALID` warning is raised, and the case is accepted.
- AC-019: Given guardian `gender` = `X`, when validated, then the downstream gender is `NOT_KNOWN`, a warning is raised, and the case is accepted.
- AC-020: Given an organisation guardian, when validated, then no gender problem is raised and no gender is added.

### FR-008 / FR-009 — Ethnicity
- AC-021: Given a guardian `observedEthnicity` found in CP Observed Ethnicity reference data, when validated, then it is kept.
- AC-022: Given a guardian `observedEthnicity` not found in reference data, when validated, then it is removed with a `DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID` warning and the case is accepted.
- AC-023: Given a guardian `selfDefinedEthnicity` not found in CP Ethnicity reference data, when validated, then it is removed with a `DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID` warning and the case is accepted.

### FR-010 / FR-011 — Individual address
- AC-024: Given an individual guardian with `personalInformation` but no `address` object, when the LIBRA case is received, then the case is rejected (FR-018) with a reason identifying the missing guardian `address1`.
- AC-025: Given an individual guardian with `address1` = the no-fixed-abode code (Q5), when validated, then `address1` is accepted and no problem is raised.
- AC-026: Given an individual guardian with `address1` blank (`""` or whitespace only), when received, then the case is rejected.
- AC-026a: Given an individual guardian whose `address` object has no `address1` property, when the command is received, then it fails JSON schema validation exactly as it does today, and no FR-018 rejection event is emitted.
- AC-027: Given (subject to Q7) `address3` of 36 characters, when validated, then `address3` is removed with a warning and the case is accepted.

### FR-012 — Individual postcode
- AC-028: Given an individual guardian with no postcode, when validated, then no postcode problem is raised and the case is accepted (Q1).
- AC-029: Given an individual guardian postcode `SW1A 1AA`, when validated, then it is kept.
- AC-030: Given an individual guardian postcode `NOT A POSTCODE`, when the LIBRA case is received, then the case is rejected with a reason identifying `INVALID_GUARDIAN_POST_CODE`.

### FR-013 — Organisation name
- AC-031: Given an organisation guardian with a valid `address1` and no `organisationName`, when the LIBRA case is received, then the case is accepted and no `organisationName` problem is raised. This holds whatever the defendant's age.
- AC-032: Given an organisation guardian with an `organisationName` of 255 characters, when validated, then it is kept.
- AC-033: Given an organisation guardian with an `organisationName` of 256 characters, when the command is received, then it fails JSON schema validation exactly as it does today, and no FR-018 rejection event is emitted.

### FR-014 — Company telephone
- AC-034: Given `companyTelephoneNumber` = `0161 496 0000`, when validated, then it is kept.
- AC-035: Given `companyTelephoneNumber` = `ext 22`, when validated, then it is removed with a warning and the case is accepted.

### FR-015 / FR-016 — Organisation address
- AC-036: Given an organisation guardian with no `address` object, when received, then the case is rejected with a reason identifying the missing organisation `address1`.
- AC-037: Given an organisation guardian with `address1` blank, when received, then the case is rejected.
- AC-037a: Given an organisation guardian whose `address` object has no `address1` property, when the command is received, then it fails JSON schema validation exactly as it does today, and no FR-018 rejection event is emitted.
- AC-038: Given (subject to Q7) organisation `address5` of 36 characters, when validated, then `address5` is removed with a warning and the case is accepted.

### FR-017 — Organisation postcode
- AC-039: Given an organisation guardian with postcode `NOT A POSTCODE` and a valid `address1`, when received, then the postcode is removed, a warning is raised, and the case is accepted.

### FR-018 — Rejection
- AC-040: Given a LIBRA case with two defendants, where defendant 1 has a valid guardian and defendant 2's guardian has an invalid postcode and no `address1`, when received, then exactly one `MigratedCaseFileProcessed` with `processingIsSuccessful=false` is emitted, its description lists both reasons, and no `MigratedCaseFileReceived` or `MigratedCaseValidatedCreationPending` is emitted.
- AC-041: Given the rejection in AC-040, when the processor runs, then `public.pcfdlrm.migrated-case-file-processed` is published with `processingIsSuccessful=false` and the same `caseId`, `caseUrn` and `submissionId`.

### FR-019 — Sanitised acceptance
- AC-042: Given a LIBRA case whose guardian has an invalid `home` number, an invalid `primaryEmail`, a future `dateOfBirth` and no `gender`, and nothing that triggers REJECT, when received, then the downstream defendant has no `home`, `primaryEmail` or `dateOfBirth`, has gender `NOT_KNOWN`, and exactly four guardian warnings are raised.
- AC-043: Given the case in AC-042, when accepted, then all other guardian and defendant fields are byte-for-byte unchanged.

### FR-020 — No regression
- AC-044: Given the existing unit and integration test suites, when run after the change, then all pass with no expectation changes for XHIBIT or non-guardian scenarios.

### FR-021 — Problem codes
- AC-045: Given each failure in AC-009, AC-024, AC-030, AC-035, AC-036 and AC-039, when raised, then each carries its own problem code and a field name that identifies the guardian field.

## Traceability

| Ticket row (00-input-brief.md) | Ticket outcome | FR | ACs |
|---|---|---|---|
| AC 1 scope (DLRM LIBRA; Sum/Chg/Req; SJP N/A) | — | FR-001, FR-020 | AC-001–003, AC-044 |
| AC 1 "null invalid, then apply missing behaviour" | — | FR-019 | AC-042, AC-043 |
| AC 1 "reject the case … per table" | — | FR-018 | AC-040, AC-041 |
| `parentGuardian.surname` | Ticket: accept. Resolved: follow the schema, which requires it inside `personalInformation` (Q11) | FR-003 | AC-006, AC-007 |
| `parentGuardian.work/home/mobileTelephoneNumber` | Accept | FR-004 | AC-008–010 |
| `parentGuardian.primaryEmail/secondaryEmail` | Accept | FR-005 | AC-011–014 |
| `parentGuardian.dateOfBirth` | Accept | FR-006 | AC-015, AC-016 |
| `parentGuardian.gender` | Accept (default) | FR-007 | AC-017–020 |
| `parentGuardian.observedEthnicity` | Accept | FR-008 | AC-021, AC-022 |
| `parentGuardian.selfDefinedEthnicity` | Accept | FR-009 | AC-023 |
| `parentGuardian.address1` | Reject (schema or business rule, Q11) | FR-010 | AC-024–026a |
| `parentGuardian.address2…5` | Accept | FR-011 | AC-027 |
| `parentGuardian.postcode` | Reject | FR-012 | AC-028–030 |
| `organisationName` | Ticket: reject if youth, else accept. Resolved: follow the schema, no youth condition (Q2) | FR-013 | AC-031–033 |
| `companyTelephoneNumber` | Accept | FR-014 | AC-034, AC-035 |
| `organisation.address1` | Reject (schema or business rule, Q11) | FR-015 | AC-036–037a |
| `organisation.address2…5` | Accept | FR-016 | AC-038 |
| `organisation.postcode` | Accept | FR-017 | AC-039 |
| (derived) guardian presence and shape | — | FR-002 | AC-004, AC-005 |
| (derived) problem-code identifiability | — | FR-021 | AC-045 |

## Constraints

- **CJS Data Standards** §3.36, §3.37, §3.39, §3.45 and §3.87, and the CP telephone standard (the ticket says it replaces E-GIF/CJS), as cited in the ticket.
- **Data Protection Act 2018 / UK GDPR.** Guardian data is personal data about people who are not parties to the case, so minimise it in logs and events (NFR-001).
- **CPP framework contract rules.** Any event or schema change updates the subscriptions descriptor, the JSON schema and, for public events, `public-publications-descriptor.yaml` together.
- **One repo per story.** No change to `cpp-context-stagingdlrm` or `progression` (CLAUDE.md).
- **JSON schema runs first.** Payloads that fail `parent-guardian-information.json`, `pcf-address.json` or `personal-information.json` never reach the business rules (current behaviour, item 7). Q11 resolved: these schemas stay unchanged, and the schema check takes precedence over the ticket's outcome where they overlap.

## Out of scope

- Non-guardian defendant fields, offence, hearing, case-level and material rules (other DD-32995 stories).
- XHIBIT guardian behaviour, beyond keeping it unchanged.
- SJP cases.
- Any change in `cpp-context-stagingdlrm`, `progression` or `listing`.
- Changing the shared defendant (non-guardian) email regex (see Q3).
- Re-validating cases already migrated.

## Open questions

Every "Proposed default" is an **assumption pending business confirmation**. Defaults are
written into the FRs so the work can move forward, but none is decided. **Blocks design** means
Stage 2 cannot settle the approach until it is answered.

| # | Question | Proposed default (assumption) | Blocks design? | Owner | Due |
|---|---|---|---|---|---|
| 1 | The individual postcode is optional but its outcome is Reject, while the organisation postcode is Accept. Is that asymmetry intended, and does an **absent** individual postcode reject? | Absent → accept. Present and invalid → reject (individual). Organisation invalid → null and warn. | No | DLRM business analyst (TBD) | TBD |
| 2 | How is a **youth** defendant defined for `organisationName`? Which date of birth field (`individual.personalInformation` / `selfDefinedInformation`), what threshold (under 18?), as at which date (charge date, first hearing date, migration date)? What if the defendant's date of birth is missing? | **Resolved:** an organisation guardian is a legal entity, so the youth-defendant condition does not apply. `organisationName` follows `parent-guardian-information.json` (supported, optional, maxLength 255). See FR-013. | No (resolved) | Requester | 2026-10-02 |
| 3 | The email regex: the ticket allows `' . - _` and no `+`, and does not need a dotted domain. The current `Constants.EMAIL` is RFC-style. Should the ticket regex apply to LIBRA guardian emails only, or also to defendant emails? | Ticket regex for LIBRA guardian emails only. Defendant emails unchanged (out of scope). | No | DLRM business analyst (TBD) | TBD |
| 4 | Does "mandatory" apply only when a guardian block is present? Is a block holding only blank strings present? | Yes, only when present. A block whose populated fields are all blank counts as absent. | No | TBD | TBD |
| 5 | What makes `address1` **invalid** (blank, over 35 characters, anything else)? What exactly is the 3-character no-fixed-abode code (for example `NFA`), and is it matched case-insensitively? | Invalid = blank or over 35 characters. `NFA` (case-insensitive) is valid. | No | TBD | TBD |
| 6 | Does LIBRA send gender as numeric `0/1/2/9` or as CP enum names? Does `9` map to `NOT_SPECIFIED` or default to `NOT_KNOWN`? | Accept both forms. `9` → `NOT_SPECIFIED`. | No | TBD | TBD |
| 7 | Does breaching a format length (A35/A255/A8/N1/D10) make a value "invalid" for business rules? The schema caps some fields only (lastName 35, phones 35, emails 255, organisationName 255, companyTelephoneNumber 35) and not address lines or postcode. | Yes. Fields with no schema cap get a business-rule length check (null and warn, or reject for `address1`). Schema-capped fields keep schema behaviour (see Q11). | No | TBD | TBD |
| 8 | Is this LIBRA only, with XHIBIT unchanged? Code shows LIBRA Summons does reach both case and defendant validation (current behaviour, item 1). | LIBRA only. XHIBIT unchanged. | No | TBD | TBD |
| 9 | How is a rejection reported? Do downstream systems or reports need specific description text or a structured reason list? | Reuse `MigratedCaseFileProcessed` (`processingIsSuccessful=false`) → `public.pcfdlrm.migrated-case-file-processed`, with a description listing problem codes. | No (wording can be settled before build) | TBD | TBD |
| 10 | When an optional field is nulled, should a warning still be raised so the data loss is visible? | Yes, one warning per nulled or defaulted field. | No | TBD | TBD |
| 11 | **Schema versus business rule.** `personal-information.json` requires `lastName` and `pcf-address.json` requires `address1`, so some payloads fail JSON schema validation before any business rule runs. | **Resolved:** follow the schema. No change to `parent-guardian-information.json`, `personal-information.json` or `pcf-address.json`. Where the schema already enforces a constraint (required `lastName`, required `address1` inside an `address` object, `maxLength` caps), the existing schema failure is the outcome. Business rules (FR-010, FR-015, FR-018) cover only what the schema does not catch. How a schema failure surfaces to migration operations is existing behaviour, to be recorded in design rather than changed. | No (resolved) | Requester | 2026-10-02 |
| 12 | The ticket says "may need a default" for guardian `dateOfBirth` and organisation `address1`, yet organisation `address1` is Reject. Is there a default value for either? | No defaults. Date of birth is nulled. Organisation `address1` rejects. | No | TBD | TBD |
| 13 | The organisation shape has no gender, but today every organisation guardian raises `PARENT_GUARDIAN_GENDER_INVALID` (current behaviour, item 4). Should gender rules be skipped for organisation guardians (LIBRA)? Is fixing this for XHIBIT in scope? | Skip for organisation guardians on LIBRA. XHIBIT unchanged (separate defect if wanted). | No | TBD | TBD |
| 14 | `Constants.POST_CODE_REGEX` rejects `ZZ99 9ZZ`, the conventional no-fixed-abode postcode. With FR-012 = Reject, a no-fixed-abode guardian could reject the case. Should no-fixed-abode postcodes be allowed? | Allow `ZZ99 9ZZ` (and other `ZZ99` forms) as valid for the guardian postcode. | No | TBD | TBD |
| 15 | Today warnings and `defendantValidationFailed` copy the invalid value (for example a full email or phone number) into events. Should guardian warnings include the offending value, or only the field name and problem code? | Field name and problem code only. No value. | No | Information assurance / DPO (TBD) | TBD |
| 16 | An empty guardian object `{}`, or one mixing individual and organisation fields, fails the `oneOf` schema. Is that the intended outcome? | Yes, keep schema behaviour. Not changed by this story. | No | TBD | TBD |

# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995)
- **Story:** [DD-43501](https://tools.hmcts.net/jira/browse/DD-43501)
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Branch:** `dev/dd-43501` (to be created off `team/libra1`)

## Epic framing (DD-32995)

LIBRA cases migrated to Common Platform through DLRM have to pass business validation before
`pcfdlrm` accepts them and sends them on to `progression`. Each field in the migration schema
has a rule, and a breach of that rule either **rejects the whole case** or **nulls the invalid
value and accepts the case**. This story covers the **Parent Guardian** part of the defendant
payload.

References:
- Confluence — [DLRM Interface § Libra and Xhibit Minimum Case Data Schema](https://hmcts.atlassian.net/wiki/spaces/DLRM/pages/280714845/DLRM+Interface#Libra-and-Xhibit-Minimum-Case-Data-Schema)
- Spreadsheet — *DLRM - CP Migration Data Schema V0.13.xlsx*
- Repo architecture reference — `docs/architecture/pcfdlrm-flow-reference.md`

## Story ask (DD-43501) — verbatim acceptance criterion

> **AC 1: Business validation for Parent Guardian level items**
>
> **GIVEN** a case has been received onto the Common Platform via DLRM LIBRA migration,
> **WHEN** the validation routines are run for the 'Parent Guardian' elements of the payload,
> **AND** the business rules are not met for any of the Parent Guardian elements in the fields
> as in the description,
> **THEN** the system must reject the case, or allow it to be migrated to CP, according to the
> table below.
>
> If any of the fields have invalid entries, they should be nulled and therefore empty and
> should follow the "required behaviour for missing fields".

Column key: **O** = optional, **M** = mandatory. SJP is **N/A** for every row, so this story
applies to **Summons, Charge and Requisition** only.

### Individual parent / guardian

| Field (ticket name) | Format | Business rule | Sum / Chg / Req | Ref data | Missing / invalid → |
|---|---|---|---|---|---|
| `parentGuardian.surname` | A35 | CJS Standards §3.39. Provide if a parent/guardian person is given. | O / O / O | — | **Accept** |
| `parentGuardian.workTelephoneNumber` | A35 | Regex `^[0-9+\ \-]{10,}$`: 10 or more characters, each a digit, `+`, `-` or space (CP standard; replaces E-GIF/CJS) | O / O / O | — | **Accept** |
| `parentGuardian.homeTelephoneNumber` | A35 | as above | O / O / O | — | **Accept** |
| `parentGuardian.mobileTelephoneNumber` | A35 | as above | O / O / O | — | **Accept** |
| `parentGuardian.primaryEmail` | A255 | 1–127 chars from `0-9 A-Z a-z ' . - _`, then `@`, then 1–127 chars from the same set | O / O / O | — | **Accept** |
| `parentGuardian.secondaryEmail` | A255 | as above | O / O / O | — | **Accept** |
| `parentGuardian.dateOfBirth` | D10 | Must not be in the future. May need a default. | O / O / O | — | **Accept** |
| `parentGuardian.gender` | N1 | CJS Standards §3.45. Defaults to "Not Known" when missing. `0`=Not Known, `1`=Male, `2`=Female, `9`=Not specified | **M / M / M** | `gender` (CP Gender) | **Accept** (default) |
| `parentGuardian.observedEthnicity` | A1 | "16+1" 2001-census list, CJS Data Standards §3.36 | O / O / O | `observedEthnicity` (CP Observed Ethnicity) | **Accept** |
| `parentGuardian.selfDefinedEthnicity` | A2 | "16+1" 2001-census list, CJS Data Standards §3.37 | O / O / O | `ethnicity` (CP Ethnicity) | **Accept** |
| `parentGuardian.address1` | A35 | Use the standard 3-character no-fixed-abode code if the address is unknown. Provide if a parent/guardian person is given. | **M / M / M** | — | **Reject** |
| `parentGuardian.address2`…`address5` | A35 | — | O / O / O | — | **Accept** |
| `parentGuardian.postcode` | A8 | Must match the postcode regex when present | O / O / O | — | **Reject** |

### Organisation as guardian

| Field (ticket name) | Format | Business rule | Sum / Chg / Req | Missing / invalid → |
|---|---|---|---|---|
| `organisationName` | A255 | CJS Standards §3.87. Provide if an organisation is given. | **M / M / M** | **Reject if the defendant is a youth; otherwise Accept** |
| `companyTelephoneNumber` | A35 | Regex `^[0-9+\ \-]{10,}$` (as above) | O / O / O | **Accept** |
| `organisation.address1` | A35 | Provide if an organisation is given. May need a default. | **M / M / M** | **Reject** |
| `organisation.address2`…`address5` | A35 | — | O / O / O | **Accept** |
| `organisation.postcode` | A8 | Must match the postcode regex when present | O / O / O | **Accept** |

## How ticket field names map to the pcfdlrm payload

Schema: `pcfdlrm-domain/pcfdlrm-domain-value-schema/src/main/resources/json/schema/parent-guardian-information.json`.
It is a `oneOf` of two shapes, both with `additionalProperties: false`:

| Ticket name | Payload path under `defendant.individual.parentGuardianInformation` |
|---|---|
| surname | `personalInformation.lastName` |
| work / home / mobile telephone | `personalInformation.contactDetails.work` / `.home` / `.mobile` |
| primary / secondary email | `personalInformation.contactDetails.primaryEmail` / `.secondaryEmail` |
| dateOfBirth, gender, observedEthnicity, selfDefinedEthnicity | top-level properties of the individual shape |
| address1–5, postcode (individual) | `personalInformation.address.address1`…`address5`, `.postcode` (`pcf-address.json`) |
| organisationName, companyTelephoneNumber | top-level properties of the organisation shape |
| organisation.address1–5, postcode | `address.address1`…`address5`, `.postcode` (`pcf-address.json`) |

## Current state on `team/libra1` (from code)

DLRM defendants are validated with `CcProsecutionValidationRuleProvider.getDefendantValidationRules`
using the `DLRM_MIGRATION` channel, which resolves to `defendantValidationMapDlrm`. That map is
an alias of the SPI map (`COMMON_DEFENDANT_RULE_SET` + `SPI_DEFENDANT_RULE_SET` + the
per-initiation-code set). Rules under
`pcfdlrm-domain/pcfdlrm-domain-aggregate/src/main/java/uk/gov/moj/cpp/pcfdlrm/validation/rules/defendant/`:

| Ticket field | Existing rule / mechanism | Gap against the AC |
|---|---|---|
| dateOfBirth | `ParentGuardianDateOfBirthValidationRule` → `DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE` | Raises a warning only. The value is **not nulled**. |
| observedEthnicity | `ParentGuardianObservedEthnicityValidationAndEnricherRule` (ref data) | Warning only. Not nulled. |
| selfDefinedEthnicity | `ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule` (ref data) | Warning only. Not nulled. |
| primary / secondary email | `ParentGuardian{Primary,Secondary}EmailAddressValidationRule` using `Constants.EMAIL` | Warning only, not nulled. **The regex differs from the ticket's** (see Q3). |
| gender | `ProsecutionCaseFileHelper` raises `PARENT_GUARDIAN_GENDER_INVALID` and defaults to `NOT_KNOWN` | Already matches the AC for invalid values. Not yet confirmed for **missing** gender or for numeric codes `0/1/2/9`. |
| postcode (individual) | `PostCodeValidationRule` → `INVALID_GUARDIAN_POST_CODE` (in `SPI_DEFENDANT_RULE_SET`) | Warning only. The AC says **Reject**. |
| work / home / mobile telephone | — | **No rule.** |
| surname | — | No rule. Optional, so accept either way. Only an A35 length check could apply. |
| address1 (individual) | — | **No mandatory-when-guardian-present rule.** The AC says Reject. |
| organisationName | — | **No rule.** No youth-defendant logic exists anywhere in `src/main`. |
| companyTelephoneNumber | — | **No rule.** |
| organisation address1 / postcode | — | **No rule.** The individual-only `PostCodeValidationRule` does not read the organisation shape's `address`. |

How problems are acted on (`MigratedCaseFileAggregate`): every defendant-level problem that is
not an offence problem becomes a `MigratedCaseValidatedWithWarnings` event, and **the case is
accepted**. The only defendant-driven rejections (`hasOffenceProblems`: invalid offence code,
plea date, verdict date) are gated on `isXhibit(...)`. **No LIBRA defendant-level rejection path
exists today**, so this story adds one.

Apart from the gender default, nothing nulls invalid values today. The sanitising step ("null the
invalid value, then apply the missing-field behaviour") is new. `ProsecutionCaseFileHelper`'s
`applyDefendantAndParentGenderRule` / `applyHearingAndDocumentationLanguageRule` are the existing
pattern to extend.

## Open questions for requirements (Stage 1)

1. **Optional field whose missing behaviour is Reject: individual `postcode`.** The field is O,
   but the "Missing / invalid" column says Reject. Read literally with "null it, then apply
   missing behaviour", an *absent* postcode would also reject. Proposed reading: **absent →
   accept; present but invalid → reject**. Needs confirming. The organisation postcode row says
   Accept, so the two disagree; confirm that is intended.
2. **Defining a youth defendant for `organisationName`.** Nothing in `pcfdlrm` derives
   youth status. Which date of birth (`defendant.individual.personalInformation`/`selfDefinedInformation`),
   which age threshold (under 18?), and as at which date (charge date, migration date, today)?
   What happens if the defendant's date of birth is missing?
3. **Email regex.** The ticket allows `0-9 A-Z a-z ' . - _` on both sides of `@` with 1–127
   characters each. It requires no dot in the domain and rejects `+`. The current
   `Constants.EMAIL` is RFC-like: it allows `+` and other symbols and requires a dotted domain.
   Should parent-guardian email (and, for consistency, defendant email) move to the ticket's
   regex for LIBRA only?
4. **"Mandatory" only when the guardian block exists.** Should `address1` / `organisationName`
   be required only when a parent-guardian block (individual or organisation) is present at
   all? The proposed answer is yes, because the ticket says "to be provided if … provided".
   Is a guardian block holding only empty strings treated as present?
5. **Invalid `address1`.** What makes `address1` invalid (longer than A35? blank?), and does
   the no-fixed-abode 3-character code need special handling?
6. **Gender codes.** Does LIBRA send numeric `0/1/2/9` or the CP enum names? Does `9 = Not
   specified` map to `NOT_SPECIFIED` or default to `NOT_KNOWN`?
7. **Field lengths (A35/A255/A8/N1/D10).** Should a length breach count as "invalid" (null it,
   then apply missing behaviour), or are lengths enforced only by the JSON schema (which today
   caps only `organisationName` at 255 and `companyTelephoneNumber` at 35)?
8. **Scope by source system and case type.** Is this LIBRA only, with XHIBIT left unchanged?
   `caseValidationMapLibra` has no `SUMMONS` key, yet the ticket lists Summons as applicable.
   Confirm that LIBRA Summons cases reach this validation.
9. **How a rejection is reported.** Proposal: a new `MigratedCaseFileProcessed` with
   `processingIsSuccessful=false` and a description per reason (as in the XHIBIT offence path),
   emitted as `public.pcfdlrm.migrated-case-file-processed`. Do downstream systems or reports
   need a specific description text?
10. **Sanitising scope.** When an optional field is nulled, should a warning
    (`MigratedCaseValidatedWithWarnings`) still be raised so the data loss is visible? The
    proposed answer is yes.

## Out of scope

- Defendant-level (non-guardian) field validation, offence, hearing and case-level rules:
  separate DD-32995 stories.
- Any change in `cpp-context-stagingdlrm` or `progression`. Changes there would be separate
  stories, one per repo.

# 01 — Requirements and responsibility analysis

- **Story:** [DD-43493](https://tools.hmcts.net/jira/browse/DD-43493) (epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Base:** `team/libra1`. STAGINGDLRM read at `team/libra1` @ `0b678de`.

## How validation works today

**STAGINGDLRM (LIBRA).** The Azure Function gate validates every LIBRA submission against
`libra.case-submission.json`, a full defendant schema (required fields, `maxLength`, phone, email,
postcode, date and NINO patterns, closed objects). The command API then validates the canonical
schema. A failure at either layer records `error-migrated-case-submission-received` and the case
never reaches PCFDLRM. The STAGINGDLRM rule engine has no defendant rules for either source system.
The convertor is pass-through apart from field renames and gender `0/1/2/9` → `NOT_KNOWN/MALE/FEMALE/NOT_SPECIFIED`
(codes `3`–`8` pass through as the strings `"3"`–`"8"`).

**STAGINGDLRM (XHIBIT).** The gate validates `caseDetails` only; XHIBIT defendants are checked only by
the canonical command schema.

**PCFDLRM.** On the `DLRM_MIGRATION` channel the defendant rule set is the same for LIBRA and XHIBIT
(`CcProsecutionValidationRuleProvider.defendantValidationMapDlrm`, an alias of the SPI map, selected by
initiation code). Every defendant problem raises `DefendantValidationFailed` and a
`MigratedCaseValidatedWithWarnings`; no defendant field rejects a case for either source. The only
XHIBIT-specific defendant behaviour is `ProsecutionCaseFileHelper.applyRuleToDefendantFields`, which
defaults or nulls invalid values:

| Problem | XHIBIT action |
|---|---|
| `HEARING_LANGUAGE_INVALID` / `DOCUMENTATION_LANGUAGE_INVALID` | default `E` |
| `DEFENDANT_GENDER_INVALID` / `PARENT_GUARDIAN_GENDER_INVALID` | default `NOT_KNOWN` |
| `DEFENDANT_NATIONALITY_INVALID` | null |
| `DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID` / `DEFENDANT_OBSERVED_ETHNICITY_INVALID` | null |
| `DEFENDANT_CUSTODY_STATUS_INVALID` | default `U` |

On LIBRA nothing is defaulted today, so an invalid language or an unmapped gender code reaches the
event processor, where `Language.valueOf` / `Gender.valueOf` throw. A lowercase language (`e`, `w`)
passes validation (it is upper-cased there) but the processor converters called `Language.valueOf`
without upper-casing, so it threw for XHIBIT and LIBRA alike.

## Requirements

| ID | Requirement |
|---|---|
| R1 | Reuse the XHIBIT defendant sanitising (`applyRuleToDefendantFields`) for LIBRA, so LIBRA and XHIBIT treat invalid values the same way. |
| R2 | Do not add PCFDLRM rules for checks STAGINGDLRM already makes for LIBRA. |
| R3 | Raise every discrepancy below with the BA; do not build LIBRA-only behaviour for them until agreed. |
| R4 | No schema change for this story (DD-43501 changes `parent-guardian-information.json`); no change to RAML, descriptors or listener; XHIBIT defendant handling unchanged. The processor upper-cases language codes before converting them (shared XHIBIT/LIBRA fix). The parent-guardian observed-ethnicity change in DD-43501 also affects XHIBIT guardians. |

## Per-field responsibility

Legend: **SD** = STAGINGDLRM LIBRA gate; **warn** = `DefendantValidationFailed` + warning, value kept.
"After" is LIBRA behaviour after this story.

| Field | Ticket | Covered by | PCFDLRM rule (shared XHIBIT/LIBRA) | LIBRA after | Discrepancy |
|---|---|---|---|---|---|
| prosecutorDefendantId | M, reject; alnum/hyphen; default 1 | SD: required, max 36 | – | unchanged | D1 (ignored) |
| asn | O, accept | SD: max 20 | – | unchanged | – |
| PNCIdentifier | O, accept | SD: max 13 | `PncIdSpiValidationRule` (warn) | unchanged | D11 (resolved) |
| CRONumber | O, accept | SD: max 12 | `CroNumberSpiValidationRule` (warn) | unchanged | D11 (resolved) |
| title | O, accept | SD: max 35 | – | unchanged | – |
| forename | M, reject; ≤35, no double spaces | SD: optional, max 255 | – | unchanged | D2 (schema: optional), D3 (open) |
| forename2 / forename3 | O, accept; ≤35, no double spaces | SD: max 255 | – | unchanged | D3 (open) |
| surname | M, reject; ≤35, no double spaces | SD: required, max 255 | – | unchanged | D3 (open) |
| organisationName | CM; reject if organisation | SD: optional, max 255 | – | unchanged | D4 (schema) |
| nationality | O, ref data, accept | SD: max 3 | `NationalityValidationAndEnricherRule` | **warn + null** | – |
| additionalNationality | O, ref data, accept | SD: max 3 | `AdditionalNationalityValidationAndEnricherRule` (not O, R or Z; warn) | unchanged | D7 (resolved) |
| work/home/mobile telephone | O, accept | SD: phone pattern, max 35 | – | unchanged | D0 (resolved: STAGINGDLRM format) |
| emails | O, accept | SD: email pattern | email rules (warn) | unchanged | D0 (resolved: STAGINGDLRM format) |
| dateOfBirth | O, not in future, accept | SD: date pattern | `DefendantDateOfBirthValidationRule` (warn) | unchanged | D6 (resolved) |
| gender | CM; 0 if organisation; ref data, accept | SD: required 0–9 for an individual | `validateGenderAndLanguage` | **warn + `NOT_KNOWN`** (covers codes `3`–`8`) | D5 (resolved) |
| observedEthnicity | O, ref data, accept | SD: integer | `ObservedEthnicityValidationAndEnricherRule` | **warn + null** | D12 (resolved) |
| selfDefinedEthnicity | O, ref data, accept | SD: max 2 | `SelfDefinedEthnicityValidationAndEnricherRule` | **warn + null** | – |
| occupation / occupationCode | O, accept | SD: max 54 / 0–99999 | – | unchanged | – |
| driverNumber | CM; reject if driving offence | SD: max 16 | – | unchanged | D9 (schema) |
| licenseCode | O (DVLA: not populated) | SD: max 1 | – (processor maps unknown to null) | unchanged | – |
| documentationLanguage / hearingLanguage | M, default E, accept | SD: required, max 1 | `validateGenderAndLanguage` | **warn + default `E`** | – |
| languageRequirement / specificRequirements | O, accept | SD: max 150 | – | unchanged | – |
| custodyStatus | O; mandatory if Charge; ref data; **reject** | SD: max 1 | `CustodyStatusValidationAndEnricherRule` (C, O, R, Z) | **LIBRA Charge: missing or not in ref data → case rejected. LIBRA R: missing → allowed (stays null); not in ref data → case rejected.** XHIBIT unchanged: warn + default `U` | D8 (resolved) |
| bailConditions | CM if custody B, accept | SD: max 2500 | `BailConditionsValidationAndEnricherRule` (C only; warn) | unchanged | D13 (resolved) |
| address1 | M, reject | SD: `personalInformation.address.address1` required | – | unchanged | D10 (schema) |
| address2–5 / postcode | O, accept | SD: max 35 / postcode pattern | `PostCodeValidationRule` (warn) | unchanged | D0 |
| nationalInsuranceNumber | O, accept | SD: NINO pattern | – | unchanged | D0 |
| individualAliases / aliasForCorporate | O, accept | SD: arrays | – | unchanged | see DD-43494 |

## Discrepancies for the BA

| # | Discrepancy | Classification |
|---|---|---|
| D0 | The ticket says an invalid value is nulled and the case accepted. For format/length checks STAGINGDLRM owns (phones, emails, postcode, NINO, dates, `maxLength`), a failure rejects the whole submission at the STAGINGDLRM gate instead. | **Resolved (2026-10-12, as DD-43501 G0):** keep the current behaviour — STAGINGDLRM schema rejection |
| D1 | `prosecutorDefendantId` alnum/hyphen rule and "Default to 1" are not implemented anywhere (presence is). | **Resolved (2026-10-12):** ignored — no format rule or default; STAGINGDLRM still requires the field |
| D2 | `forename` mandatory for a person: optional in STAGINGDLRM, no rule in PCFDLRM, not required for XHIBIT. | **Resolved (2026-10-12):** follow the STAGINGDLRM schema — forename stays optional (only surname and address are required) |
| D3 | Names over 35 characters should be truncated, and must not contain consecutive spaces. STAGINGDLRM allows 255; PCFDLRM's command schema caps `lastName`, `givenName2`, `givenName3` at 35 and `firstName` at 130, so a LIBRA surname of 36–255 characters passes STAGINGDLRM and then fails PCFDLRM's schema (HTTP 400, no outcome recorded). Nothing truncates or checks spaces. | **Open:** truncation in STAGINGDLRM `MigratedCaseConvertor` was withdrawn after TL review (2026-10-13), so names pass through unchanged and a LIBRA surname or middle name over 35 characters still fails PCFDLRM's schema. Consecutive-spaces rule ignored (2026-10-12, as DD-43494 A2) |
| D4 | `organisationName` conditional mandatory, and a defendant being a person or an organisation but not both: not implemented anywhere. | **Resolved (2026-10-12):** follow the STAGINGDLRM schema — `organisationName` optional, person/organisation exclusivity not enforced |
| D5 | Gender "must be 0 (Not Known)" for an organisation: not implemented. | **Resolved (2026-10-12):** follow the STAGINGDLRM schema — gender required (0–9) for an individual; not enforced for an organisation defendant |
| D6 | Defendant date of birth in the future: warned for both sources, never nulled. The ticket says null and accept. | **Resolved (2026-10-12):** keep the current behaviour — STAGINGDLRM date format; PCFDLRM warns on a future date, value kept |
| D7 | Additional nationality not in ref data: warned but never nulled, and the rule does not run for initiation codes O, R or Z. The ticket says null and accept. | **Resolved (2026-10-12):** keep the current behaviour — PCFDLRM warns when not in reference data, value kept; rule not run for O, R or Z |
| **D8** | **`custodyStatus` (headline question).** The ticket says reject. For initiation codes C, O, R and Z, XHIBIT (and now LIBRA) treat a **missing or invalid** custody status as a warning and set it to `U` (unconditional bail), then accept the case — so a LIBRA Charge defendant with no custody status reaches `progression` as unconditional bail. The rule does not run for S, Q or J. The ticket says mandatory for Charge and null for summons/SJP. | **Resolved (BA):** for **LIBRA**, custody status is **mandatory for Charge (C)**: a missing custody status, or one that does not match the bail-status reference data, rejects the case (`MigratedCaseFileProcessed`, `processingIsSuccessful=false`, "Missing or Invalid custody status"). For **R** a missing custody status is allowed (no warning; stays null, so `progression` gets no bail status) and an invalid one rejects the case. S, Q and J do not check custody status. XHIBIT is unchanged (warning, default `U`). A custody status sent for Summons or SJP is passed through, not nulled (keep the schema, 2026-10-12). |
| D9 | `driverNumber` reject when the offence is endorsable: not implemented; needs offence endorsability data. Possibly one of the two red rows. | **Resolved (2026-10-12):** follow the STAGINGDLRM schema — `driverNumber` optional (max 16), no driving-offence rejection |
| D10 | `address1` for an organisation defendant: STAGINGDLRM requires an address only under `individual.personalInformation`; the defendant-level (organisation) address is optional. | **Resolved (2026-10-12):** follow the STAGINGDLRM schema — defendant-level (organisation) `address` optional; `personalInformation.address.address1` required for an individual |
| D11 | PNC/CRO: only permissive SPI regexes in PCFDLRM (warn); invalid values are not nulled. | **Resolved (2026-10-12):** keep the current behaviour — STAGINGDLRM max length only; PCFDLRM SPI regexes warn, value kept |
| D12 | Observed ethnicity list: "4 Plus 1 or 6 Plus 1 tbc". | **Resolved (2026-10-12):** keep the current behaviour — validated against CP observed-ethnicity reference data |
| D13 | Bail conditions required when custody status is B: rule runs for Charge only. | **Resolved (2026-10-12):** keep the current behaviour — bail conditions required for custody status B on Charge only (warning) |

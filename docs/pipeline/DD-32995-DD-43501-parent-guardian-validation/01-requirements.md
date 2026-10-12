# 01 — Requirements and responsibility analysis

- **Story:** [DD-43501](https://tools.hmcts.net/jira/browse/DD-43501) (epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Base:** `team/libra1`. STAGINGDLRM read at `team/libra1` @ `0b678de`.

## Today

**STAGINGDLRM LIBRA gate** (`libra.case-submission.json`): the guardian is a `oneOf` of a person
(required `gender` 0–9 and `personalInformation`, which requires `surname` and `address` with
`address1`) or an organisation (required `organisationName`). Names, address lines, phones, emails,
postcode and dates have `maxLength` and patterns. A failure rejects the submission before PCFDLRM.
The convertor maps gender `0/1/2/9` to CP names (`3`–`8` pass through as strings) and copies observed
ethnicity into `personalInformation.observedEthnicity` (integer); it never sets the guardian's
top-level `observedEthnicity`. The STAGINGDLRM rule engine has no guardian rules.

**PCFDLRM**: the same guardian rules run for LIBRA and XHIBIT (`COMMON_DEFENDANT_RULE_SET`):
date of birth not in future, observed and self-defined ethnicity against ref data, primary and
secondary email, postcode; plus the guardian gender check in `validateGenderAndLanguage`. Problems
are warnings. XHIBIT defaults an invalid guardian gender to `NOT_KNOWN` in
`applyRuleToDefendantFields`; LIBRA defaults nothing.

`ParentGuardianObservedEthnicityValidationAndEnricherRule` reads the top-level
`parentGuardianInformation.observedEthnicity`, which STAGINGDLRM never sets, so it never validates
the value LIBRA (or XHIBIT) actually sends.

## Requirements

| ID | Requirement |
|---|---|
| R1 | Reuse the XHIBIT guardian handling for LIBRA (via the shared `applyRuleToDefendantFields`; see DD-43493): invalid or missing guardian gender defaults to `NOT_KNOWN`. |
| R2 | Guardian observed ethnicity is read from `personalInformation.observedEthnicity`, the integer STAGINGDLRM sends, by the existing rule, the ref-data enricher and the event-processor converter, so it is validated and sent to `progression`. The unused top-level `observedEthnicity` is removed from `parent-guardian-information.json`. |
| R3 | No PCFDLRM rule for checks STAGINGDLRM already makes. |
| R4 | Raise the discrepancies below with the BA; do not build LIBRA-only behaviour for them. |

## Per-field responsibility

| Field | Ticket | Covered by | PCFDLRM rule (shared) | LIBRA after | Discrepancy |
|---|---|---|---|---|---|
| surname | O, accept | STAGINGDLRM: required (agreed) | – | unchanged | – |
| work/home/mobile telephone | O, accept | STAGINGDLRM phone pattern (agreed) | – | unchanged | G0 |
| primary/secondary email | O, accept | STAGINGDLRM email pattern (agreed) | email rules (warn) | unchanged | G0 |
| dateOfBirth | O, not in future, accept | STAGINGDLRM date pattern | `ParentGuardianDateOfBirthValidationRule` (warn) | **warn + cleared (LIBRA only)** | G1 (resolved) |
| gender | M, default Not Known | STAGINGDLRM: required 0–9 | `validateGenderAndLanguage` | **warn + `NOT_KNOWN`** (covers `3`–`8`) | – |
| observedEthnicity | O, ref data, accept | STAGINGDLRM: integer | `ParentGuardianObservedEthnicityValidationAndEnricherRule` | **validated against ref data; warn + cleared (LIBRA only)** | G1 (resolved), G2 |
| selfDefinedEthnicity | O, ref data, accept | STAGINGDLRM: max 2 | `ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule` (warn) | **warn + cleared (LIBRA only)** | G1 (resolved) |
| address1 | M, reject | STAGINGDLRM: required | – | unchanged | – |
| address2–5 | O, accept | STAGINGDLRM: max 35 | – | unchanged | G0 |
| postcode | O, reject if invalid | STAGINGDLRM: postcode pattern | `PostCodeValidationRule` (warn) | unchanged | – |
| organisationName | M (schema, no youth condition) | STAGINGDLRM: required in the organisation branch | – | unchanged | G4 |
| companyTelephoneNumber | O, accept | STAGINGDLRM: phone pattern | – | unchanged | – |
| organisation address1–5 / postcode | address1 M, reject | STAGINGDLRM: not accepted on the LIBRA organisation branch (`additionalProperties: false`) | – | unchanged | G4, G5 (open) |
| gender (organisation guardian) | – | not in the organisation branch | guardian-gender check skipped when `organisationName` is set | **no problem, no gender added** | G3 |

## Discrepancies for the BA

| # | Discrepancy | Classification |
|---|---|---|
| G0 | The ticket says an invalid phone, email or address line (and organisation postcode) is nulled and the case accepted; STAGINGDLRM's schema rejects the whole submission instead. | **Resolved (BA and TL, 2026-10-12):** keep the current behaviour — STAGINGDLRM schema rejection. |
| G1 | Guardian date of birth in the future and observed/self-defined ethnicity not in ref data were warned but never nulled. The ticket says null and accept (invalid entries are nulled, then "Accept the case"). | **Resolved:** a future guardian date of birth and observed/self-defined ethnicities not in reference data are cleared for LIBRA only (2026-10-13); XHIBIT keeps them with the warning. Warnings kept, case accepted. |
| G2 | The ref-data enricher and the event-processor converter used the unused top-level field, so a valid guardian observed ethnicity was never sent to `progression`. | Resolved in this story: all three read `personalInformation.observedEthnicity`, and the top-level field is removed from the schema |
| G3 | An organisation guardian has no gender, but the guardian-gender check flagged it and the shared XHIBIT/LIBRA default then added `gender: NOT_KNOWN`, giving an object that matches neither `oneOf` branch, so the `migrated-case-file-received` event failed schema validation in the processor (XHIBIT and, after the defendant-validation change, LIBRA). | Resolved: the guardian-gender check is skipped when `organisationName` is set (shared XHIBIT/LIBRA) |
| G4 | The ticket's organisation guardian rows need an organisation address. PCFDLRM's event processor builds no associated person for an organisation guardian, because the core `associatedPerson` model requires a person, so it does not reach `progression` (existing XHIBIT and main prosecution case file behaviour). | **Resolved (BA and TL, 2026-10-12):** keep the current behaviour — organisation guardians are validated in PCFDLRM but not sent to `progression`. |
| G5 | `organisation.address1` is mandatory (reject if missing), but the STAGINGDLRM LIBRA schema does not accept an address on an organisation guardian. | **Open:** making the address required in the LIBRA gate was withdrawn after TL review (2026-10-13). Organisation guardians are not sent to `progression` (G4). |

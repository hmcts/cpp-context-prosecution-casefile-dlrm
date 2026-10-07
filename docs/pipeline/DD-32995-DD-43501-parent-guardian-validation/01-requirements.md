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
| dateOfBirth | O, not in future, accept | STAGINGDLRM date pattern | `ParentGuardianDateOfBirthValidationRule` (warn) | unchanged | G1 |
| gender | M, default Not Known | STAGINGDLRM: required 0–9 | `validateGenderAndLanguage` | **warn + `NOT_KNOWN`** (covers `3`–`8`) | – |
| observedEthnicity | O, ref data, accept | STAGINGDLRM: integer | `ParentGuardianObservedEthnicityValidationAndEnricherRule` | **validated against ref data** (warn) | G1, G2 |
| selfDefinedEthnicity | O, ref data, accept | STAGINGDLRM: max 2 | `ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule` (warn) | unchanged | G1 |
| address1 | M, reject | STAGINGDLRM: required | – | unchanged | – |
| address2–5 | O, accept | STAGINGDLRM: max 35 | – | unchanged | G0 |
| postcode | O, reject if invalid | STAGINGDLRM: postcode pattern | `PostCodeValidationRule` (warn) | unchanged | – |
| organisation fields | – | not sent by LIBRA (agreed) | – | unchanged | – |

## Discrepancies for the BA

| # | Discrepancy | Classification |
|---|---|---|
| G0 | The ticket says an invalid phone, email or address line is nulled and the case accepted; STAGINGDLRM's schema rejects the whole submission instead. | Needs clarification (same as DD-43493 D0) |
| G1 | Guardian date of birth in the future and observed/self-defined ethnicity not in ref data: warned for both sources but never nulled. The ticket says null and accept. XHIBIT nulls the defendant's own ethnicities but not the guardian's. | Extend the XHIBIT behaviour to both sources, or keep warn-only |
| G2 | The ref-data enricher and the event-processor converter used the unused top-level field, so a valid guardian observed ethnicity was never sent to `progression`. | Resolved in this story: all three read `personalInformation.observedEthnicity`, and the top-level field is removed from the schema |
| G3 | The STAGINGDLRM LIBRA schema still accepts an organisation guardian, although LIBRA will not send one. If one arrived, the shared gender default would make it invalid against the guardian schema (see `02-design.md`). | Enforce the agreement: drop the organisation branch from `libra.case-submission.json` (STAGINGDLRM story), or skip the guardian-gender check for organisation guardians (shared XHIBIT/LIBRA) |

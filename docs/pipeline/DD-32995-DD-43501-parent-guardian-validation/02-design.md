# 02 — Design

- **Story:** [DD-43501](https://tools.hmcts.net/jira/browse/DD-43501) (epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))

## Changes

1. **Guardian gender default for LIBRA.** `ProsecutionCaseFileHelper.validateDefendantErrors` now
   calls `applyRuleToDefendantFields` for LIBRA as well as XHIBIT (shared with DD-43493). Its existing
   `PARENT_GUARDIAN_GENDER_INVALID` branch sets the guardian gender to `NOT_KNOWN`. No new code.
2. **Guardian observed ethnicity.** The existing
   `ParentGuardianObservedEthnicityValidationAndEnricherRule` reads
   `parentGuardianInformation.personalInformation.observedEthnicity` (integer, converted to a string
   for the ref-data lookup) instead of the top-level field that is never populated. Same problem code
   and field key. The rule is shared, so XHIBIT guardians are validated too (warning only).
3. **Guardian observed ethnicity reaches `progression`.** `ObservedEthnicityRefDataEnricher` preloads
   the guardian's `personalInformation.observedEthnicity`, and
   `ProsecutionCaseFileMigratedDefendantToCCDefendantConverter.buildEthnicity` maps it onto the
   `ParentGuardian` associated person (`observedEthnicityId/Code/Description`) in
   `progression.initiate-court-proceedings`.
4. **Schema.** The unused top-level `observedEthnicity` is removed from the person branch of
   `parent-guardian-information.json`. STAGINGDLRM never sets it (its guardian schema has no such
   field), so no STAGINGDLRM change is needed; its generated copy of the class drops the field on the
   next `pcfdlrm.version` bump.
   **Replay:** production events never carried the field (STAGINGDLRM never set it), and aggregate
   rehydration ignores unknown properties. Non-production event stores that received commands posted
   directly with the old top-level field (for example the previous integration fixtures) hold
   `migrated-case-file-received` events that would fail schema validation on a processor catch-up, so
   those environments may need an event-store reset first.

5. **Organisation guardians (G3).** `ProsecutionCaseFileHelper.validateGenderAndLanguage` skips the
   guardian-gender check when `parentGuardianInformation.organisationName` is set. An organisation
   guardian has no gender; previously the check flagged it and the shared default added
   `gender: NOT_KNOWN`, producing an object that matches neither `oneOf` branch of
   `parent-guardian-information.json`, so the `migrated-case-file-received` event failed validation in
   the processor. Shared XHIBIT/LIBRA fix.

6. **Invalid guardian date of birth and ethnicities (G1), LIBRA only.** `applyParentGuardianRules`,
   called after `applyRuleToDefendantFields` for LIBRA only, clears the guardian `dateOfBirth` on
   `DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE`, `personalInformation.observedEthnicity` on
   `DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID`, and `selfDefinedEthnicity` on
   `DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID`, in the same way the defendant's own
   nationality and ethnicities are cleared. The warnings remain and the case is accepted; a future
   LIBRA guardian date of birth no longer reaches `progression`. XHIBIT keeps the values (warning only).

## Not changed

- No new rule class, problem code or event; rule sets unchanged.
- Organisation guardians are not mapped to `progression` (G4: current behaviour kept, agreed with the BA and TL).

## Tests

- `ParentGuardianObservedEthnicityValidationAndEnricherRuleTest`: stubs the integer in
  `personalInformation`.
- `ProsecutionCaseFileHelperTest`: the existing gender-and-language sanitising test, which covers the
  guardian gender default, runs for `XHIBIT` and `LIBRA`.
- `ObservedEthnicityRefDataEnricherTest`: guardian ethnicity supplied in `personalInformation`.
- `ProsecutionCaseFileMigratedDefendantToCCDefendantConverterTest`: the existing guardian test also
  asserts the observed ethnicity id and code on the associated person; `CaseReceivedHelper` builds the
  guardian with `personalInformation.observedEthnicity`.
- `ProsecutionCaseFileHelperTest.shouldClearFutureParentGuardianDateOfBirthAndInvalidEthnicitiesOnlyForLibra`
  (LIBRA: date of birth and both ethnicities cleared; XHIBIT: kept).
- `ProsecutionCaseFileHelperTest.shouldNotDefaultGenderForOrganisationParentGuardian` (XHIBIT and LIBRA):
  no `PARENT_GUARDIAN_GENDER_INVALID` and the organisation guardian is unchanged.
- 27 integration-test command fixtures move the guardian's observed ethnicity from the top level
  (now rejected by the schema) to `personalInformation.observedEthnicity`, where STAGINGDLRM sends it.
  The expected `progression.initiate-court-proceedings` payloads are unchanged.

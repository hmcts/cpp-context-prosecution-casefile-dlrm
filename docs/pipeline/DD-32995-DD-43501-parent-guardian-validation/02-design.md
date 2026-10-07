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

## Not changed

- No new rule class, problem code or event; rule sets unchanged.
- Guardian date of birth and ethnicities are not nulled (G1, awaiting the BA).
- Organisation guardian handling (LIBRA does not send it). Note: the STAGINGDLRM LIBRA schema still
  accepts the organisation branch. If one were sent, the existing guardian-gender check would flag the
  missing gender and the shared XHIBIT default would add `gender: NOT_KNOWN`, giving an object that
  matches neither `oneOf` branch, so the `migrated-case-file-received` event would fail validation in
  the processor (XHIBIT already behaves this way). See G3.

## Tests

- `ParentGuardianObservedEthnicityValidationAndEnricherRuleTest`: stubs the integer in
  `personalInformation`.
- `ProsecutionCaseFileHelperTest`: the existing gender-and-language sanitising test, which covers the
  guardian gender default, runs for `XHIBIT` and `LIBRA`.
- `ObservedEthnicityRefDataEnricherTest`: guardian ethnicity supplied in `personalInformation`.
- `ProsecutionCaseFileMigratedDefendantToCCDefendantConverterTest`: the existing guardian test also
  asserts the observed ethnicity id and code on the associated person; `CaseReceivedHelper` builds the
  guardian with `personalInformation.observedEthnicity`.
- 27 integration-test command fixtures move the guardian's observed ethnicity from the top level
  (now rejected by the schema) to `personalInformation.observedEthnicity`, where STAGINGDLRM sends it.
  The expected `progression.initiate-court-proceedings` payloads are unchanged.

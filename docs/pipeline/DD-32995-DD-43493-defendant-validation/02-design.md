# 02 — Design

- **Story:** [DD-43493](https://tools.hmcts.net/jira/browse/DD-43493) (epic [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))

## Change

`ProsecutionCaseFileHelper.validateDefendantErrors` calls `applyRuleToDefendantFields` for every DLRM
migration defendant with problems, without the previous XHIBIT-only source-system check (the only
migration source systems are XHIBIT and LIBRA). No new rule class, problem code, event or schema.

For LIBRA this means the existing XHIBIT handling now applies to invalid values: languages default to
`E`, defendant and parent-guardian gender default to `NOT_KNOWN`, nationality and self-defined/observed
ethnicity are nulled, and custody status defaults to `U`. Problems are still reported as
`DefendantValidationFailed` and warnings, and the case is accepted, exactly as for XHIBIT. It also
stops invalid LIBRA languages and gender codes (`3`–`8`) from reaching the event processor, where
`Language.valueOf` / `Gender.valueOf` would throw.

**Custody status (D8, resolved with the BA).** LIBRA only; XHIBIT is unchanged.
- **Charge (C):** mandatory. A missing custody status, or one not in the bail-status reference data,
  keeps its `DEFENDANT_CUSTODY_STATUS_INVALID` problem, and `MigratedCaseFileAggregate` rejects the case:
  `MigratedCaseFileProcessed` with `processingIsSuccessful=false` and description
  `"Missing or Invalid custody status"`, right after the existing XHIBIT `hasOffenceProblems` check and
  in the same style.
- **Other initiation codes where the rule runs (R):** a missing custody status is allowed —
  `ProsecutionCaseFileHelper.validateDefendantErrors` drops the problem for a LIBRA individual with no
  custody status when the initiation code is not Charge, so there is no warning, no `U` default, and
  `progression` receives no bail status. An invalid one still rejects the case.

**Lowercase languages (shared XHIBIT/LIBRA fix).** Validation upper-cases language codes, so `e` or
`w` pass and are not defaulted, but the processor converters
(`ProsecutionMigrationCaseToCCPersonDefendantConverter`,
`ProsecutionCaseFileMigrationInitialHearingToCCHearingRequestConverter`) called `Language.valueOf`
without upper-casing and threw. They now upper-case first, as the hearing converter already did in
its own validity check.

Everything else in the ticket is either already enforced by the STAGINGDLRM LIBRA schema or listed
as a discrepancy in `01-requirements.md` for the BA. None of the discrepancies is implemented.

## Not changed

- Rule sets, rule classes, problem codes, RAML, descriptors, listener.
- No schema change for this story (DD-43501 changes `parent-guardian-information.json`).
- XHIBIT defendant handling. (The DD-43501 parent-guardian observed-ethnicity change does affect XHIBIT guardians.)
- Apart from a missing (Charge) or invalid custody status (D8), no defendant field rejects a LIBRA case; other
  rejection requests in the ticket are covered by STAGINGDLRM or left to the STAGINGDLRM schema
  (D2, D4, D9, D10) or ignored (D1).

## Tests

The three existing `ProsecutionCaseFileHelperTest` sanitising tests (gender and language defaults,
custody status `U` with observed ethnicity kept, `U` bail-status ref data added) are parameterised
over `XHIBIT` and `LIBRA`.

Three LIBRA aggregate fixtures change to match the XHIBIT ones, because LIBRA invalid values are now
handled the same way: `migrated-case-file-received-no-materials-libra.json`,
`migrated-case-file-received-case-marker-invalid-libra.json` and
`migrated-case-file-received-hearing-unscheduled-libra.json` now expect custody status `U` and no
nationality or self-defined ethnicity.

`ProsecutionMigrationCaseToCCPersonDefendantConverterTest` covers lowercase `e` and `w`.

`MigratedCaseFileAggregateTest.shouldRejectLibraCaseWithMissingOrInvalidCustodyStatus` covers LIBRA: a
missing or invalid custody status on a C case and an invalid one on an R case (rejected); `U` on a C
case, and a missing one on R and Q cases (accepted, with the custody status passed through unchanged).
The LIBRA aggregate scenarios built on a Charge case give the defendant a valid custody status (`U`).
LIBRA expectations for non-Charge defendants with no custody status no longer include a custody-status
warning or a `U` default; XHIBIT expectations are unchanged.
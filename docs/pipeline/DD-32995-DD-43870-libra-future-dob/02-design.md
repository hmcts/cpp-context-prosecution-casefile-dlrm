# 02 — Design

- **Bug:** [DD-43870](https://tools.hmcts.net/jira/browse/DD-43870)
- **Inputs:** `01-requirements.md`. No production code changed at this stage.

## Summary

No new event, schema, subscription or problem code. The existing `DEFENDANT_DOB_IN_FUTURE` warning
keeps firing. On the DLRM channel the helper now also nulls the defendant DOB, for LIBRA and XHIBIT.
It is the same shape as the LIBRA-only clearing of a future parent/guardian DOB (DD-43501 G1), which
is unchanged (FR-4).

## Change

| # | File | Change | FR |
|---|------|--------|----|
| C-1 | `ProsecutionCaseFileHelper.java` | New `applyDefendantDateOfBirthRule(MigratedDefendant.Builder, List<Problem>)`: if `hasProblem(…, DEFENDANT_DOB_IN_FUTURE)`, rebuild `individual.selfDefinedInformation` with `withDateOfBirth(null)`, following the `applyDefendantAndParentGenderRule` shape. | FR-1, FR-3 |
| C-2 | `ProsecutionCaseFileHelper.validateDefendantErrors` (~L131) | Call C-1 after `applyRuleToDefendantFields`, before the `if (LIBRA.equals(…))` parent/guardian block, so it runs for both sources. It runs only on the DLRM channel, after the problems are added to `defendant-validation-failed`, so the warning keeps the original DOB. | FR-1, FR-2, FR-4 |

## Options considered

| Option | Why not |
|---|---|
| **A. Null in the helper for both sources (chosen)** | — Same shape as the parent/guardian DOB (G1). |
| B. Reject the case | Against the ticket decision (accept + null). |
| C. Null in the processor converter | The case submission in the aggregate event (`MigratedCaseFileReceived` / `MigratedCaseValidatedCreationPending`) would still hold the future DOB. |
| D. LIBRA only | Superseded: scope extended to XHIBIT (agreed 2026-11-06). |

## Downstream

- The accepted case reaches `public.pcfdlrm.migrated-case-file-processed` as today. The
  `CCPersonDefendant` converter sends no `dateOfBirth`, so CP shows no age instead of a negative
  one.
- Warning events are unchanged (FR-2). The reconciliation report is updated manually (separate
  sub-task), so nothing downstream changes for it.

## Edge cases

- **"Today" boundary:** the rule uses `isAfter(now(Europe/London))`, so a DOB of today is kept (FR-3).
- **Missing individual or self-defined information:** the rule returns `VALID` (DD-32995 null
  guards), so C-1 never runs.
- **Several defendants:** each is cleared on its own. A valid DOB on another defendant is kept.
- **Copies that keep the original DOB:** `defendant-validation-failed.defendant` (by design, it records
  what was received), the `prosecutionWithReferenceData` in `MigratedCaseValidatedCreationPending`,
  and the defendant copies under `migratedHearingWithReferenceData`, which are built before
  defendant validation. The processor does not read the DOB from any of them: the progression
  defendant comes from the case submission, and the listing request reads only the defendant id and
  hearing language.
- **Youth-dependent logic:** no pcfdlrm rule reads the defendant DOB except C-1's trigger. The
  parent/guardian rules key off the parent/guardian block, not the defendant's age.

## Tests (FR-5)

- `ProsecutionCaseFileHelperTest`: `shouldClearFutureDefendantDateOfBirth` (XHIBIT, LIBRA). It
  asserts the DOB is null and that the `defendant-validation-failed` problem keeps the original date.
- `ProsecutionCaseFileHelperTest`: `shouldKeepDefendantDateOfBirthOfTodayOrEarlier` (both sources ×
  today, yesterday, long past).
- `MigratedCaseFileAggregateTest`: `shouldAcceptCaseAndClearFutureDefendantDateOfBirth` (LIBRA,
  XHIBIT). The case is not rejected, the accepted event (`MigratedCaseFileReceived` or
  `MigratedCaseValidatedCreationPending`) holds a null DOB, and a "Defendant validation"
  `MigratedCaseValidatedWithWarnings` holds `DEFENDANT_DOB_IN_FUTURE : [<dob>]`. New input builders
  `AggregateScenarioInputs.libraDateOfBirthInput` and `xhibitDateOfBirthInput`.
- `ReceiveMigratedCaseFileIT`: two rows added to `receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings`
  (XHIBIT `no-material`, LIBRA `libra-journey`) that inject a defendant DOB one year ahead. The
  test asserts the DOB was injected, then checks the progression `initiatecourtproceedings` request
  matches the existing fixture, which has no defendant DOB. No new fixtures.

# 02 — Design

- **Bug:** [DD-43888](https://tools.hmcts.net/jira/browse/DD-43888)
- **Inputs:** `01-requirements.md` (approved 2026-10-29). No production code changed at this stage.

## Summary

No new event, schema or subscription. Add a LIBRA-only composite offence rule (allocation decision
valid → then date check) to the existing `LIBRA_DEFENDANT_RULE_SET` (DD-43502) and one reject in
`getLibraOffenceRejection`. XHIBIT never runs
the rule, so it is unchanged (FR-3).

## Change

| # | File | Change | FR |
|---|------|--------|----|
| C-1 | `ProblemCode.java` | Add `ALLOCATION_DECISION_DATE_INVALID` and `ALLOCATION_DECISION_INVALID`. | FR-1, FR-1a |
| C-2 | `FieldName.java` | Add `OFFENCE_ALLOCATION_DECISION_DATE("offence_allocationDecision_allocationDecisionDate")` and `OFFENCE_ALLOCATION_DECISION_MOT_REASON_ID("offence_allocationDecision_motReasonId")`. | FR-1, FR-1a |
| C-3 | New `validation/rules/defendant/offence/OffenceAllocationDecisionDateValidationRule.java` | Offence has an `allocationDecision` and its `allocationDecisionDate` is null or after today (Europe/London) → problem C-1, one value per offence. Same shape as `OffenceCommittedEndDateValidationRule`. | FR-1, FR-2 |
| C-3a | New `OffenceAllocationDecisionMotReasonValidationRule` | Offence has an `allocationDecision` whose `motReasonId` is not in `ReferenceDataVO.getModeOfTrialReasonsReferenceData()` (loaded by `ModeOfTrialRefDataEnricher` before validation) → problem `ALLOCATION_DECISION_INVALID`, one value per offence. Same lookup pattern as `PleaValidationRule`; no service call. | FR-1a |
| C-3b | New `OffenceAllocationDecisionValidationRule` | Runs C-3a; only if `VALID`, runs C-3 (fail fast). Same shape as `OffenceCommittedDatesValidationRule` / `CourtHearingLocationValidationRule`. | FR-1, FR-1a |
| C-4 | `CcProsecutionValidationRuleProvider.java` ~L315 | Add C-3b to `LIBRA_DEFENDANT_RULE_SET`. | FR-1, FR-3 |
| C-5 | `MigratedCaseFileAggregate.java` `getLibraOffenceRejection` ~L480 | After the end-date check: `ALLOCATION_DECISION_INVALID` → reject "Invalid allocation decision", then `ALLOCATION_DECISION_DATE_INVALID` → reject "Missing or invalid allocation decision date" (new constants after `MISSING_OR_INVALID_OFFENCE_COMMITTED_END_DATE`). | FR-1, FR-1a |
| C-6 | `pom.xml` | `progression.version` 17.104.306, `sjp.version` 17.104.197 (`RequireLatestMojInterfaceRule`); classifier deps follow the properties. | — |

## Options considered

| Option | Why not |
|---|---|
| **A. LIBRA-only rule + aggregate reject (chosen)** | — Same pattern as DD-43502. |
| B. Make the date required in `migrated-allocation-decision.json` | Shared with XHIBIT (FR-3); a schema failure is a 400, not a recorded reject. |
| C. Inline check in the aggregate | Breaks the rule-class pattern; harder to unit-test. |

## Downstream

- Reject → `MigratedCaseFileProcessedProcessor` → `public.pcfdlrm.migrated-case-file-processed`
  (unchanged).

## Edge cases

- **Rule order:** existing rejects first; one reject per case, first match wins, as today.
- **Invalid allocation decision** (open question 3, resolved): rejected by C-3a; the date check (C-3)
  does not run for it.

## Tests (FR-4)

- `OffenceAllocationDecisionDateValidationRuleTest`: no AD → valid; AD with past/today date → valid;
  AD missing date → problem; AD future date → problem; several offences → one value each.
- `OffenceAllocationDecisionMotReasonValidationRuleTest`: no AD → valid; known
  `motReasonId` → valid; unknown → problem.
- `MigratedCaseFileAggregateTest`: LIBRA reject rows for unknown `motReasonId`, missing and future
  date; LIBRA AD-with-date accepted; XHIBIT AD with no date accepted.
- `OffenceAllocationDecisionValidationRuleTest`: C-3a not valid → C-3 not run; C-3a valid → C-3 result.
- `CcProsecutionValidationRuleProviderTest` (if it asserts the LIBRA set): include C-3b.
- `ReceiveMigratedCaseFileIT.shouldSetAllocationDecisionForDifferentScenario`: LIBRA row reusing the
  XHIBIT allocation-decision payload — valid AD with date accepted and sent to progression. New
  fixture `libra-allocation-with-decision.json` (no `endDate`, no `wordingWelsh`: existing LIBRA
  behaviour). No negative LIBRA IT; existing ITs stay green.

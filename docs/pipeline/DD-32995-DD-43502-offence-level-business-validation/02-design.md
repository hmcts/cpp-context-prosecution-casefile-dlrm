# 02 — Design

- **Story:** [DD-43502](https://tools.hmcts.net/jira/browse/DD-43502) (epic
  [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Inputs:** `01-requirements.md` (approved 2026-10-08). No production code changed at this stage.

## Summary

No new event, schema or subscription. The offence rules already run for LIBRA; the aggregate
discards the rejects and offence warnings behind two `isXhibit` gates. Drop both gates (FR-1 – FR-3,
same approach as DD-43499 / DD-43500). Reuse the existing charge / arrest date rules for FR-4 / FR-5,
add two LIBRA-only rules, end date (FR-6) and committed date (FR-6b), and null a stray end date in the converter (FR-6a).

## Current flow (`MigratedCaseFileAggregate`, receive path)

1. `validateDefendantErrors` — ~L269: runs the defendant + offence rule set, all sources.
2. **Gate O1** `hasOffenceProblems` — ~L423 `if (isXhibit(...))`: rejects on `OFFENCE_CODE_IS_INVALID`,
   `PLEA_DATE_ABSENT` / `_CANNOT_BE_FUTURE_DATE`, `VERDICT_DATE_ABSENT` / `_CANNOT_BE_FUTURE_DATE`.
3. Defendant warnings — ~L291: every problem except the `offenceProblems` list (~L114).
4. **Gate O2** `hasXhibitDefendantProblems` — ~L308 (helper ~L548): `offenceProblems` (invalid plea /
   verdict id, plea / verdict date absent) → warning type "Offence validation".

## Change

| # | File | Change | FR |
|---|------|--------|----|
| C-1 | `MigratedCaseFileAggregate.java` ~L424 | Gate O1: drop `isXhibit`; existing three rejects now apply to LIBRA. | FR-1 – FR-3 |
| C-2 | `MigratedCaseFileAggregate.java` ~L308, ~L548 | Gate O2: drop `isXhibit`; rename `hasXhibitDefendantProblems` → `hasDefendantProblems`. | FR-7 |
| C-3 | `MigratedCaseFileAggregate.java` `hasOffenceProblems` | LIBRA-only, after the C-1 rejects: `CHARGE_DATE_IN_FUTURE` with value "Charge date not provided" → reject "Missing charge date"; `ARREST_DATE_IN_FUTURE` (missing or future) on a case with initiation code `C` → reject "Missing or invalid arrest date". | FR-4, FR-5 |
| C-4 | New `validation/rules/defendant/offence/OffenceCommittedEndDateValidationRule.java` + `ProblemCode.OFFENCE_COMMITTED_END_DATE_INVALID` | `offenceDateCode` = 4 and `offenceCommittedEndDate` null, not after `offenceCommittedDate`, or after today (Europe/London) → problem. | FR-6 |
| C-5 | `CcProsecutionValidationRuleProvider.java` ~L330; caller `ProsecutionCaseFileHelper.java` ~L97 | New `LIBRA_DEFENDANT_RULE_SET` (`OffenceCommittedDatesValidationRule`: runs C-4a, then C-4 only if the committed date is valid — same shape as `CourtHearingLocationValidationRule`), appended when the source is LIBRA; `getDefendantValidationRules` gets the source system name, as `getCaseValidationRules` already does. | FR-6, FR-8 |
| C-4a | New `OffenceCommittedDateValidationRule` + `ProblemCode.OFFENCE_COMMITTED_DATE_IN_FUTURE`, in the LIBRA rule set (C-5) | Committed date after today (Europe/London) → problem → reject "Invalid offence committed date". | FR-6b |
| C-6 | `MigratedCaseFileAggregate.java` `hasOffenceProblems` | `OFFENCE_COMMITTED_END_DATE_INVALID` → reject "Missing or invalid offence committed end date" (LIBRA only by C-5). | FR-6 |
| C-7 | `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.java` ~L129 | LIBRA and `offenceDateCode` ≠ 4 → `endDate` null. | FR-6a |

Existing rules (C-3): `ChargeDateValidationRule` / `ArrestDateValidationRule` already detect a missing
date on the right case types (charge: J, C, Q, R, Z; arrest: C, R, Z) but report it under the
`_IN_FUTURE` codes; the problem value tells missing from future (`ChargeDateValidationRule:40`,
`ArrestDateValidationRule:40`). Their codes are kept so XHIBIT warnings don't change (FR-8). A future
charge date stays a warning; the arrest rule also runs for R / Z, so the reject checks the
case initiation code (LIBRA has it at case level only). No end-date rule exists in pcfdlrm or prosecutioncasefile.

## Options considered

| Option | Why not |
|---|---|
| **A. Drop gates, reuse charge / arrest rules, LIBRA-only end-date rule, converter null (chosen)** | — Follows the rule-class pattern; XHIBIT untouched. |
| B. End-date rule in the common rule set | Raises new problems for XHIBIT too (FR-8). |
| C. New `_ABSENT` codes in the charge / arrest rules | Changes XHIBIT warning codes (FR-8). |
| D. Inline LIBRA checks in the aggregate | Breaks the rule-class pattern; harder to unit-test. |
| E. Null the end date in the aggregate fix-ups | The event would store the nulled value, so the original end date is lost on replay. Converter keeps the event as received (design note 2). |

## Downstream

- Rejects → `MigratedCaseFileProcessedProcessor` → `public.pcfdlrm.migrated-case-file-processed`
  (unchanged).
- C-7 changes the `endDate` sent to `progression` for LIBRA offences with code ≠ 4.

## Edge cases

- **Initiation code `R`** (allowed for LIBRA, not in the ticket columns) is not Summons → missing charge
  date rejects.
- **Rule order:** C-1 rejects first, then C-3 / C-6; one reject per case, first match wins, as today.

## Tests (FR-9)

- `MigratedCaseFileAggregateTest`: parameterise the XHIBIT offence-code, plea-date and verdict-date
  reject tests by source; new LIBRA reject rows for C-3 (missing charge date; missing / future arrest date on `C`), C-4a and C-6.
  Not-rejected rows: LIBRA future charge date, missing arrest date on `R`, not-guilty plea with no
  date, unrecognised plea code (plus its "Offence validation" warning); XHIBIT missing / future arrest
  date on `C`, future committed date, code 4 with no end date (AC-9).
- `OffenceCommittedEndDateValidationRuleTest`: code 4 with end date missing / not after start / future
  / valid; code ≠ 4 ignored.
- `OffenceCommittedDateValidationRuleTest`: future → problem; today, past or missing → none.
- `OffenceCommittedDatesValidationRuleTest`: future committed date → only that problem; valid committed date → end-date result.
- `CcProsecutionValidationRuleProvider` test: LIBRA set includes the committed-dates rule, XHIBIT set doesn't.
- Converter test: LIBRA code 1 + end date → null; code 4 → kept; XHIBIT unchanged.
- ITs: no new ITs. Three LIBRA expected payloads (`libra-journey`, `libra-indicated-plea`,
  `libra-indicated-guilty-plea`) send code 1 with an end date → drop `endDate` from their expected
  `initiate-court-proceedings` JSON. Two LIBRA multiple-hearing fixtures used offence code `OFCODE13`,
  unknown to the IT ref-data stub, so FR-1 rejected them → changed to `OFCODE12`.

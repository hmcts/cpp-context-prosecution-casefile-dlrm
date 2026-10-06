# 02 — Design

- **Story:** [DD-43500](https://tools.hmcts.net/jira/browse/DD-43500) (epic
  [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995))
- **Status:** Stage 2 approved 2026-10-05.
- **Inputs:** `01-requirements.md` (approved 2026-10-05). No production code changed at this stage.

## Summary

No new pattern, rule, event or schema. The hearing rules already run for LIBRA; the aggregate
discards the result behind two `isXhibit(...)` gates. Drop both gates so LIBRA acts on hearing
problems exactly as XHIBIT does (FR-1, FR-6). Same approach as DD-43499.

## Current flow (`MigratedCaseFileAggregate`, receive path)

1. Case rules + case rejects — ~L210-251.
2. `validateHearings` — ~L254: runs `MIGRATED_HEARING_RULE_SET` per hearing, all sources.
3. **Gate H1** `hasNoMatchingDefendantsForXhibitHearing` — ~L257 (helper ~L542):
   `NO_MATCHING_DEFENDANTS_FOR_HEARING` → `migrated-case-file-processed` (`processingIsSuccessful=false`),
   return.
4. Defendant / offence validation, case + defendant warnings — ~L267-302.
5. **Gate H2** `generateXhibitHearingWarnings` — ~L305 (method ~L411): every hearing problem →
   `migrated-case-validated-with-warnings`, type "Hearing validation".

## Change

| # | File | Change |
|---|------|--------|
| C-1 | `MigratedCaseFileAggregate.java` ~L542 | Gate H1: drop `&& isXhibit(...)`; inline to `hasNoMatchingDefendantsForHearing`, remove the XHIBIT-named helper. |
| C-2 | `MigratedCaseFileAggregate.java` ~L411 | Gate H2: drop `&& isXhibit(...)`; rename `generateXhibitHearingWarnings` → `generateHearingWarnings`. |

After C-1/C-2 the new LIBRA effects are:

| Problem (LIBRA) | Outcome | FR |
|---|---|---|
| `NO_MATCHING_DEFENDANTS_FOR_HEARING` | Reject, "No matching defendants with hearings found for the hearing" | FR-6 |
| `COURT_HEARING_LOCATION_OUCODE_INVALID`, `COURTROOM_ID_INVALID`, `HEARING_TYPE_CODE_INVALID`, `DATE_OF_HEARING_IN_THE_PAST`, `DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE`, week-commencing problems | Accept + hearing warning | FR-1 |

Unchanged, already source-agnostic: processor converter skips a hearing with unknown OU / hearing
type or a past date, lists without a room if the court room doesn't match (FR-2); 10:00 default
(FR-3); duration default (FR-4). XHIBIT unaffected (FR-6a).

## Options considered

| Option | Why not |
|---|---|
| **A. Drop both gates (chosen)** | — Smallest change; nothing in the hearing set is XHIBIT-only. |
| B. LIBRA-only branch | Duplicates the reject/warning blocks; two copies to drift. |

## Downstream

- Reject → `MigratedCaseFileProcessedProcessor` → `public.pcfdlrm.migrated-case-file-processed`
  (unchanged). No new subscription or schema.
- Warning event has no processor subscription (event store only), same as XHIBIT.

## Edge cases

- **Past hearings** are common in migrated LIBRA data → each gets a `DATE_OF_HEARING_IN_THE_PAST`
  warning. Expected, as XHIBIT.
- **A hearing with any problem doesn't get the 10:00 default** (~L387 requires `problems.isEmpty()`).
  Not reachable for LIBRA: stagingdlrm rejects a missing time.
- **Partial defendant match** — one unmatched listed defendant/offence empties the whole match
  (`ProsecutionCaseFileHelper` ~L164) → reject. As XHIBIT.
- **Existing tests:** no current LIBRA unit input carries hearings, and all LIBRA IT fixtures with
  hearings have fully matching listed defendants → no expected-event changes.

## Tests

| Level | Change |
|---|---|
| Unit — `AggregateScenarios` | LIBRA row reusing the XHIBIT no-matching-defendants input (source system now a parameter). |
| Unit — `MigratedCaseFileAggregateTest` | XHIBIT unscheduled-hearing test parameterised over XHIBIT + LIBRA. LIBRA received fixture differs only where XHIBIT-only defendant fix-ups apply (custody status default, invalid ethnicity / nationality removal). |

No LIBRA ITs (agreed scope). No new endpoint or `@Handles`. Existing ITs must stay green via
`./runIntegrationTests.sh`.

## ADR

Not needed — follows XHIBIT behaviour and the DD-43499 / DD-43130 precedent.

## Open questions

None.

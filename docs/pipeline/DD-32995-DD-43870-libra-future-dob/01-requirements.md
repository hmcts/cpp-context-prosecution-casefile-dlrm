# 01 — Requirements

- **Bug:** DD-43870 — see `00-input-brief.md`.
- **Scope:** pcfdlrm only, LIBRA and XHIBIT (agreed 2026-11-06).

| ID | Requirement |
|----|-------------|
| FR-1 | LIBRA or XHIBIT defendant `individual.selfDefinedInformation.dateOfBirth` after today (Europe/London) → set it to null and accept the case. Every other defendant field is unchanged. |
| FR-2 | The `DEFENDANT_DOB_IN_FUTURE` warning is still raised for FR-1 (`defendant-validation-failed`, `MigratedCaseValidatedWithWarnings`) and carries the original DOB. |
| FR-3 | Defendant DOB of today or earlier, or no DOB → unchanged. |
| FR-4 | Parent/guardian rules unchanged: still cleared for LIBRA only (DD-43501). |
| FR-5 | Unit tests for FR-1 – FR-3 for both sources. One IT per source: a case with a future defendant DOB is accepted and sent to progression without `dateOfBirth`. Existing ITs stay green. |

## Acceptance criteria

- **AC-1:** GIVEN a LIBRA or XHIBIT case whose defendant has a future date of birth, WHEN it is
  validated, THEN the case is accepted, the defendant reaches progression with no date of birth, and
  a `DEFENDANT_DOB_IN_FUTURE` warning with the original date is recorded.
- **AC-2:** GIVEN a LIBRA or XHIBIT case whose defendant DOB is today or earlier, or absent, WHEN it
  is validated, THEN the DOB is passed through unchanged.

## Open questions

1. ~~Reconciliation report~~ — **resolved (2026-11-06):** adding the case to the reconciliation
   report is a manual process, tracked in a separate sub-task of DD-43870. Out of scope here.
2. ~~Organisation defendants~~ — **resolved:** the DOB only exists on `individual`, so there is
   nothing to change.

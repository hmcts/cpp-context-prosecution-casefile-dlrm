# 01 — Requirements

- **Bug:** DD-43888 — see `00-input-brief.md`.
- **Scope:** pcfdlrm only, LIBRA only. XHIBIT behaviour unchanged.

| ID | Requirement |
|----|-------------|
| FR-1 | LIBRA offence has a valid `allocationDecision` and `allocationDecisionDate` is missing or in the future → reject the case ("Missing or invalid allocation decision date"). A future date is invalid, and invalid is treated as missing (ticket note) → reject, as DD-43502 arrest date. |
| FR-1a | LIBRA offence `allocationDecision.motReasonId` not in CP mode-of-trial ref data → reject the case ("Invalid allocation decision"); the date is not checked (BA, open question 3). |
| FR-2 | LIBRA offence with no `allocationDecision` → accept (no change). |
| FR-3 | XHIBIT paths unchanged. |
| FR-4 | Unit tests for the new rejects; one positive LIBRA allocation decision IT; no negative LIBRA IT; existing ITs stay green. |

## Acceptance criteria

- **AC-1:** GIVEN a LIBRA case with an offence whose valid allocation decision has a missing or
  future allocation decision date, WHEN it is validated, THEN the case is rejected.
- **AC-1a:** GIVEN a LIBRA case with an offence whose allocation decision `motReasonId` is not in CP
  ref data, WHEN it is validated, THEN the case is rejected ("Invalid allocation decision").
- **AC-2:** GIVEN a LIBRA case whose offences have no allocation decision, or a valid allocation
  decision dated today or earlier, WHEN it is validated, THEN the case is accepted as today.

## Open questions

1. ~~Rejection wording~~ — "Missing or invalid allocation decision date" (same style as
   "Missing or invalid arrest date").
2. ~~Future date~~ — **resolved:** reject (FR-1; user 2026-10-29).
3. ~~Invalid allocation decision~~ — **resolved:** (a) reject the case (FR-1a; BA verbal 2026-10-29,
   written update on DD-43888 to follow).

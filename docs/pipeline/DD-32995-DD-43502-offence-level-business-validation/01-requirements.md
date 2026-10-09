# 01 — Requirements

- **Story:** DD-43502 (epic DD-32995) — see `00-input-brief.md`.
- **Scope:** LIBRA offence-level business validation. XHIBIT behaviour unchanged.
- **Format column / mandatory fields:** already enforced by the LIBRA intake schema
  (`libra.case-submission.json`) — no pcfdlrm change.

## pcfdlrm

| ID | Requirement | XHIBIT precedent |
|----|-------------|------------------|
| FR-1 | `offenceCode` not in CP offence ref data → reject ("Invalid offence code"). | `hasInvalidOffenceCode` reject (XHIBIT-only today) |
| FR-2 | Guilty-type plea with missing or future `pleaDate` → reject ("Missing or Invalid plea date"). Conditional mandatory: follows XHIBIT, so a non-guilty plea without a date is accepted (user decision 2026-10-08). | `PleaValidationRule` + `hasInvalidPleaDate` (XHIBIT-only today) |
| FR-3 | Valid verdict with missing or future `verdictDate` → reject ("Missing or Invalid verdict date"). | `VerdictValidationRule` + `hasInvalidVerdictDate` (XHIBIT-only today) |
| FR-4 | Missing `chargeDate` on a J / C / Q / R case (any LIBRA code except Summons) → reject. Future date stays a warning. | `ChargeDateValidationRule` detects; XHIBIT only warns |
| FR-5 | Missing or future `arrestDate` on a Charge case → reject (ticket: "must not be in the future"; invalid → missing). | `ArrestDateValidationRule` detects; XHIBIT only warns |
| FR-6 | `offenceDateCode` = 4 and `offenceCommittedEndDate` missing, not after `offenceCommittedDate`, or in the future → reject. | None (new) |
| FR-6a | `offenceDateCode` ≠ 4 and `offenceCommittedEndDate` present → null it, accept (ticket: "must be blank"; invalid → null → missing behaviour). | None (new) |
| FR-6b | `offenceCommittedDate` in the future → reject (ticket: "must not be in the future"; missing → rejected by the LIBRA intake schema). | None (new) |
| FR-7 | No change: `convictingCourtCode` (follow XHIBIT), `allocationDecisionDate` (user decision 2026-10-08), alcohol / vehicle / location (invalid → warning, value kept, as XHIBIT — user decision 2026-10-08). | Already source-agnostic |
| FR-8 | XHIBIT validation paths unchanged. | — |
| FR-9 | Unit tests only for the new LIBRA outcomes; no negative LIBRA ITs; existing ITs stay green. | — |

## stagingdlrm

- `offenceSequenceNumber` minimum 0 → 1 ("number starting at 1").

## Acceptance criteria

- **AC-1:** LIBRA offence problems rejected as XHIBIT for offence code, plea date and verdict date
  (FR-1 – FR-3), plus the ticket's charge date, arrest date and end-date rejects (FR-4 – FR-6b); all
  other offence problems stay warnings (FR-7).

## Open questions

1. ~~FR-6 location~~ — **resolved:** pcfdlrm business validation, not schema (user 2026-10-08).
2. ~~Optional + invalid~~ — **resolved:** keep XHIBIT behaviour, warning and value kept (FR-7).
3. ~~End date when code ≠ 4~~ — **resolved:** LIBRA follows the ticket rules (FR-6, FR-6a). Existing
   LIBRA fixtures send an end date with code 1, so expected outputs may change.

# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995)
- **Story:** [DD-43502](https://tools.hmcts.net/jira/browse/DD-43502) — "Business validation for Offence
  level items"
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`). Schema-level checks go to stagingdlrm
  (`libra.case-submission.json`), handled separately.

> AC taken from the ticket export of 2026-10-08. Reconcile against the live ticket before sign-off.

## Acceptance criteria (as given)

**AC-1 — Offence-level business validation:** GIVEN a case received onto CP via DLRM LIBRA migration,
WHEN validation runs for the offence elements and a business rule is not met, THEN the system rejects
or accepts the case per the table below.

> Ticket note: invalid entries are nulled and follow "required behaviour for missing fields". Per the
> DD-43499 decision: mandatory + invalid → follow XHIBIT; optional + invalid → null.
> M = mandatory, CM = conditional mandatory, O = optional.

| Field | Format | SJP / Summons / Charge / Req | Missing → |
|---|---|---|---|
| `prosecutorOffenceId` | A36 | M / M / M / M | Reject |
| `offenceCode` | A8 (CP offence ref data) | M / M / M / M | Reject |
| `offenceSequenceNumber` | N3, starts at 1 | M / M / M / M | Reject |
| `chargeDate` | D10, follow XHIBIT | M / N/A / M / M | Accept for Summons, reject otherwise |
| `offenceDateCode` | N1, 1–6 | M / M / M / M | Reject |
| `offenceCommittedDate` | D10, not in future | M / M / M / M | Reject |
| `offenceCommittedEndDate` | D10; mandatory if date code = 4 | CM | Reject if "between" (4), else accept |
| `arrestDate` | D10, not in future | N/A / N/A / M / N/A | Reject for Charge, accept otherwise |
| `alcoholOrDrugLevelMethod` | A1 (CP ref data) | CM | Accept |
| `alcoholOrDrugLevelAmount` | N3 | CM | Accept |
| `offenceLocation` | A80 | O | Accept |
| `offenceWording` | A2500 | M / M / M / M | Reject |
| `offenceWordingWelsh`, `statementOfFacts`, `statementOfFactsWelsh` | A2500 / A4000 | O | Accept |
| `vehicleCode` (L / O), `vehicleMake`, `vehicleRegistrationMark` | A1 / A60 / A11 | O | Accept |
| `pleaCode` | A36 | O | Accept |
| `pleaDate` | D10 | CM | Reject if a plea is entered |
| `verdictType` | A36 | O | Accept |
| `verdictDate` | D10 | CM | Reject if there is a verdict |
| `convictingCourtCode` | A7 OU code | CM | Follow XHIBIT |
| `allocationDecision` | A36 | O | Accept |
| `allocationDecisionDate` | D10 | CM | Reject if there is an allocation decision |

## Investigation

pcfdlrm and stagingdlrm read from `origin/team/libra1`.

**stagingdlrm LIBRA intake schema** already requires `prosecutorOffenceId`, `offenceCode`,
`offenceSequenceNumber`, `offenceDateCode` (1–6), `offenceCommittedDate`, `offenceWording`, and
enforces every length / date format above. Gaps: `offenceSequenceNumber` minimum is 0, not 1;
end date not tied to date code 4. The schema is draft-04 (networknt V4), so a conditional needs
`anyOf`, not `if/then`.

**pcfdlrm (XHIBIT precedent)** — offence rules run for every source; rejects are XHIBIT-only:

| Field | XHIBIT | LIBRA today |
|---|---|---|
| `offenceCode` not in ref data | Reject "Invalid offence code" | Warning only |
| `pleaDate` future (any valid plea) or missing (guilty-type plea) | Reject "Missing or Invalid plea date" | Not rejected, no warning |
| `verdictDate` missing on a valid verdict, or future | Reject "Missing or Invalid verdict date" | Missing: not rejected, no warning. Future: warning |
| `chargeDate` missing (J, C, Q, R, Z) or future | Warning only | Same |
| `arrestDate` missing (C, R, Z) or future | Warning only | Same |
| `offenceCommittedEndDate` | Not validated | Same |
| `allocationDecisionDate` | Not validated | Same |
| `convictingCourtCode` | Never rejected; derived from hearing OU for guilty plea / verdict | Same |
| `offenceCommittedDate` in future | Not validated | Same |
| Alcohol method / amount, vehicle code, location | Warning only, value kept | Same |

XHIBIT rejects sit in `MigratedCaseFileAggregate.hasOffenceProblems` (~L423, `isXhibit` gate).
`offenceDateCode` is hard-coded (schema 1–6); its ref-data plumbing is unused.

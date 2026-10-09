# 03 — User Story

## DD-43502 — LIBRA offence-level business validation

**As** the DLRM migration service
**I want** LIBRA offence problems rejected or reported per the DD-43502 table, reusing XHIBIT's handling
**So that** a LIBRA case with a missing or invalid mandatory offence item is rejected instead of
silently migrated.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`. The stagingdlrm schema change
  (`offenceSequenceNumber` minimum 1) is handled in that repo.
- `MigratedCaseFileAggregate`, `CcProsecutionValidationRuleProvider`, new
  `OffenceCommittedEndDateValidationRule` and `OffenceCommittedDateValidationRule`, offence converter (see `02-design.md` C-1 – C-7).

### Acceptance criteria

See `01-requirements.md` FR-1 – FR-9. For LIBRA, each reject is `migrated-case-file-processed`,
`processingIsSuccessful=false`, with the description below and no received / creation-pending event:
1. Offence code not in ref data → "Invalid offence code".
2. Guilty-type plea with missing or future plea date → "Missing or Invalid plea date".
3. Valid verdict with missing or future verdict date → "Missing or Invalid verdict date".
4. Missing charge date on a J / C / Q / R case (not Summons) → "Missing charge date". Future charge date → warning.
5. Missing or future arrest date on a Charge case → "Missing or invalid arrest date". Other case types → warning.
6. Date code 4 with end date missing, not after the committed date, or in the future → "Missing or
   invalid offence committed end date".
6a. Committed date in the future → "Invalid offence committed date".
7. Date code ≠ 4 with an end date → case accepted, `endDate` not sent to `progression`.
8. Invalid plea / verdict id → case accepted, "Offence validation" warning, as XHIBIT.
9. XHIBIT outcomes unchanged for all of the above.

### Definition of done

- [ ] C-1 – C-7 implemented; no other production change.
- [ ] LIBRA unit tests reuse the XHIBIT offence inputs/fixtures, parameterised by source; new tests for
      the end-date and committed-date rules, the LIBRA rule set and the converter.
- [ ] Existing XHIBIT tests green unchanged (AC-9).
- [ ] `mvn clean install` green; `./runIntegrationTests.sh` green (existing ITs; three LIBRA expected
      payloads lose `endDate`; no new LIBRA ITs).

### Out of scope

- Format / mandatory fields — enforced by the LIBRA intake schema (stagingdlrm).
- `convictingCourtCode` (follow XHIBIT), `allocationDecisionDate` (left as is).
- Optional fields with invalid values — warning, value kept, as XHIBIT.
- Negative LIBRA ITs (unit level only).

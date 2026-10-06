# 03 — User Story

## DD-43500 — LIBRA hearing-level business validation

**As** the DLRM migration service
**I want** LIBRA hearing validation problems acted on the same way as XHIBIT's
**So that** a LIBRA hearing with no matching defendants is rejected instead of silently migrated,
and other hearing problems are reported as warnings.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`. stagingdlrm unchanged.
- `MigratedCaseFileAggregate` gates H1 (~L542) and H2 (~L411) + `AggregateScenarios` (see
  `02-design.md`).

### Acceptance criteria

See `01-requirements.md` FR-1 – FR-6a:
1. LIBRA, a hearing whose listed defendants / offences don't all match → `migrated-case-file-processed`,
   `processingIsSuccessful=false`, "No matching defendants with hearings found for the hearing"; no
   received / creation-pending event.
2. LIBRA, invalid court location, court room or hearing type, or a past / pre-offence hearing date →
   case accepted, plus `migrated-case-validated-with-warnings` ("Hearing validation", problem code).
3. XHIBIT outcomes unchanged for all of the above.

### Definition of done

- [x] `isXhibit` removed from both hearing gates; `generateXhibitHearingWarnings` renamed to
      `generateHearingWarnings`; no other production change.
- [x] LIBRA unit tests for AC-1 (`AggregateScenarios` row) and AC-2 (standalone test); no existing rows re-baselined.
- [x] Existing XHIBIT scenarios green unchanged (AC-3) — aggregate 338/338.
- [x] `mvn clean install` green; `./runIntegrationTests.sh` green — 28/28 (existing ITs, no new LIBRA ITs).

### Out of scope

- Ticket Format column — enforced by the shared stagingdlrm schema.
- Missing court room / date / time — rejected in stagingdlrm (kept); ticket divergence flagged to BA.
- Negative LIBRA ITs (unit level only).
- Duration-to-days conversion (downstream); `hearingType` mapping check (BA).

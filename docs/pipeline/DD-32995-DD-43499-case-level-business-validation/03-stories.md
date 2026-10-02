# 03 — User Story

## DD-43499 — LIBRA case-level business validation (pcfdlrm half)

**As** the DLRM migration service
**I want** LIBRA case-level validation problems acted on the same way as XHIBIT's
**So that** a LIBRA case with an unrecognised prosecuting authority is rejected instead of
silently migrated, and invalid case markers are reported as warnings.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`. The stagingdlrm half (schema + informant
  rule) is fulfilled separately in `cpp-context-stagingdlrm` under the same story.
- `MigratedCaseFileAggregate` gates A (~L214) and B (~L275) + `AggregateScenarios` (see
  `02-design.md`).

### Acceptance criteria

See `01-requirements.md` FR-1 – FR-6:
1. LIBRA, `prosecutingAuthority` not recognised by refdata → `migrated-case-file-processed`,
   `processingIsSuccessful=false`, "Invalid Prosecuting Authority"; no received / creation-pending
   event.
2. LIBRA, invalid case marker → case accepted, plus `migrated-case-validated-with-warnings`
   ("Case validation", `CASE_MARKER_IS_INVALID`); invalid markers are not sent to progression —
   exactly as XHIBIT.
3. LIBRA, initiation code not in refdata initiation-types → case accepted (not acted on).
4. XHIBIT outcomes unchanged for all of the above.

### Definition of done

- [x] Prosecuting-authority reject moved outside the `isXhibit` gate; `isXhibit` removed from the case-marker warning gate; no other production change.
- [x] `AggregateScenarios`: LIBRA rows for AC-1 and AC-2 (mirroring the XHIBIT rows); existing LIBRA
      rows re-baselined if they gain a warning.
- [x] Existing XHIBIT scenarios green unchanged (AC-4) — aggregate 336/336.
- [x] `mvn clean install` green; `./runIntegrationTests.sh` green — 28/28 (existing ITs, no new LIBRA ITs).

### Out of scope

- Negative LIBRA ITs (user direction — unit level only).
- `originatingOrganisation` / `cpsOrganisation` checks — XHIBIT has none (Stage 1 OQ1; ticket
  divergence flagged to BA).
- `prosecutorCaseReference` generation, source case-identifier uniqueness (Stage 1 OQ2, OQ3).
- Processor converter null-safety on unknown prosecutor (no longer reachable; not changed).

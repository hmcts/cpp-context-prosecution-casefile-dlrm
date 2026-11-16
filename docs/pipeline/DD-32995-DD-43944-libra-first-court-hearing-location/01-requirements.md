# 01 — Requirements

- **Story:** DD-43944 — see `00-input-brief.md`.
- **Scope (this repo):** pcfdlrm. The Function App and stagingdlrm service changes are the stagingdlrm
  story; the cross-repo contract is [ADR-DD-43944](../adrs/ADR-DD-43944-libra-first-court-hearing-location.md).

## Answered questions

| # | Question | Answer |
|---|----------|--------|
| Q1 | Must the value reach Progression / CP, and how? | Yes, inside `ProsecutionCase.migrationSourceSystem`. Core-domain adds the field to core `migrationSourceSystem`; Progression and pcfdlrm bump `coredomain` (developer, 2026-11-13). |
| Q2 | Reference-data check on the OU code in pcfdlrm? | Yes. Look the OU code up in reference data. Not found → set it to null (not sent to Progression), accept the case, and add the case to the reconciliation report (developer, 2026-11-13; supersedes an earlier "no validation" answer the same day). |
| Q3a | Core-domain branch for the Progression change? | New team branch `team/libra4` in `cpp-platform-core-domain`, cut from `release/17.104.x` (`66ce1aa14`, 17.104.7-SNAPSHOT); changes go in through `dev/*` branch PRs into it (developer, 2026-11-13; replaces earlier `main` and `release/17.104.x` answers). Progression and pcfdlrm bump `coredomain` to the version that branch publishes. |
| Q3b | Progression branch for the `coredomain` bump? | New team branch `team/libra4` in `cpp-context-progression`, cut from `main` (`f3dbf8c89`, 17.104.309-SNAPSHOT, `coredomain` 17.104.6); changes go in through `dev/*` branch PRs into it (developer, 2026-11-13). |
| Q4 | XHIBIT? | Unchanged; it does not send the field (developer, 2026-11-13). |
| Q5 | How is the case added to the reconciliation report? | Manual process, as DD-43870: the "Case validation" warning records the original OU code; no report automation in this story (developer, 2026-11-13). |
| Q6 | Warning problem code? | New `ProblemCode.FIRST_COURT_LOCATION_OUCODE_INVALID` (not `COURT_LOCATION_OUCODE_INVALID`), field `firstCourtHearingLocation`, original value (developer, 2026-11-13). Keeps it out of the XHIBIT `hasInvalidOuCode` reject path, which keys on `COURT_LOCATION_OUCODE_INVALID`. |

## Requirements

| ID | Requirement |
|----|-------------|
| FR-1 | `migrated-migrationSourceSystem.json` declares optional `firstCourtHearingLocation` (string, length 7). |
| FR-2 | `MigratedCaseToProsecutionCaseConverter.buildMigrationSourceSystem` copies it to `ProsecutionCase.migrationSourceSystem`. |
| FR-2a | When `firstCourtHearingLocation` is set and the reference-data organisation-unit lookup finds nothing: raise a "Case validation" warning (`FIRST_COURT_LOCATION_OUCODE_INVALID`, field `firstCourtHearingLocation`, original value), set the field to null in the stored case, and accept the case. Found → kept unchanged. |
| FR-3 | `coredomain` (and `criminal-court-public-model`) bumped to the core-domain release that has the field. |
| FR-4 | Field absent → unchanged. XHIBIT unchanged. |
| FR-5 | Unit tests for FR-1, FR-2, FR-2a, FR-4. One IT: a LIBRA case with the field reaches Progression with `migrationSourceSystem.firstCourtHearingLocation`. Existing ITs stay green. |

## Acceptance criteria

- **AC-1:** GIVEN a LIBRA case whose `migrationSourceSystem` has `firstCourtHearingLocation`, WHEN it is
  accepted, THEN `initiate-court-proceedings` carries it in `prosecutionCases[0].migrationSourceSystem`.
- **AC-1a:** GIVEN a `firstCourtHearingLocation` not found in reference data, WHEN the case is validated,
  THEN the case is accepted, Progression receives `migrationSourceSystem` without it, and a "Case
  validation" warning with the original OU code is recorded for the reconciliation report.
- **AC-2:** GIVEN no `firstCourtHearingLocation`, THEN behaviour is unchanged.
- **AC-3:** GIVEN an XHIBIT case, THEN nothing changes.

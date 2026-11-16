# 02 — Design

- **Inputs:** `01-requirements.md`, [ADR-DD-43944](../../adrs/ADR-DD-43944-libra-first-court-hearing-location.md). Assumes the proposed answers to Q3 – Q4.

## Change

| # | File | Change | FR |
|---|------|--------|----|
| C-1 | `DesignSchemas/global/migrationSourceSystem.json` | Add `"firstCourtHearingLocation": {"description": "OU code of the court to which the case was originally sent by the prosecuting authority", "type": "string"}` after `migrationSourceSystemCaseIdentifier`. | FR-1 |
| C-2 | `criminal-court-public-model/src/main/resources/json/schema/global/migrationSourceSystem.json` | Same property, same position (keep this file's multi-line formatting). | FR-1 |

Nothing else. `criminal-court-public-model` packages the schema (C-2); the `MigrationSourceSystem` class
with `getFirstCourtHearingLocation()` / `withFirstCourtHearingLocation()` is generated from it in
`criminal-court-api-test-model` and by consumers' own code generation (FR-3).

## Order

1. This story (`dev/*` → `team/libra4`) after the version PR (#227) merges.
2. Progression (`team/libra4`) and pcfdlrm (`team/libra4`) bump `coredomain` to the published version.

## Tests

- `mvn clean install`: 260 tests, 0 failures; generated `MigrationSourceSystem` has the accessor, and the
  packaged `criminal-court-public-model` jar carries the field.
- No schema test suite exists for this file here; the consumer ITs (Progression
  `ACourtProceedingsInitiatedIT`, pcfdlrm `ReceiveMigratedCaseFileIT`) cover AC-1 and AC-2.

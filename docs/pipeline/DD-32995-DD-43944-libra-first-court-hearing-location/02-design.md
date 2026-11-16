# 02 — Design

- **Story:** [DD-43944](https://tools.hmcts.net/jira/browse/DD-43944)
- **Inputs:** `01-requirements.md`, [ADR-DD-43944](../adrs/ADR-DD-43944-libra-first-court-hearing-location.md).
  Q1 – Q6 answered in `01-requirements.md`. No production code changed at this stage.

## Change

| # | File | Change | FR |
|---|------|--------|----|
| C-1 | `pcfdlrm-domain-value-schema/.../migrated/migrated-migrationSourceSystem.json` | Add optional `firstCourtHearingLocation`: `{"type": "string", "minLength": 7, "maxLength": 7}`. | FR-1 |
| C-1a | New `FirstCourtHearingLocationValidationRule` (`validation/rules/`) | Input: the migration source system; `referenceDataQueryService.retrieveOrganisationUnits(code)` empty → `FIRST_COURT_LOCATION_OUCODE_INVALID` problem (field `FieldName.FIRST_COURT_HEARING_LOCATION`, original value); absent or found → `VALID`. | FR-2a |
| C-1c | `ProblemCode` and `FieldName` (`validation/`, `validation/rules/`) | Add `FIRST_COURT_LOCATION_OUCODE_INVALID` and `FIRST_COURT_HEARING_LOCATION("firstCourtHearingLocation")`. Problem codes are not in a JSON schema enum, so no schema change. | FR-2a |
| C-1b | `MigratedCaseFileAggregate.receiveMigratedCaseFile` | Run C-1a for the case; add its problem to the case "Case validation" warnings (`:306`); when it fails, rebuild `migratedCaseDetails.migrationSourceSystem` without `firstCourtHearingLocation` in the `receiveMigratedCaseFile` copy the aggregate already rebuilds (`:357`), so the stored `MigratedCaseValidatedCreationPending` and the case sent to Progression both drop it. | FR-2a |
| C-2 | `MigratedCaseToProsecutionCaseConverter.buildMigrationSourceSystem` | `.withFirstCourtHearingLocation(migrationSourceSystem.getFirstCourtHearingLocation())`. | FR-2 |
| C-3 | `pom.xml` | `coredomain.version` → the core-domain 17.104.x release with the field (published from core-domain `team/libra4`, cut from `release/17.104.x`). Check the matching `criminal-court-public-model` / schema classifier deps follow (`${coredomain.version}`). | FR-3 |

`Prosecution` (the case-rule input) does not carry `migrationSourceSystem`, so C-1a is called from the
aggregate with the migration source system rather than added to the `CcProsecutionValidationRuleProvider`
case rule sets. Clearing in the aggregate follows DD-43870: clearing only in the converter would leave
the unknown code in the stored events.

## Dependencies and order

1. Core-domain story (`dev/*` → `team/libra4`, cut from `release/17.104.x`): field added to
   `DesignSchemas/global/migrationSourceSystem.json`, and a version published from that branch.
2. Progression story (`dev/*` → `team/libra4`): `coredomain` bumped and **deployed** before pcfdlrm sends the field. Progression
   validates `initiate-court-proceedings` against the closed core schema, so an older Progression
   would reject a case carrying it.
3. This story (C-1 – C-3).
4. stagingdlrm story: bumps its pcfdlrm version and maps the field. Its schema changes can merge earlier.

C-1 can merge on its own before core is released: pcfdlrm would accept and record the field but not
send it on.

## Options considered

| Option | Why / why not |
|---|---|
| **Core `migrationSourceSystem` (chosen, Q1)** | Developer's decision; the value appears in Progression where the migration details live. |
| `ProsecutionCase.sendingCourt` via reference data | No core change, but not where the developer wants it. |
| Unknown OU code: reject the case | Developer chose accept + null + reconciliation report (Q2), as DD-43870. |
| Unknown OU code: pass through | Superseded by Q2. |

## Tests

- `FirstCourtHearingLocationValidationRuleTest`: found → `VALID`; not found → problem with the original
  value; absent → `VALID`, no lookup.
- `MigratedCaseFileAggregateTest`: unknown code → "Case validation" warning and
  `MigratedCaseValidatedCreationPending` without the field; known code → kept, no warning.
- `MigratedCaseToProsecutionCaseConverterTest`: field copied to `ProsecutionCase.migrationSourceSystem`; absent → absent.
- Schema: a `receive-migrated-case-file` payload with the field validates (extend an existing LIBRA fixture).
- `ReceiveMigratedCaseFileIT`: LIBRA payload with a known `migrationSourceSystem.firstCourtHearingLocation`
  → `initiate-court-proceedings` has `prosecutionCases[0].migrationSourceSystem.firstCourtHearingLocation`.
  IT reference-data stubs already cover court OU codes; add a row if the chosen code is not stubbed.

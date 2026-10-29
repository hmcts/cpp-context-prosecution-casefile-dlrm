# 03 — User Story

## DD-43888 — LIBRA allocation decision date validation

**As** the DLRM migration service
**I want** a LIBRA case rejected when an offence's allocation decision is invalid or has a missing or
future date
**So that** an allocation decision is not migrated without its date (CM, DD-43502).

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`.
- `ProblemCode`, `FieldName`, new `OffenceAllocationDecisionDateValidationRule`,
  `OffenceAllocationDecisionMotReasonValidationRule` and
  `OffenceAllocationDecisionValidationRule`, `CcProsecutionValidationRuleProvider`,
  `MigratedCaseFileAggregate` (see `02-design.md` C-1 – C-6).

### Acceptance criteria

See `01-requirements.md` FR-1 – FR-4 (incl. FR-1a). For LIBRA, a reject is `migrated-case-file-processed`,
`processingIsSuccessful=false`, with no received / creation-pending event:
1. Allocation decision with no date → "Missing or invalid allocation decision date".
2. Allocation decision with a future date → "Missing or invalid allocation decision date".
3. Allocation decision with a date today or earlier → accepted as today.
4. No allocation decision → accepted as today.
5. XHIBIT outcomes unchanged.
6. Allocation decision `motReasonId` not in CP ref data → "Invalid allocation decision".

### Definition of done

- [ ] C-1 – C-6 implemented; no other production change.
- [ ] Unit tests for the three new rules and the LIBRA aggregate rows (`02-design.md` Tests).
- [ ] `mvn clean install` green; `./runIntegrationTests.sh` green (plus one positive LIBRA allocation
      decision IT; no negative LIBRA IT).

### Out of scope

- Negative LIBRA ITs (unit level only).

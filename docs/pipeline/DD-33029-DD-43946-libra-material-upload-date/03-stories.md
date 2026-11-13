# 03 — User Story

## DD-43946 — LIBRA material upload date (pcfdlrm)

**As** a CP user viewing a migrated LIBRA case's materials
**I want** each material's "Uploaded" date to be the date it was uploaded to LIBRA
**So that** the case history is accurate, not the migration date.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`, base `team/libra4`.
- `02-design.md` C-1 – C-4.
- Depends on the stagingdlrm sub-task of DD-43946 (Function App reads the date from `manifest.json`), per
  [ADR-DD-43946](../adrs/ADR-DD-43946-libra-material-upload-date.md). This story can merge first:
  until the date is sent, behaviour is unchanged.

### Acceptance criteria

See `01-requirements.md` AC-1 – AC-3.

### Definition of done

- [x] Q1 – Q8 confirmed and recorded in `01-requirements.md`.
- [ ] C-1 – C-4 implemented. No other production change.
- [ ] Tests in `02-design.md`.
- [ ] `mvn clean install` green. `./runIntegrationTests.sh` green.

### Out of scope

- Function App and stagingdlrm service changes (stagingdlrm story).
- `material` context changes (Q2).
- Validating the date beyond the schema: the future-date check is in stagingdlrm (Q8).

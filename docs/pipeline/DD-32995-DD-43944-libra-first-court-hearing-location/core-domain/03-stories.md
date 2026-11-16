# 03 — User Story

## DD-43944 — firstCourtHearingLocation in core migrationSourceSystem

**As** the DLRM migration
**I want** core `migrationSourceSystem` to accept `firstCourtHearingLocation`
**So that** pcfdlrm can send a LIBRA case's first court to Progression.

### Scope

- One repo: `cpp-platform-core-domain`, branch `team/libra4`.
- `02-design.md` C-1 – C-2.

### Definition of done

- [ ] Q3 – Q4 confirmed and recorded in `01-requirements.md`.
- [ ] C-1 – C-2 implemented. `mvn clean install` green.
- [ ] The `-LIBRA4-SNAPSHOT` version the pipeline publishes on merge is recorded for Progression and pcfdlrm.

### Out of scope

- Progression, pcfdlrm and stagingdlrm changes (their own stories).

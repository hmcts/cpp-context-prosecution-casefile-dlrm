# 03 — User Story

## DD-43944 — LIBRA first court hearing location (pcfdlrm)

**As** a CP user viewing a migrated LIBRA case
**I want** the court the prosecuting authority first sent the case to
**So that** the case has a court even when no hearings were migrated.

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`, base `team/libra4`.
- `02-design.md` C-1, C-1a, C-1b, C-1c, C-2, C-3.
- Depends on the core-domain release and a deployed Progression bump (`02-design.md` order).
- Merges before the stagingdlrm story's mapping change.

### Acceptance criteria

See `01-requirements.md` AC-1 – AC-3.

### Definition of done

- [x] Q1 – Q6 confirmed and recorded in `01-requirements.md`.
- [ ] C-1 – C-3 (incl. C-1a, C-1b) implemented; tests in `02-design.md`.
- [ ] `mvn clean install` green. `./runIntegrationTests.sh` green.

### Out of scope

- Function App and stagingdlrm changes (stagingdlrm story).
- The core-domain change and the Progression `coredomain` bump (their own stories).
- Adding cases to the reconciliation report (manual process, as DD-43870).

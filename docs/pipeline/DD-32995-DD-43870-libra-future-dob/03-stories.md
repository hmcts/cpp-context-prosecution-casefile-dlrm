# 03 — User Story

## DD-43870 — Future defendant date of birth (LIBRA and XHIBIT)

**As** the DLRM migration service
**I want** a defendant's future date of birth cleared, with the case still accepted
**So that** CP does not show a negative age and the case is not blocked (ticket decision, DD-43493).

### Scope

- One repo: `cpp-context-prosecution-casefile-dlrm`.
- `ProsecutionCaseFileHelper` only (see `02-design.md` C-1 – C-2).
- LIBRA and XHIBIT (agreed 2026-11-06).

### Acceptance criteria

See `01-requirements.md` FR-1 – FR-5.
1. LIBRA or XHIBIT defendant DOB in the future → case accepted, DOB null in the payload sent to
   progression.
2. `DEFENDANT_DOB_IN_FUTURE` warning still recorded with the original date.
3. DOB of today or earlier, or absent → unchanged.
4. Parent/guardian DOB handling unchanged (LIBRA only, DD-43501).

### Definition of done

- [ ] C-1 – C-2 implemented. No other production change.
- [ ] Unit tests and the LIBRA and XHIBIT IT rows (`02-design.md` Tests).
- [ ] `mvn clean install` green. `./runIntegrationTests.sh` green.

### Out of scope

- Adding the case to the reconciliation report: a manual process, tracked in a separate sub-task of
  DD-43870.
- Future parent/guardian DOB for XHIBIT.

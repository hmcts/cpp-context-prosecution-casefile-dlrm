# 01 — Requirements

- **Story:** DD-43463 — see `00-input-brief.md`.
- **Scope:** pcfdlrm only, LIBRA only, happy path only. Nothing XHIBIT-related changes.

| ID | Requirement |
|----|-------------|
| FR-1 | A LIBRA case with materials in accepted bundles 1, 2, 3, 6, 8, 99 is accepted, and each material is sent to Material and then to Progression as a court document. Bundle 9 (Exhibits, Crown only) is out of scope. |
| FR-2 | Section, document type and case/defendant level come from reference data (`get-all-parent-bundle-section` → `get-all-document-type-access`), as today. No new fields from stagingdlrm. |
| FR-3 | A LIBRA case-level material (bundles 3, 8) reaches `material-ready-for-court-document` with its `caseDocument` category. Today this throws a NullPointerException in `acceptMigratedCase` (`MigratedCaseFileAggregate.java:668`). |
| FR-4 | In `acceptMigratedCase`, XHIBIT keeps the current code. For other sources: a defendant-level document goes to all case defendants (as today); a case-level document keeps its `documentCategory` unchanged. |
| FR-5 | Uploaded date: no change here (handled by Material). |
| FR-6 | Manual uploads after migration: no change (AC-3, existing CP process). |
| FR-7 | XHIBIT paths unchanged, proven by the existing tests plus one test on `acceptMigratedCase`. |
| FR-8 | Unit tests for LIBRA case-level and defendant-level materials. One positive LIBRA IT covering every accepted bundle. IT stubs gain the STE `WS`, `MCEB`, `UM` rows and bundle `3`. No negative LIBRA IT. Existing ITs stay green. |

## Acceptance criteria

- **AC-1:** GIVEN a LIBRA case with materials in bundles 1, 2, 3, 6, 8 or 99, WHEN it is accepted by CP,
  THEN each material is sent to Progression as a court document at the reference-data level.
- **AC-2:** GIVEN a LIBRA case-level material (bundle 3 or 8), WHEN the case is accepted, THEN the court
  document has a `caseDocument` category and no error is raised.
- **AC-3:** GIVEN an XHIBIT case with materials, WHEN it is accepted, THEN the output is as today.

## Open questions

1. ~~Defendant-level target~~ — **resolved (user, 2026-11-03):** keep today's behaviour, all case
   defendants (FR-4).
2. ~~Reference data~~ — **resolved (STE, 2026-11-03):** 1 IDPC, 2 MCEB, 6 UM, 99 PSJH = Defendant level;
   3 WS, 8 WS = Case level. One row per section code, no validity dates set. IT stubs copy these
   section names exactly.
3. ~~Unknown bundle codes~~ — **resolved (user, 2026-11-03):** a code outside the accepted set (e.g. 100)
   is a failure; today it throws `NotYetImplementedException` (`:637-638`). No change.
4. ~~Bundle 99 for LIBRA~~ — **resolved (user, 2026-11-03):** in scope; XHIBIT uses it today but nothing
   stops LIBRA using it. No code change.
5. **CC rules for LIBRA SJP cases** — for TA/TL, see `00-input-brief.md`. Not changed here.

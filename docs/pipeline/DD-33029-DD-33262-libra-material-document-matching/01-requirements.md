# 01 — Requirements

- **Story:** DD-33262 — see `00-input-brief.md`.
- **Scope:** pcfdlrm only, LIBRA only, happy path only. Nothing XHIBIT-related changes. Rejections,
  unmatched material and the bulk-scan route are DD-43983 (or a separate story).

## Decision

**Scope (developer, 2026-11-27):** the AC's happy path is met by today's pcfdlrm matching, which runs
prosecutioncasefile's own v1 CC material rules (document type from the section; defendant-level
documents matched by `prosecutorDefendantId`, then ASN; case not ejected) and sends matched material to
Progression (DD-43463). This story **proves** that end to end and closes small gaps; it does not route
migrated material through prosecutioncasefile or documentqueue.

## Gaps found

| # | Gap | Evidence |
|---|-----|----------|
| G1 | No test proves what Progression receives. The LIBRA IT stops at pcfdlrm's `material-ready-for-court-document` event; `progression.add-court-document` is stubbed but never verified. | `ReceiveMigratedCaseFileIT.java:236-288`; `MaterialReadyForCourtDocumentProcessor` |
| G2 | Defendant matching is nominal for LIBRA. A LIBRA material carries no defendant identifier (manifest and `migrated-material.json` have none in use), so pcfdlrm passes the **first defendant's id** as `prosecutorDefendantId` (`MigratedCaseFileAggregate.java:374`), the defendant-level rule always matches, and `acceptMigratedCase` attaches the document to **all** case defendants (DD-43463 FR-4). | `:374`, `:395`, `acceptMigratedCase` |
| G3 | V2-only live rules (file type, prosecutor, OU code) are not run. Each adds a rejection, whose outcome is DD-43983. | `CCMaterialsValidationRuleProviderV2` (prosecutioncasefile) |
| G4 | documentqueue never sees migrated material. On the live happy path `material-added` only clears queue items that a failure created, so a migrated case needs nothing. | prosecutioncasefile `MaterialEventProcessor`, documentqueue `CaseProcessor.java:125-145` |

## Requirements

| ID | Requirement |
|----|-------------|
| FR-1 | A LIBRA case with matched materials in accepted bundles 1, 2, 3, 6, 8, 99 results in one `progression.add-court-document` per material, carrying the reference-data `documentTypeId`, the case-level (`caseDocument.prosecutionCaseId`) or defendant-level (`defendantDocument.defendants`) category, and the Material `materials[].id`. |
| FR-2 | The existing LIBRA IT verifies FR-1 at the Progression boundary (the outbound command), not only at pcfdlrm's internal event. |
| FR-3 | No production change unless FR-2 exposes a defect. |
| FR-4 | G2, G3, G4 are recorded, not changed (see Open questions). |
| FR-5 | XHIBIT unchanged; existing unit tests and ITs stay green. |

## Acceptance criteria

- **AC-1:** GIVEN a LIBRA case with one material in each accepted bundle, WHEN Progression creates the
  case and pcfdlrm accepts it, THEN Progression receives one `add-court-document` per material with the
  reference-data document type and level.
- **AC-2:** GIVEN a case-level material (bundle 3 or 8), THEN its court document has a `caseDocument`
  for the migrated case and no `defendantDocument`.
- **AC-3:** GIVEN a defendant-level material, THEN its court document names the migrated case's
  defendant(s).
- **AC-4:** GIVEN an XHIBIT case, THEN behaviour is as today.

## Open questions (for TA/TL / LIBRA extract team)

1. **G2 — per-defendant matching.** Should LIBRA send a defendant identifier per material (e.g.
   `prosecutorDefendantId` in the manifest) so a defendant-level document is attached to the right
   defendant on a multi-defendant case? Until then it goes to all defendants (DD-43463 decision).
2. **G3 — V2 rules.** Are file-type / prosecutor / OU-code checks wanted for migrated material? If yes,
   with DD-43983, since their only effect is a rejection.
3. **Simon Bartlett's comment** — does LIBRA hold Court Store material at all, or should this be
   generated NOWs/extracts? (Assumed yes: DD-43463 is live for LIBRA bundles.)

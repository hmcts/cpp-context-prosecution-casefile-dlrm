# 00 — Input Brief

> **Self-contained** — everything an SDLC stage needs for this story is in this directory.

| | |
|---|---|
| Epic | [DD-33029](https://tools.hmcts.net/jira/browse/DD-33029) — Receive case material for migrated case |
| Story | [DD-33262](https://tools.hmcts.net/jira/browse/DD-33262) — (Happy path) Migrated case material must follow document matching flow > case material UI. 13 points, DLRM Sprint 69 |
| Related | DD-43983 — (Un-happy path) Court store migrated case material must follow document matching flow (New). DD-43463 — LIBRA case material happy path (delivered, `docs/pipeline/DD-33029-DD-43463-libra-case-material/`) |
| Repo | `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`). Base branch `team/libra4` |

## Epic framing

DD-33029 migrates the material a LIBRA case held in Court Store into CP with the case, so that it can
be viewed in the CP Case Material UI. A LIBRA submission (`case.json` + `manifest.json` + files) goes
through the stagingdlrm Function App and stagingdlrm to pcfdlrm. pcfdlrm validates the case and its
material, uploads each file to the Material context, and sends each one to Progression as a court
document once Progression has created the case.

## Request (as given)

**As an** authorised HMCTS user **I want** to migrate a magistrates' case's stored case materials to CP
**so that** all its documents are accessible as part of the case once the case has been migrated.

**AC 1 — Document matching flow for Court Store material for migrated cases.** GIVEN a case is being
migrated from LIBRA to CP AND it has case materials stored before migration, WHEN the case is migrated
AND the material is fetched AND migrated with the case, THEN the material undergoes a validation flow
(just as the CP retrieve or bulk scan route) AND, when correctly matched with the migrated case AND
there are no other validation errors, it is available in the Case Material UI for the migrated case.

**Ticket comments:**
- Simon Bartlett (2024-12-02): does LIBRA store material at all, or should this be generated NOWs/extracts?
- Dan O'Connor (2026-11-24): "We may have to break this into two, the bulk scan route may need to be another story."

## What "document matching" is on the live routes (`cpp-context-prosecution-casefile`)

Traced on `team/libra1`. "CP retrieve" is not a term used in code; the nearest is the CPS/CMS
document channel (`add-cps-material`).

1. **Intake.** Bulk scan and SPI/CPS uploads reach staging-prosecutors, which maps prosecuting
   authority + URN to a CP caseId and sends `prosecutioncasefile.add-material` (v1), `add-material-v2`
   or `add-cps-material` (`staging-prosecutors MaterialSubmittedProcessor.java:72-195`).
2. **Matching in the `ProsecutionCaseFile` aggregate.**
   - Case not yet accepted → `MaterialPending` (re-validated when the case is accepted; expires after
     28 days, 5 for bulk scan → `MATERIAL_EXPIRED` / `BulkscanMaterialRejected`).
   - Otherwise the CC rules run (`CCMaterialsValidationRuleProvider`): document type = reference data
     `documentsTypeAccess.section` → documentTypeId + category (case / defendant / application level);
     a defendant-level document must match a defendant by `prosecutorDefendantId`, falling back to
     ASN; case not ejected. V2 adds file type, prosecutor, OU code and application rules.
3. **Happy path.** `material-added` → `material.command.upload-file` → `material.material-added` →
   `progression.add-court-document` (v1) or `add-court-document-v2`, with the category built from
   reference data. `public.prosecutioncasefile.material-added` also clears the document from the
   document queue when its section is not `actionRequired`.
4. **Unhappy path.** `material-rejected` → bulk scan rejects/flags the document; CPS rejection or an
   unmatched case → `document-review-required` → documentqueue (review UI).
5. **Migrated material** has no special handling in prosecutioncasefile.

## Current behaviour in pcfdlrm (`team/libra4`)

- `MigratedCaseFileAggregate.receiveMigratedCaseFile` runs, per material, the **same v1 CC rule set**
  copied from prosecutioncasefile: `CCDocumentTypeValidationRule`, `CCDocumentDefendantLevelValidationRule`
  (prosecutorDefendantId then ASN), `CaseEjectValidationRule`
  (`validation/provider/CCMaterialsValidationRuleProvider.java`, called from
  `validateMaterialWithDocumentDetails`, `MigratedCaseFileAggregate.java:748-800`). The section comes
  from the LIBRA bundle code via `get-all-parent-bundle-section` (`addMaterial`, `:654-678`).
- Matched material → `MaterialAdded` → Material upload → `material-ready-for-court-document` →
  `progression.add-court-document` with the reference-data category (DD-43463). This is the AC's
  happy path, already live for LIBRA bundles 1, 2, 3, 6, 8, 99.
- **No pending state is needed**: the material arrives with the case, and is sent to Progression only
  after Progression creates it.
- **Any rejection throws** `NotYetImplementedException("Only happy path implemented now")` (`:677`);
  an unknown bundle code also throws (`:663`). `MaterialRejected` exists but is never applied. That is
  the unhappy path (DD-43983).
- pcfdlrm publishes no `material-added` / `document-review-required` public event, so documentqueue
  never sees migrated material.

## Gap between the AC and today

On the happy path, migrated LIBRA material is already validated with prosecutioncasefile's own CC
matching rules and shown in the Case Material UI when it matches. What is not like the live routes:
V2-only rules (file type, prosecutor, OU code), documentqueue involvement, and every failure outcome.
The last two belong to the unhappy path (DD-43983) and possibly a separate bulk-scan story (comment above).

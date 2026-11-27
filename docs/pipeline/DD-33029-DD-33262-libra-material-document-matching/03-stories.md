# 03 — Stories

One story, one repo (`cpp-context-prosecution-casefile-dlrm`), branched from `team/libra4`.

## DD-33262 — Prove LIBRA material reaches the Case Material UI via document matching

**As an** authorised HMCTS user
**I want** each Court Store material on a migrated LIBRA case to be matched against reference data and
sent to Progression as a court document
**so that** it can be seen in the Case Material UI for the migrated case.

### Acceptance criteria

- **AC-1 — one court document per material.** GIVEN a LIBRA case with one material in each accepted
  bundle (1, 2, 3, 6, 8, 99), WHEN Progression creates the case and pcfdlrm accepts it, THEN Progression
  receives exactly one `add-court-document` per material, whose `materialId` and `materials[0].id` are
  that material's id and whose `documentTypeId` is the reference-data id for the bundle's section.
- **AC-2 — case level.** GIVEN a bundle 3 or 8 material, THEN its court document has
  `caseDocument.prosecutionCaseId` = the migrated case id and no `defendantDocument`.
- **AC-3 — defendant level.** GIVEN a bundle 1, 2, 6 or 99 material, THEN its court document's
  `defendantDocument.defendants` lists the migrated case's defendant.
- **AC-4 — reference data drives the type.** GIVEN the material-added event pcfdlrm raises, THEN its
  `documentTypeId` and `documentCategory` match reference data for the bundle.
- **AC-5 — no regression.** All existing unit tests and ITs pass, including XHIBIT.

### Tasks

| # | Task | Design ref | AC |
|---|------|-----------|----|
| T1 | `MaterialReadyForCourtDocumentProcessorTest`: send a real `{materialId, courtDocument}` payload, and assert it is forwarded unchanged as `progression.add-court-document` | C3 | AC-1 |
| T2 | IT: add the bundle → `documentTypeId` map; assert it on each `MaterialAdded`; echo it via a new `createMaterialAddedPayload` overload | C1 | AC-4 |
| T3 | IT helper `addCourtDocumentRequestsByMaterialIdFor(caseId, expected)` on `ReceiveMigratedCaseFileHelper`, using `ProgressionStub.ADD_COURT_DOCUMENT_COMMAND` | C2 | — |
| T4 | Extend `shouldSendEveryAcceptedLibraBundleForCourtDocumentOnAccept` to await and assert the Progression requests per material | C2 | AC-1–3 |
| T5 | Run unit tests, then `ReceiveMigratedCaseFileIT`, then the full IT suite (`./runIntegrationTests.sh`) | — | AC-5 |
| T6 | Only if T4 exposes a defect: failing unit test, minimal fix, and a "Defect found" note in `02-design.md` | FR-3 | — |

### Definition of done

- T1–T5 complete, green locally. One squashed commit, a PR against `team/libra4` with a short description.
- No production code changed unless T6 was triggered.
- Open questions in `01-requirements.md` (G2 per-defendant id, G3 V2 rules, NOWs) raised with the TA/TL.
  They do not block this story.

### Out of scope

- Unhappy path, rejections, documentqueue and bulk scan: DD-43983, or a separate story per Dan O'Connor's
  comment.
- Per-defendant matching for multi-defendant LIBRA cases (G2).
- V2 rules (G3).

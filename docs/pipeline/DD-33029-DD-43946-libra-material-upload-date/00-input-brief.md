# 00 — Input Brief

- **Epic:** [DD-33029](https://tools.hmcts.net/jira/browse/DD-33029) — Receive case material for migrated case
- **Story:** [DD-43946](https://tools.hmcts.net/jira/browse/DD-43946) — Add the date that the case materials was uploaded to Libra to the CP Schema
- **Follows:** [DD-43891](https://tools.hmcts.net/jira/browse/DD-43891) — (Spike) Understand the behaviour of the case materials upload date (closed)
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`). Base branch `team/libra4`.
- **Other half:** the stagingdlrm story in `cpp-context-stagingdlrm`, same directory name. The shared
  decision is [ADR-DD-43946](../adrs/ADR-DD-43946-libra-material-upload-date.md) in this repo.

## Epic framing

LIBRA case materials are migrated with the case: the Function App picks up the files listed in the
submission's `manifest.json`, stagingdlrm passes them to pcfdlrm, pcfdlrm uploads each one to the
`material` context and then adds it to the case in `progression` as a court document
(DD-43463, merged on `team/libra4`). DD-43463 left the date out: "Uploaded date: handled on the
Material side. No date work in this story."

## Request

LIBRA materials migrated to CP must show the date they were uploaded to LIBRA (the date they became
case material), in the place where CP shows the "Uploaded" date and time for case materials today.
The CP schema needs to carry that date.

## Ticket comment

- **Ticket comment, 13 hours ago:** this also needs changes to the stagingdlrm Function App and
  service, and pcfdlrm, and the date carried through to material.

## Current behaviour (code, `team/libra4`)

- **stagingdlrm Function App:** `stagingdlrm.manifest.json` `files[]` has `fileName`, `fileType`,
  `documentType` only. `StagingDlrmCommandHelper` builds each material from them; no date.
- **stagingdlrm service:** canonical `migrated-material.json` already has an optional
  `receivedDateTime` (date-time), and `MigratedCaseConvertor` already copies it to pcfdlrm. Nothing
  sets it today, so it always arrives empty.
- **pcfdlrm command schema:** `migrated-material.json` already has optional `receivedDateTime`.
- **pcfdlrm aggregate:** `MigratedCaseFileAggregate` (`:372`) calls `addMaterial(..., ZonedDateTime.now(), ...)`,
  so `material-added.receivedDateTime` is the pcfdlrm receive time. `migratedMaterial.getReceivedDateTime()`
  is never read.
- **pcfdlrm processor:** `MaterialEventProcessor` puts `receivedDateTime` into the material upload's
  `ccMetadata`, and after `material.material-added` builds the court document with
  `materials[0] = { id, receivedDateTime }`. **No `uploadDateTime` is sent.**
- **progression:** `MaterialEnricher.getUploadDateTime` keeps a supplied `materials[].uploadDateTime`,
  otherwise sets it to now. `CourtDocumentQueryView` orders case documents by
  `material.uploadDateTime`. The core `material.json` (criminal-court-public-model 17.104.4) has both
  `uploadDateTime` and `receivedDateTime`.

So the "Uploaded" date for a migrated LIBRA material is today the time `progression` adds the court
document, which is when the case is accepted.

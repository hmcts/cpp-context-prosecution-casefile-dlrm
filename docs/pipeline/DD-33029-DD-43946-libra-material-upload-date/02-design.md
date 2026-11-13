# 02 — Design

- **Story:** [DD-43946](https://tools.hmcts.net/jira/browse/DD-43946)
- **Inputs:** `01-requirements.md`, [ADR-DD-43946](../adrs/ADR-DD-43946-libra-material-upload-date.md).
  No production code changed at this stage. Q1 – Q8 answered in `01-requirements.md`.

## Summary

pcfdlrm reads the date that stagingdlrm already forwards (`migratedMaterial.receivedDateTime`) and
sends it to `progression` as the court document's `materials[].uploadDateTime`. One additive optional
field on the `material-added` event. No new event, subscription or command.

## Change

| # | File | Change | FR |
|---|------|--------|----|
| C-1 | `pcfdlrm-domain-event/.../pcfdlrm.events.material-added.json` | New optional `uploadDateTime` (core `date-time`). | FR-1 |
| C-2 | `MigratedCaseFileAggregate.addMaterial` / `validateMaterialWithDocumentDetails` | Pass `migratedMaterial.getReceivedDateTime()` through and set `MaterialAdded.uploadDateTime` from it (null when absent). `receivedDateTime` stays `ZonedDateTime.now()`. | FR-1, FR-2, FR-5 |
| C-3 | `MaterialEventProcessor.uploadToMaterialContext` | When `uploadDateTime` is set, add it to `ccMetadata` as `uploadDateTime`. | FR-3 |
| C-4 | `MaterialEventProcessor.handleDocumentUploadedEvent` / `buildMaterials` | Read `uploadDateTime` from `ccMetadata` and, when present, add it to `materials[0]`. | FR-3, FR-4 |

`ccMetadata` is echoed back by `material.material-added`, which is how `receivedDateTime` already
reaches `buildMaterials`; `uploadDateTime` uses the same route (Q2).

## Options considered

| Option | Why / why not |
|---|---|
| **A. Separate `uploadDateTime`, set only when supplied (chosen)** | "Uploaded" is the only field that changes. XHIBIT and materials without a date keep today's behaviour. |
| B. Use the supplied date as `receivedDateTime` and set `uploadDateTime` = `receivedDateTime` always | Changes XHIBIT: its "Uploaded" would become the pcfdlrm receive time, not the accept time. |
| C. New `uploadedDateTime` field on the pcfdlrm command schema | The transport field `receivedDateTime` already exists end to end and is unused; a new field means a schema change in both repos for no gain. See the ADR. |
| D. Change the `material` context | Not needed (Q2): CP reads `progression`'s `uploadDateTime`. |

## Edge cases

- **Date only, no time:** rejected by the date-time schema; LIBRA sends a full UTC date-time with milliseconds (Q3).
- **Future date:** rejected upstream by the stagingdlrm Function App (Q8); pcfdlrm never receives one.
- **Several materials:** each carries its own date.
- **Replay:** `uploadDateTime` comes from the command payload, so it is stable on replay.

## Tests

- `MigratedCaseFileAggregateTest`: material with `receivedDateTime` → `material-added.uploadDateTime`
  set, `receivedDateTime` = now; without → `uploadDateTime` null.
- `MaterialEventProcessorTest`: `uploadDateTime` added to `ccMetadata` when present, absent otherwise;
  `materials[0].uploadDateTime` set from `ccMetadata`.
- `ReceiveMigratedCaseFileIT`: LIBRA payload with `materials[].receivedDateTime` → the
  `progression` court document has `materials[0].uploadDateTime` equal to it.

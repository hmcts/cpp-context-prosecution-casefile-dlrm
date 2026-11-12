# ADR — Carrying the LIBRA material upload date to CP

- **Status:** Accepted (2026-11-11)
- **Date:** 2026-11-11
- **Story:** [DD-43946](https://tools.hmcts.net/jira/browse/DD-43946) / epic [DD-33029](https://tools.hmcts.net/jira/browse/DD-33029)
- **Repos:** `cpp-context-stagingdlrm` (Function App + service) and `cpp-context-prosecution-casefile-dlrm`.
  This ADR is the single copy; the stagingdlrm half (a sub-task of DD-43946) links here.

## Context

CP shows an "Uploaded" date for each case material: `progression` court document
`materials[].uploadDateTime`, which `progression` sets to now unless one is supplied. Migrated LIBRA
materials should show the date they were uploaded to LIBRA. Nothing in the pipeline carries that date
today, although an optional `receivedDateTime` already exists, unused, on the migrated material in
both stagingdlrm's canonical schema and pcfdlrm's command schema, and stagingdlrm already copies it
across.

## Decision

1. **Input:** LIBRA sends the date per file in `manifest.json` as optional `files[].uploadedDateTime`,
   ISO-8601 UTC date-time with milliseconds, e.g. `2026-11-11T14:04:07.107Z`.
2. **stagingdlrm:** the Function App validates it (`stagingdlrm.manifest.json`) and writes it to the
   material's existing `receivedDateTime`. The service passes it on unchanged (it already does).
3. **pcfdlrm:** reads `migratedMaterial.receivedDateTime` as the source upload date, records it as
   `material-added.uploadDateTime`, and sends it to `progression` as `materials[].uploadDateTime`.
   pcfdlrm's own `receivedDateTime` keeps meaning "received by pcfdlrm".
4. **Future date:** not allowed. The stagingdlrm Function App rejects the whole submission as an
   error submission. pcfdlrm does no extra check.
5. **Absent date:** today's behaviour everywhere. XHIBIT does not send the field and is unchanged.

## Consequences

- No schema change between stagingdlrm and pcfdlrm; the only new fields are the manifest input and
  the optional `material-added.uploadDateTime` event field.
- The transport field is named `receivedDateTime` but carries the source upload date between the two
  services. Each repo's design states this.
- The repos can merge in either order: each side is a no-op until the other is live.

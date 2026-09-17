# 00 — Input Brief

- **Epic:** [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806)
- **Story:** [DD-35807](https://tools.hmcts.net/jira/browse/DD-35807) — "Derive the conviction date
  (Indicated guilty and guilty pleas) Automation Journey"
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Branch:** not yet created

> **Provenance note.** Title and acceptance criteria below are as pasted by the requester
> directly (no Jira fetch — Atlassian MCP is not authenticated in this session, and wasn't
> needed since the ticket text was supplied verbatim). No free-text "Description" body was
> supplied beyond the AC block. Reconcile against the live DD-35806/DD-35807 ticket before
> sign-off, particularly the scope call in `01-requirements.md`.

## Acceptance criteria (as given)

**AC 1 — Derive the conviction date (indicated Guilty plea)**
GIVEN a migrated LIBRA case is created on Common Platform AND the future hearing is created in CP
WHEN the case detail has an indicated guilty plea and date against any offence on the case
THEN the backend sets the indicated guilty plea date as the conviction date for that offence.

**AC 2 — Derive the conviction date (Guilty plea)**
GIVEN a migrated LIBRA case is created on Common Platform AND the future hearing is created in CP
WHEN the case detail has a guilty plea and date against any offence on the case
THEN the backend sets the guilty plea date as the conviction date for that offence.

## Where this happens (from investigation)

Conviction-date derivation from a guilty plea already exists in production code:
`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.getConvictionDate()`
(`pcfdlrm-event/pcfdlrm-event-processor/.../convertor/...Converter.java:551-576`) returns the
migrated plea's `pleaDate` whenever the reference-data `pleaTypeGuiltyFlag` for that offence's
plea is `"Yes"` — which is true for both `GUILTY` and `INDICATED_GUILTY` reference-data rows, and
is not gated on `migrationSourceSystemName`, so it already fires for LIBRA.

This is unit-tested, but only for **XHIBIT** and only at the converter-unit level:
`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverterTest.shouldSetConvictionDateAsPleaDateWhenPleaIsGuilty`
(line 260, `@CsvSource({"G", "IG"})`).

At the **integration-test / automation** level (`pcfdlrm-integration-test`, the "journey" tests),
there is a real gap for LIBRA:
- `pcfdlrm.command.receive-migrated-case-file-libra-journey.json` already carries a `GUILTY`
  plea (reference-data id `7fbc9a21-...-8c01`, guilty flag `Yes`, plea date `2024-06-09`), but its
  expected fixture `json/xhibit/initiate-court-proceedings/libra-journey.json` does not assert
  `convictionDate` at all — **AC-2 is exercised but not verified**.
- `pcfdlrm.command.receive-migrated-case-file-libra-indicated-plea.json` only covers
  `INDICATED_NOT_GUILTY` (guilty flag `No` — correctly no conviction date). There is **no LIBRA
  fixture at all for `INDICATED_GUILTY`** (reference-data id `9a1e0d34-...-3c2b`, guilty flag
  `Yes`) — **AC-1 has no automation coverage**.

This reads as a test-automation ("Automation Journey") story closing an existing coverage gap,
not a production-code change. See open questions in `01-requirements.md`.

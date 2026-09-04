# 00 — Input Brief

- **Epic:** [DD-34567](https://tools.hmcts.net/jira/browse/DD-34567)
- **Story:** [DD-34568](https://tools.hmcts.net/jira/browse/DD-34568)
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)
- **Branch:** `dev/dd-34568`

> **Provenance note.** The HMCTS Jira (`tools.hmcts.net/jira`) sits behind SSO and could
> not be read by the tooling that produced this pipeline. The brief below is reconstructed
> from (a) the requester's instruction and (b) a source-cited investigation of the codebase.
> **Reconcile this against the actual DD-34567 / DD-34568 ticket text before sign-off** —
> in particular the open questions in `01-requirements.md`.

## Epic framing (DD-34567)

Migrated prosecution case files must carry a defendant's **indicated plea** through to the
`progression` context in the shape `progression` expects. The courts `Offence` model has
carried a dedicated `indicatedPlea` object (distinct from `plea`) since interface version
`criminal-court-public-model` 17.104.x, but `pcfdlrm` has never populated it — every migrated
plea, indicated or not, is emitted as a `plea`. This epic closes that gap so that indicated
pleas drive the downstream allocation decision correctly.

## Story ask (DD-34568)

> If the plea value is `INDICATED_GUILTY` or `INDICATED_NOT_GUILTY`, populate the
> `indicatedPlea` object before sending to `progression`, rather than the `plea` object.

## Where this happens (from investigation)

Plea data does **not** travel on the public event `public.pcfdlrm.migrated-case-file-processed`
(that event is status-only: `submissionId`, `caseId`, `caseUrn`, `processingIsSuccessful`,
`description`). Plea data reaches `progression` on the **`progression.initiate-court-proceedings`**
command, built by `MigratedCaseReceivedProcessor` via a converter chain that terminates in:

`pcfdlrm-event/pcfdlrm-event-processor/.../convertor/ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.java`

That converter calls `.withPlea(...)` on every offence and never calls `.withIndicatedPlea(...)`.

## Key reference facts

- Migrated `pleaValue` is sourced from reference data (`PleaReferenceData.getPleaValue()`), in
  UPPER_SNAKE form: `GUILTY`, `NOT_GUILTY`, `INDICATED_GUILTY`, `INDICATED_NOT_GUILTY`, …
- Courts `IndicatedPlea` (from `criminal-court-public-model` 17.104.4) requires:
  `offenceId`, `indicatedPleaDate`, `indicatedPleaValue` (enum
  `INDICATED_GUILTY|INDICATED_NOT_GUILTY|NO_INDICATION`), and **`source`** (enum
  `ONLINE|IN_COURT`). `originatingHearingId` is optional.
- `source` has no counterpart in migrated data — see ADR
  `docs/pipeline/adrs/ADR-DD-34568-indicated-plea-source.md`.

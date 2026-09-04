# ADR — `indicatedPlea.source` for migrated cases

- **Status:** Proposed (assumption pending PO / `progression` confirmation)
- **Date:** 2026-09-03
- **Story:** [DD-34568](https://tools.hmcts.net/jira/browse/DD-34568) /
  epic [DD-34567](https://tools.hmcts.net/jira/browse/DD-34567)
- **Repo:** `cpp-context-prosecution-casefile-dlrm`

## Context

The courts `indicatedPlea` object (`criminal-court-public-model` 17.104.4) requires a `source`
field, enum `ONLINE | IN_COURT`, described as the origin of the plea indication used to drive an
allocation decision in the magistrates' court.

Migrated prosecution case files carry **no** plea-channel information — the migrated plea shape
(`pcf-plea.json`) has only `pleaDate` and `pleaValue`, and reference data
(`PleaReferenceData`) adds only type/guilty-flag metadata. There is therefore nothing to map
`source` from.

Because `source` is a required field, the `indicatedPlea` object cannot be emitted without a
value, so a default must be chosen.

## Decision

Default `indicatedPlea.source` to **`IN_COURT`** for all migrated cases.

Rationale: migrated cases originate from the legacy court estate (LIBRA / XHIBIT). `ONLINE`
represents the newer digital plea channel that did not produce the legacy records being migrated,
so `IN_COURT` is the more faithful representation of a legacy indicated plea.

## Consequences

- Every migrated indicated plea reaches `progression` with `source = IN_COURT`.
- If `progression` treats `source` differently by channel (e.g. for allocation logic or audit),
  this default must be validated with that team — it is the primary open question in
  `01-requirements.md`.
- If confirmation says otherwise, the change is a one-line edit in
  `ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter.convertIndicatedPlea(...)`.

## Alternatives considered

- **`ONLINE`** — rejected; misrepresents legacy court-originated pleas as digital-channel pleas.
- **Omit `source`** — not possible; schema-required, would 500 on dispatch to `progression`.
- **Derive from migration source system (LIBRA/XHIBIT)** — both are court systems, so both would
  map to `IN_COURT` anyway; adds mapping complexity for no behavioural difference.

# 00 — Input Brief

- **Epic:** [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806)
- **Story:** [DD-43296](https://tools.hmcts.net/jira/browse/DD-43296) — "Derive the conviction date
  (Guilty verdict) Automation journey"
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)

> ACs below as supplied verbatim — no Jira fetch (Atlassian MCP not authenticated this session).
> Reconcile against the live ticket before sign-off.

## Acceptance criteria (as given)

**AC-1 — Derive the conviction date (verdict of guilty):** GIVEN a migrated LIBRA case is created
on Common Platform, AND a future hearing is created in CP, WHEN the case has a not-guilty plea and
a guilty verdict against any offence on the case, THEN the backend sets the conviction date for
that offence to the date the guilty verdict was made.

## Investigation

Sibling story [DD-35807](../DD-35806-DD-35807-conviction-date-derivation/) covered the *plea*-driven
conviction date path and explicitly scoped the *verdict*-driven path out ("already covered by
existing tests"). That turned out to be only partly true — see `01-requirements.md`.

`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter` already derives `convictionDate` from
a guilty verdict when no guilty plea applies:
- `deriveConvictionDateFromVerdict()` (`...Converter.java:617`) sets `convictionDate` to
  `verdict.getVerdictDate()` whenever `verdictType.category == "Guilty"`.
- `convertVerdict()` (`...Converter.java:470`) nulls the verdict entirely whenever the offence's
  plea is `GUILTY` — so this path only ever fires for a non-guilty plea, matching the AC.
- Logic is source-system-agnostic (no XHIBIT/LIBRA branching in the converter).

Unit-tested for XHIBIT only (`shouldSetConvictionDateFromVerdictWhenGuiltyVerdict`). **No IT
coverage exists for this path at all**, for any source system:
- No expected fixture under `pcfdlrm-integration-test/.../json/xhibit/initiate-court-proceedings/`
  references `"verdict"`.
- The one command fixture that pairs a plea + verdict on the same offence
  (`pcfdlrm.command.receive-migrated-case-file.json`) uses a `GUILTY` plea, so the verdict is
  nulled before `deriveConvictionDateFromVerdict` ever runs.
- The only `verdict-types` reference-data stub in the suite
  (`referencedata.query.verdict-types.json`) has `category: "Not Guilty but Guilty of alternative
  offence"`, not `"Guilty"` — it can never satisfy the guilty-verdict check even if referenced.
- `VerdictDataRefDataEnricher.process()` filters verdict-type reference data on
  `jurisdiction == (XHIBIT ? CROWN : MAGISTRATES) || EITHER || MAGISTRATES`. For a LIBRA case a new
  "Guilty" verdict-type stub row would need `jurisdiction: MAGISTRATES` or `EITHER` to pass — not
  yet confirmed empirically (DD-35807's equivalent plea-side bug only surfaced on a real Docker IT
  run, not from static reading).

Likely scoped the same way as DD-35807: an automation-coverage gap, not a production-code change —
to be confirmed once an IT run is actually attempted.

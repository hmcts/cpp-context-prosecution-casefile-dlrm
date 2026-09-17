# 02 — Design

- **Story:** [DD-35807](https://tools.hmcts.net/jira/browse/DD-35807) (epic
  [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806))
- **Change scope:** `pcfdlrm-integration-test` fixtures + one IT parametrization only. No
  production code, no schema/RAML/subscriptions-descriptor changes — the derivation logic already
  exists and is source-system-agnostic (`00-input-brief.md`).

## Layer analysis (the three-layer rule)

N/A for this story — nothing in command side, event listener, or event processor changes. The
existing event-processor converter (`ProsecutionCaseFileMigratedOffenceToCourtsOffenceConverter`)
is exercised as-is by the new/extended IT scenarios; it is not modified.

## Components touched

All under `pcfdlrm-integration-test/src/test/`:

| File | Change |
|------|--------|
| `resources/json/xhibit/initiate-court-proceedings/libra-journey.json` | Extend the existing offence to assert `convictionDate` (AC-2, guilty). |
| `resources/command-json/pcfdlrm.command.receive-migrated-case-file-libra-indicated-guilty-plea.json` | **New.** Clone of `...-libra-indicated-plea.json` with the plea `id` swapped to the `INDICATED_GUILTY` reference-data row (`9a1e0d34-5b2c-4f8e-bd6c-7a9f1e8d3c2b`, guilty flag `Yes`) instead of `INDICATED_NOT_GUILTY`'s. |
| `resources/json/xhibit/initiate-court-proceedings/libra-indicated-guilty-plea.json` | **New.** Clone of `libra-indicated-plea.json` with `indicatedPlea.indicatedPleaValue = INDICATED_GUILTY` and a `convictionDate` added (AC-1, indicated guilty). |
| `java/.../it/ReceiveMigratedCaseFileIT.java` | Add a third `@CsvSource` row to `receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings`, pairing the new command/expected fixtures above. |

No new IT class — this reuses the existing "journey" parametrized test per FR-3, matching the
established pattern XHIBIT/LIBRA scenarios already use in this method.

## Expected values — design assumption, to be confirmed against a real run

`getConvictionDate()` returns the plea date verbatim (no "default to today" fallback — that
fallback is only in `convertPlea`/`convertIndicatedPlea` for *non*-guilty pleas). So on paper:

- **`libra-journey` (guilty, plea date `2024-06-09`):** `convictionDate: "2024-06-09"`.
- **New `libra-indicated-guilty-plea` (indicated-guilty, plea date `2024-06-09`):**
  `indicatedPlea.indicatedPleaDate: "2024-06-09"` (unchanged from the not-guilty fixture's shape)
  **and** `convictionDate: "2024-06-09"` at the offence level.

`WholePayloadMatcher` is a **STRICT** whole-payload compare (`pcfdlrm-test-support`) — an
undeclared key in the actual payload fails the match, exclusions only skip *value* comparison for
keys already present in both. `libra-journey.json` today declares neither `plea` nor
`convictionDate` on its offence, yet it's a `GUILTY` plea — which should already populate both
per the converter. That mismatch means either the fixture is currently under-asserting real output
(the FR-1 gap this story exists to close) or plea reference-data enrichment doesn't resolve for
this fixture's ids for some reason not visible from static reading alone.

**This must be resolved empirically, not assumed** (per `feedback: iterate-one-real-failure-at-a-time`):
Stage 5 will first re-run the *current, unmodified* `libra-journey` case against the Docker IT
stack to see the real actual payload, then edit the expected fixture to match reality — adding
`convictionDate` and, if the real payload has it too, a `plea` object. The same empirical check
applies to the new `libra-indicated-guilty-plea` fixture before locking in its expected JSON.

## Test strategy

1. Baseline: run `receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings[libra-journey]`
   unmodified, capture the actual `initiate-court-proceedings` payload for offence
   `550e8400-...-40001`.
2. Update `libra-journey.json` to add `convictionDate` (and `plea`, if the baseline shows it) —
   re-run until green.
3. Add the new command + expected fixture pair and the third `@CsvSource` row for
   `libra-indicated-guilty-plea`; run, then reconcile the expected `indicatedPlea`/`convictionDate`
   values against the real payload — re-run until green.
4. No production code change expected. If step 1 or 3 reveals the converter is *not* actually
   deriving the conviction date for one of these cases, stop and flag it at this gate rather than
   silently loosening the assertion — that would be a real defect, out of this story's
   test-automation-only scope per FR-4.
5. Full local check before PR: `mvn clean && ./runIntegrationTests.sh` (per the hard rule for
   endpoint-adjacent IT coverage), plus the unaffected `mvn clean install` unit-test run.

## Open questions carried from Stage 1

None newly opened. Stage-1 open questions 2–4 (plea-value breadth, verdict-derived dates, XHIBIT
parity) remain out of scope, unchanged by this design.

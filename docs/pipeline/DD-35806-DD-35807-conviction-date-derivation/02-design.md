# 02 — Design

- **Story:** [DD-35807](https://tools.hmcts.net/jira/browse/DD-35807) (epic
  [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806))
- **Change scope:** `pcfdlrm-integration-test` fixtures + one IT parametrization + one shared
  stub-data record. No production code, no schema/RAML/subscriptions-descriptor changes.

## Layer analysis

N/A — the existing event-processor converter is exercised as-is; not modified.

## Components touched

| File | Change |
|------|--------|
| `resources/json/xhibit/initiate-court-proceedings/libra-journey.json` | Added `convictionDate`, `convictingCourt`, `plea` to the offence (AC-2). |
| `resources/command-json/pcfdlrm.command.receive-migrated-case-file-libra-indicated-guilty-plea.json` | **New.** Clone of `...-libra-indicated-plea.json`, plea `id` swapped to the `INDICATED_GUILTY` row (`9a1e0d34-5b2c-4f8e-bd6c-7a9f1e8d3c2b`, guilty flag `Yes`). |
| `resources/json/xhibit/initiate-court-proceedings/libra-indicated-guilty-plea.json` | **New.** `indicatedPleaValue: INDICATED_GUILTY`, plus `convictionDate` and `convictingCourt` (AC-1). |
| `resources/stub-data/referencedata.query.plea-types.json` | Guilty (`G`) plea-type row: `jurisdiction` `CROWN` → `EITHER`. |
| `java/.../it/ReceiveMigratedCaseFileIT.java` | Third `@CsvSource` row pairing the new fixtures. |

No new IT class (FR-3) — reuses the existing "journey" parametrized test.

## Root cause (found empirically, not by static reading)

`PleaDataRefDataEnricher.process()` only keeps a plea reference-data row if its `jurisdiction`
matches the source system (XHIBIT→`CROWN`, else→`MAGISTRATES`), or is `EITHER`, or
`pleaTypeCode == "IG"`. The shared plain-`GUILTY` row (id `7fbc9a21-...-8c01`) was
`jurisdiction: CROWN` — so for a LIBRA case it was filtered out entirely, and `getConvictionDate()`
never ran. This wasn't visible from reading the converter alone; it only surfaced on the first
Docker IT run against the unmodified `libra-journey` fixture (`convictionDate` missing).

Fix: widened that row to `jurisdiction: EITHER` — passes the filter for any source system, and is
arguably the more correct value (a Guilty plea isn't jurisdiction-specific). Safe for the ~15 other
XHIBIT fixtures sharing that id, since `EITHER` was already one of the filter's pass conditions;
confirmed by a full `./runIntegrationTests.sh` run (28/28 green, no regressions).

Fixing the stub also correctly resolved `plea`/`convictingCourt` for `libra-journey` and
`convictingCourt` for `libra-indicated-guilty-plea` (guilty status also drives court-centre
derivation from the hearing's `courtHearingLocation`, `getConvictingCourt()`). These fields were
absent from both fixtures until this fix landed and had to be added. `convictingCourt` resolves to
the same fixed record in both cases — the default org-unit and enforcement-area WireMock stubs
return one record regardless of the code queried:
```json
{ "id": "f8254db1-...-b87fde5a0a23",
  "lja": { "ljaCode": "1080", "ljaName": "Bedfordshire Magistrates' Court", "welshLjaName": "WELSH_NAME" },
  "name": "Port Talbot", "welshName": "Welsh Name" }
```

## Test strategy (as executed)

1. Baseline run against unmodified fixtures → 2 real failures: `libra-journey` missing
   `convictionDate`; `libra-indicated-guilty-plea` had an unexpected `convictingCourt`.
2. Root-caused to the stub jurisdiction filter (above) — fixed the one stub row.
3. Rerun surfaced the follow-on `plea`/`convictingCourt` gaps (previously masked by the jurisdiction
   bug) — added them to both fixtures.
4. Final rerun: 28/28 green. No production code touched (FR-4 held).

## Open questions carried from Stage 1

None newly opened. Stage-1 questions 2–4 remain out of scope, unaffected.

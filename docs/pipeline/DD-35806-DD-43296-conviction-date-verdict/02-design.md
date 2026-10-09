# 02 — Design

- **Story:** [DD-43296](https://tools.hmcts.net/jira/browse/DD-43296) (epic
  [DD-35806](https://tools.hmcts.net/jira/browse/DD-35806))
- **Change scope (as executed):** `pcfdlrm-integration-test` fixtures + one IT parametrization row
  + one new verdict-type stub-data row. No production code, no schema/RAML/subscriptions-descriptor
  changes — confirmed against a real Docker IT run.

## Layer analysis

N/A — the existing event-processor converter (`deriveConvictionDateFromVerdict`,
`convertVerdict`) is exercised as-is; not modified unless the IT run proves otherwise.

## Components added (as executed)

| File | Change |
|------|--------|
| `resources/stub-data/referencedata.query.verdict-types.json` | **New row.** Category `"Guilty"`, `categoryType: "GUILTY"`, `verdictCode`/`cjsVerdictCode` `"G"`, `jurisdiction: EITHER` — passes `VerdictDataRefDataEnricher`'s filter for LIBRA on the first try, no jurisdiction bug this time (open question 2 resolved). |
| `resources/command-json/pcfdlrm.command.receive-migrated-case-file-libra-guilty-verdict.json` | **New.** Clone of `...-libra-indicated-plea.json`, offence's `plea.id` swapped to the real `NOT_GUILTY` reference-data row (`5f3a1e9d-0c2b-4d8e-bd6c-1a7f2b5c9d4e` — plain not-guilty, not indicated, per AC wording), plus a `verdict` block (`id` = new stub row above, `verdictDate` set) on the same offence. |
| `resources/json/xhibit/initiate-court-proceedings/libra-guilty-verdict.json` | **New.** Expected output: offence carries `plea` (`NOT_GUILTY`, not `indicatedPlea`), a `verdict` block (`verdictType` including `categoryType`, per the actual IT run — see root cause below), `convictionDate` equal to the verdict date, and `convictingCourt` resolved via the guilty-verdict branch of `getConvictingCourt()`. |
| `java/.../it/ReceiveMigratedCaseFileIT.java` | Fourth `@CsvSource` row on `receiveMigratedCaseFileWithoutMaterialInitiatesCourtProceedings`, pairing the two new fixtures. |

No new IT class (FR-3) — reuses the existing "journey" parametrized test, same as DD-35807.

## Root cause (found empirically, not by static reading)

No jurisdiction bug this time — the guess in `EITHER` held. The one real failure on first run was
different: `convertVerdict()` unconditionally sets `verdictType.categoryType` from the reference
data (`...Converter.java:501-508`), but the expected fixture was drafted against an older
event-processor unit-test JSON sample that happened not to populate that field. Actual IT output
included `"categoryType": "GUILTY"` under `verdict.verdictType`; the expected fixture didn't have
it → one assertion failure (`Unexpected: categoryType`). Fixed by adding the field to the expected
fixture — no production code touched.

## Test strategy (as executed)

1. Added the new stub row + fixture pair + `@CsvSource` row.
2. First run (`./runIntegrationTests.sh`, full module): 28/29 — one failure, the `categoryType`
   gap above.
3. Fixed the expected fixture; reran just `ReceiveMigratedCaseFileIT` (env already up) — 28/28
   green.
4. Final full-module run (`failsafe:integration-test` + `failsafe:verify`, no filter) — 29/29
   green, no regressions. No production code touched (FR-4 held).

## Open questions carried from Stage 1

1. **Scope.** Resolved — automation-only, confirmed. No production defect found.
2. **Verdict-type stub jurisdiction.** Resolved — `EITHER` passed the LIBRA filter on the first
   real run; no fix needed.
3–5. Unchanged from Stage 1 — not affected by this design.

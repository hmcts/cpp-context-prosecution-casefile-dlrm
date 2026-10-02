# 00 — Input Brief

- **Epic:** [DD-32995](https://tools.hmcts.net/jira/browse/DD-32995)
- **Story:** [DD-43499](https://tools.hmcts.net/jira/browse/DD-43499) — "Business validation for Case
  level items"
- **Repo:** `cpp-context-prosecution-casefile-dlrm` (`pcfdlrm`)

> AC taken from the ticket export supplied by the user — no Jira fetch. Reconcile against the live
> ticket before sign-off.

## Acceptance criteria (as given)

**AC-1 — Case-level business validation:** GIVEN a case received onto CP via DLRM LIBRA migration,
WHEN validation runs for the case elements and a business rule is not met, THEN the system rejects
or accepts the case per the table below.

> The ticket's note "invalid entries should be nulled and follow the required behaviour for missing
> fields" is **wrong** per the user — BA to correct. Not used as a requirement.

| Field | Format | Business rule | SJP / Summons / Charge / Postal Req | Ref data | Missing → |
|---|---|---|---|---|---|
| `prosecutingAuthority` | A7 | Valid CJS OU code; CPS use cpsOrg (A codes) | M / M / M / M | prosecutingAuthority, cpsOrg | Reject |
| `originatingOrganisation` | A7 | Valid CJS OU code | M / M / M / M | prosecutingAuthority (OU code) | Reject |
| `initiationCode` | A1 | C, Q, J, S (R — TBC) | M / M / M / M | — | Reject |
| `prosecutorCaseReference` | A36 | Unique to prosecutor; `[A-Za-z0-9-]` | M / M / M / M | — | Generate a reference, accept |
| `migrationSourceSystemCaseIdentifier` | A100 | Unique across all migrated cases (LIBRA case-area prefix TBC) | M / M / M / M | — | Reject |
| `migrationSourceSystemName` | A6 | `LIBRA` | M / M / M / M | — | Reject |
| `cpsOrganisation` | A7 | — | O / O / O / O | cpsOrg | Accept |
| `informant` | A92 | — | O / M / O / O | — | Reject for Summons, accept otherwise |
| `caseMarkers` | Array | CJS descriptive/procedural markers | O / O / O / O | — | Accept |

## Investigation

Split: **stagingdlrm** does schema validation (API level), **pcfdlrm** does business validation.
stagingdlrm read from `origin/team/libra1`; Azure Functions intake not considered.

### stagingdlrm — API-level schema (`stagingdlrm-domain-value-schema`)

`receive-migrated-case-submission.json` → `migrated/migrated-case.json` → `case-details.json`,
`pcf-prosecutor.json`, `migrationSourceSystem.json`. Schema failure never reaches pcfdlrm.

| Field | Constraint today | Gap vs ticket |
|---|---|---|
| `prosecutingAuthority` | required, length 7 | — |
| `originatingOrganisation` | required, length 7 | — |
| `initiationCode` | required, enum C/Q/J/R/O/S; LIBRA rule engine allows C/Q/J/R/S | R allowed (ticket: TBC) |
| `prosecutorCaseReference` | required, maxLength 36 | no `[A-Za-z0-9-]` pattern; required, so "generate when missing" unreachable |
| `migrationSourceSystemCaseIdentifier` | required, no maxLength | no A100 limit; no uniqueness check |
| `migrationSourceSystemName` | required, enum LIBRA/XHIBIT | — |
| `cpsOrganisation` | optional, length 7 | — |
| `informant` | optional, maxLength 92; **not mapped** to pcfdlrm | no Summons-conditional check; never reaches pcfdlrm |
| `caseMarkers` | optional, `markerTypeCode` maxLength 3 | — |

### pcfdlrm — business validation

**For LIBRA, case-level problems are computed then discarded.** `MigratedCaseFileAggregate`
(~L214, ~L275) only acts on `caseProblems` when `isXhibit(...)`. Today no case-level rule can
reject a LIBRA case.

LIBRA rule set (`CcProsecutionValidationRuleProvider` ~L127-144): CaseInitiation,
ProsecutorReferenceData, CaseMarkers, PoliceForceCode.

| Field | Rule today | Gap |
|---|---|---|
| `prosecutingAuthority` | `ProsecutorReferenceDataValidationRule` (refdata prosecutor by OU code) | Ignored for LIBRA |
| `originatingOrganisation` | none | No OU-code check |
| `initiationCode` | `CaseInitiationValidationRule` (refdata initiation-types) | Ignored for LIBRA |
| `caseMarkers` | `CaseMarkersValidationAndEnricherRule` | Invalid markers silently dropped for LIBRA |
| `informant` | none; not in pcfdlrm schema | — |

**XHIBIT precedent (invalid prosecuting authority):** rule raises `PROSECUTOR_OUCODE_NOT_RECOGNISED`
→ aggregate (~L240) applies `migrated-case-file-processed` with `processingIsSuccessful=false`,
description "Invalid Prosecuting Authority", and returns early → republished as
`public.pcfdlrm.migrated-case-file-processed`. Null `prosecutingAuthority` is unreachable (staging
schema requires it).

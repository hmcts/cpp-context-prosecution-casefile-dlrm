# Design — DD-43905

A plain `git merge origin/main` into a branch cut from `team/25.104.x`. Conflicts were resolved as below.

| Conflict | Resolution |
|---|---|
| Project/parent `<version>` in 24 poms | Kept `25.104.26-DLRMJ25-SNAPSHOT` |
| `progression.version` (`17.0.297` vs `17.104.299`) | Took main's, then bumped to `17.104.306` — latest release, required by the interface enforcer |
| `sjp.version` (auto-merged to main's `17.104.193`) | Bumped to `17.104.197` for the same enforcer |
| `azure-pipelines.yaml` agent (`ubuntu-j25` vs `ubuntu24-j17`) | Kept `ubuntu-j25` |
| `CLAUDE.md` | Kept the Java 25 branch text |
| `ReceiveMigratedCaseFileHelper` imports (`jakarta.json` vs `javax.json`) | Kept `jakarta.json`; main's new `Json.create…` calls use the static `JsonObjects` builders, as the rest of the Java 25 branch does |

Merged without conflict: aggregate fix and tests, new accept-migrated-case IT and fixture,
`EventSelector`, converter test, removal of the unused `pcfdlrm.events.hearing-validation-failed.json`
schema (no subscription, RAML or Java references it), and a `.gitignore` comment.
`coredomain` stays `25.104.0` — main did not change it.

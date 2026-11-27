# Requirements — DD-43905

1. Merge `origin/main` into `team/25.104.x`: 62 commits (48 non-merge) covering DD-42701, DD-42716,
   DD-42727, DD-42776, DD-42799, DD-42845, DD-42991, DD-42999, DD-43012, DD-43054, DD-43197, DD-43276.
2. The branch stays Java 25: project version `25.104.26-DLRMJ25-SNAPSHOT`, `service-parent-pom`
   and `coredomain` `25.104.0`, `ubuntu-j25` build agent, `jakarta.*` only (no `javax.*`).
3. Interface dependencies satisfy `RequireLatestMojInterfaceRule`.
4. Build, unit tests and the full IT suite pass on JDK 25 with `cpp-developers-docker` on `java-25`.

## Open questions

- The ticket has no acceptance criteria; the four points above come from the ticket title only.

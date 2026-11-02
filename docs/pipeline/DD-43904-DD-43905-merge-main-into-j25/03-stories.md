# Story — DD-43905

**As a** pcfdlrm developer **I want** main merged into `team/25.104.x` **so that** the Java 25 branch
carries main's latest fixes.

## Acceptance criteria

1. All `origin/main` commits are in the branch history.
2. No `javax.*` imports; poms and pipeline stay on the Java 25 values in `02-design.md`.
3. `mvn clean install` passes on JDK 25, including the interface enforcer.
4. `./runIntegrationTests.sh` passes in full on JDK 25 with `cpp-developers-docker` on `java-25`.

Test specs (Stage 4) are not produced: this is a merge with no new behaviour beyond what main
already tested.

CI retriggered 2026-11-02 after DevOps registry credential fix.

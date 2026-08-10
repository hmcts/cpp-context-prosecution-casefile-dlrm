package uk.gov.moj.cpp.pcfdlrm.builder;

/**
 * The migration source system a test case is built for. Deliberately a value type rather than two
 * more positional {@code String} parameters on {@link ObjectBuilder}: the surrounding builder method
 * already takes six consecutive strings, so a seventh and eighth would be a silent-transposition
 * waiting to happen.
 *
 * <p>There is no XHIBIT default and no defaulting overload — every caller states its source system,
 * so adding a new one later is a data change at the call site, not a code change in the builder.
 */
public record SourceSystem(String migrationSourceSystemName,
                           String migrationSourceSystemCaseIdentifier) {

    public static SourceSystem sourceSystem(final String migrationSourceSystemName,
                                            final String migrationSourceSystemCaseIdentifier) {
        return new SourceSystem(migrationSourceSystemName, migrationSourceSystemCaseIdentifier);
    }
}

package uk.gov.moj.cpp.pcfdlrm.aggregate;

import uk.gov.moj.cpp.pcfdlrm.test.FixtureLoader;

import java.util.List;
import java.util.Map;

/**
 * One row of the {@link AggregateScenario}/{@link MigratedCaseFileAggregateTest#assertEventsMatchExpected}
 * scenario harness. {@code fixture} is a classpath path unless {@code inline} is true, in which case
 * it is the expected JSON body itself — see {@link AggregateScenarios#warning}. {@code parameters}
 * feeds {@code {{token}}} substitution into a path fixture (see {@link FixtureLoader#fixture(String, Map)});
 * it is ignored when {@code inline} is true.
 */
record ExpectedEvent(Class<?> type, String fixture, boolean inline, Map<String, String> parameters, List<String> exclusions) {

    ExpectedEvent(final Class<?> type, final String fixture) {
        this(type, fixture, false, Map.of(), List.of());
    }

    ExpectedEvent(final Class<?> type, final String fixture, final List<String> exclusions) {
        this(type, fixture, false, Map.of(), exclusions);
    }

    ExpectedEvent(final Class<?> type, final String fixture, final Map<String, String> parameters) {
        this(type, fixture, false, parameters, List.of());
    }

    ExpectedEvent(final Class<?> type, final String fixture, final Map<String, String> parameters, final List<String> exclusions) {
        this(type, fixture, false, parameters, exclusions);
    }

    String expectedJson() {
        return inline ? fixture : FixtureLoader.fixture(fixture, parameters);
    }
}

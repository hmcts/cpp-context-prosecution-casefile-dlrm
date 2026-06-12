package uk.gov.moj.cpp.pcfdlrm.event.processor.counter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

public class PcfMigratedCaseFailedCounter {

    @Inject
    private CompositeMeterRegistry registry;

    private Counter counter;

    @PostConstruct
    public void init() {
        counter = Counter.builder("pcf-migrated-case-failed")
                .description("The counter for failed migrated cases processed")
                .tag("component", "aggregate")
                .register(registry);
    }

    public void increment() {
        counter.increment();
    }
}

package uk.gov.moj.cpp.pcfdlrm.event.processor.counter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

public class PcfMaterialUploadedCounter {

    @Inject
    private CompositeMeterRegistry registry;

    private Counter counter;

    @PostConstruct
    public void init() {
        counter = Counter.builder("pcf-material-uploaded")
                .description("The counter for material uploaded from material context")
                .tag("component", "aggregate")
                .register(registry);
    }

    public void increment() {
        counter.increment();
    }
}

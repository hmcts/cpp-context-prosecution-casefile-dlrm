package uk.gov.moj.cpp.pcfdlrm.event.processor.counter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

public class PcfMaterialToUploadCounter {

    @Inject
    private CompositeMeterRegistry registry;

    private Counter counter;

    @PostConstruct
    public void init() {
        counter = Counter.builder("pcf-material-to-upload")
                .description("The counter for material sent to material context for upload")
                .tag("component", "aggregate")
                .register(registry);
    }

    public void increment() {
        counter.increment();
    }
}

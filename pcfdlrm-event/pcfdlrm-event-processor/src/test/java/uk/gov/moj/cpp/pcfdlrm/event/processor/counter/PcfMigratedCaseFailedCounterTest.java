package uk.gov.moj.cpp.pcfdlrm.event.processor.counter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PcfMigratedCaseFailedCounterTest {

    private PcfMigratedCaseFailedCounter counter;
    private CompositeMeterRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        registry = new CompositeMeterRegistry();
        registry.add(new SimpleMeterRegistry());
        counter = new PcfMigratedCaseFailedCounter();
        setField(counter, "registry", registry);
        counter.init();
    }

    @Test
    void shouldIncrementCounter() {
        counter.increment();
        counter.increment();

        assertEquals(2, registry.find("pcf-migrated-case-failed").counter().count());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

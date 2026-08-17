package uk.gov.moj.cpp.pcfdlrm.test;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Wraps {@link FieldUtils#writeField(Object, String, Object, boolean)} so test classes that hand-wire
 * a private-{@code @Inject}-field object graph (because {@code @InjectMocks} only reaches one level
 * of a multi-level hierarchy) don't each declare their own {@code throws IllegalAccessException}.
 * Deliberately generic: the module-specific tree topology (which concrete classes get wired to which
 * field names) stays in the test class that needs it, not here — this module cannot depend on any
 * {@code pcfdlrm-*} module that itself depends on {@code pcfdlrm-test-support} at test scope without
 * creating a Maven reactor cycle.
 */
public final class ReflectionFieldInjector {

    private ReflectionFieldInjector() {
    }

    public static void writeField(final Object target, final String fieldName, final Object value) {
        try {
            FieldUtils.writeField(target, fieldName, value, true);
        } catch (final IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Unable to write field '" + fieldName + "' on " + target.getClass().getName(), e);
        }
    }
}

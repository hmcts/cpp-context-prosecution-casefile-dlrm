package uk.gov.moj.cpp.pcfdlrm.event.processor.convertor;

public interface ParameterisedConverter<S, T, P> {

    T convert(final S source, final P param);

}

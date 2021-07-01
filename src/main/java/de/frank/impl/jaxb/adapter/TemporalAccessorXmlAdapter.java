package de.frank.impl.jaxb.adapter;

import jakarta.xml.bind.annotation.adapters.*;

import java.time.format.*;
import java.time.temporal.*;
import java.util.*;


public class TemporalAccessorXmlAdapter<T extends TemporalAccessor> extends XmlAdapter<String, T> {
    private final DateTimeFormatter formatter;
    private final TemporalQuery<? extends T> temporalQuery;

    public TemporalAccessorXmlAdapter(DateTimeFormatter formatter, TemporalQuery<? extends T> temporalQuery) {
        this.formatter = (DateTimeFormatter) Objects.requireNonNull(formatter, "formatter must not be null");
        this.temporalQuery = (TemporalQuery) Objects.requireNonNull(temporalQuery, "temporal query must not be null");
    }

    public T unmarshal(String stringValue) {
        return stringValue != null ? this.formatter.parse(stringValue, this.temporalQuery) : null;
    }

    public String marshal(T value) {
        return value != null ? this.formatter.format(value) : null;
    }
}

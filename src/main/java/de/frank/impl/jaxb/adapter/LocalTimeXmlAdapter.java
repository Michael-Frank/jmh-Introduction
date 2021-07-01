package de.frank.impl.jaxb.adapter;

import java.time.*;
import java.time.format.*;

public class LocalTimeXmlAdapter extends TemporalAccessorXmlAdapter<LocalTime> {
    public LocalTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_LOCAL_TIME, LocalTime::from);
    }
}

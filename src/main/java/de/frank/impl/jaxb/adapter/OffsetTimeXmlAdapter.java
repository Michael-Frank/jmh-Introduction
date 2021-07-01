package de.frank.impl.jaxb.adapter;

import java.time.*;
import java.time.format.*;

public class OffsetTimeXmlAdapter extends TemporalAccessorXmlAdapter<OffsetTime> {
    public OffsetTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_OFFSET_TIME, OffsetTime::from);
    }
}

package de.frank.impl.jaxb.adapter;

import java.time.*;
import java.time.format.*;

public class OffsetDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<OffsetDateTime> {
    public OffsetDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_OFFSET_DATE_TIME, OffsetDateTime::from);
    }
}

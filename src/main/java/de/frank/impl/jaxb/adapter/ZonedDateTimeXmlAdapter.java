package de.frank.impl.jaxb.adapter;

import java.time.*;
import java.time.format.*;

public class ZonedDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<ZonedDateTime> {
    public ZonedDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_ZONED_DATE_TIME, ZonedDateTime::from);
    }
}

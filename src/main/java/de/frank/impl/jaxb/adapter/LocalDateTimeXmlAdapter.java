package de.frank.impl.jaxb.adapter;

import java.time.*;
import java.time.format.*;

public class LocalDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<LocalDateTime> {
    public LocalDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from);
    }
}

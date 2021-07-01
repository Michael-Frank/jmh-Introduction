package de.frank.impl.jaxb.adapter;

import java.time.*;
import java.time.format.*;

public class InstantXmlAdapter extends TemporalAccessorXmlAdapter<Instant> {
    public InstantXmlAdapter() {
        super(DateTimeFormatter.ISO_INSTANT, Instant::from);
    }
}

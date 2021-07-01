/**
 * Setup of XmlJavaTypeAdapters for Jaxb serialization of package contents
 * WARNING:
 * package-info is only valid for ONE package and not inherited by sub-packages
 * sub-packages require their own copy of this class!
 */
@XmlJavaTypeAdapters({
        //Package scoped XmlJavaTypeAdapters for java.time.*
        @XmlJavaTypeAdapter(type = OffsetDateTime.class, value = OffsetDateTimeXmlAdapter.class),
        @XmlJavaTypeAdapter(type = ZonedDateTime.class, value = ZonedDateTimeXmlAdapter.class),
        @XmlJavaTypeAdapter(type = Duration.class, value = DurationXmlAdapter.class),
        @XmlJavaTypeAdapter(type = Instant.class, value = InstantXmlAdapter.class),
        @XmlJavaTypeAdapter(type = LocalDateTime.class, value = LocalDateTimeXmlAdapter.class),
        @XmlJavaTypeAdapter(type = LocalDate.class, value = LocalDateXmlAdapter.class),
        @XmlJavaTypeAdapter(type = LocalTime.class, value = LocalTimeXmlAdapter.class),
        @XmlJavaTypeAdapter(type = OffsetTime.class, value = OffsetTimeXmlAdapter.class),
        @XmlJavaTypeAdapter(type = ZoneId.class, value = ZoneIdXmlAdapter.class),
        //other:
        @XmlJavaTypeAdapter(type = Locale.class, value = LocaleXmlAdapter.class)
})
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
package de.frank.jmh.model;

import de.frank.impl.jaxb.adapter.*;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.*;

import java.time.*;
import java.util.*;



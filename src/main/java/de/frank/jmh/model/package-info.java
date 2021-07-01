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
        @XmlJavaTypeAdapter(type = Locale.class, value = LocaleXmlAdapter.class)
})
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
package com.virtualvenue.model;

import io.github.threetenjaxb.core.DurationXmlAdapter;
import io.github.threetenjaxb.core.InstantXmlAdapter;
import io.github.threetenjaxb.core.LocalDateTimeXmlAdapter;
import io.github.threetenjaxb.core.LocalDateXmlAdapter;
import io.github.threetenjaxb.core.LocalTimeXmlAdapter;
import io.github.threetenjaxb.core.OffsetDateTimeXmlAdapter;
import io.github.threetenjaxb.core.OffsetTimeXmlAdapter;
import io.github.threetenjaxb.core.ZoneIdXmlAdapter;
import io.github.threetenjaxb.core.ZonedDateTimeXmlAdapter;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;



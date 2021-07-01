package de.frank.impl.jaxb.adapter;

import jakarta.xml.bind.annotation.adapters.*;

import java.time.*;

public class DurationXmlAdapter extends XmlAdapter<String, Duration> {
    public DurationXmlAdapter() {
    }

    public Duration unmarshal(String stringValue) {
        return stringValue != null ? Duration.parse(stringValue) : null;
    }

    public String marshal(Duration value) {
        return value != null ? value.toString() : null;
    }
}

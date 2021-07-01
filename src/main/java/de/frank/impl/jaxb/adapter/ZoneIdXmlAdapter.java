package de.frank.impl.jaxb.adapter;

import jakarta.xml.bind.annotation.adapters.*;

import java.time.*;

public class ZoneIdXmlAdapter extends XmlAdapter<String, ZoneId> {
    public ZoneIdXmlAdapter() {
    }

    public ZoneId unmarshal(String stringValue) {
        return stringValue != null ? ZoneId.of(stringValue) : null;
    }

    public String marshal(ZoneId value) {
        return value != null ? value.getId() : null;
    }
}

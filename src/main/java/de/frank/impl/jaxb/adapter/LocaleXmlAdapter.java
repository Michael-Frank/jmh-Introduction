package de.frank.impl.jaxb.adapter;

import jakarta.xml.bind.annotation.adapters.*;

import java.util.*;


/**
 * {@code XmlAdapter} mapping  {@code java.util.Locale} from and to the {@link Locale#toLanguageTag()}
 * <p>
 *
 * @see Locale
 */
public class LocaleXmlAdapter extends XmlAdapter<String, Locale> {
    @Override
    public Locale unmarshal(String stringValue) {
        return stringValue != null ? Locale.forLanguageTag(stringValue) : null;
    }

    @Override
    public String marshal(Locale value) {
        return value != null ? value.toLanguageTag() : null;
    }
}


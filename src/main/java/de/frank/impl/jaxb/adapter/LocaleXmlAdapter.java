package com.virtualvenue.model;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Locale;


/**
 * {@code XmlAdapter} mapping  {@code java.util.Locale} from and to the {@link Locale#toLanguageTag()}
 * <p>
 *
 * @see java.util.Locale
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


package de.frank.impl.jaxb;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.*;
import lombok.*;
import lombok.experimental.*;
import org.eclipse.persistence.jaxb.*;
import org.xml.sax.*;

import javax.xml.validation.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Marshall jakarta.xml.bindannotation.* annotated classes to and from XML
 */
@UtilityClass
public class CachedJaxbXmlMapper {

    /**
     * JaxBContext is heavyweight but thread safe and should be cached.
     * Marshaller and Un-marshaller are NOT thread safe, but still heavyweight.
     * We therefore cache them safely with in a @{link ThreadLocal}
     */
    private static final ConcurrentHashMap<Class<?>, ScopedJAXB> SCOPED_JAXB_CONTEXTS = new ConcurrentHashMap<>();

    /**
     * serialize (marshall) an Object graph to xml
     *
     * @param model the input object to serialize
     * @return the xml String representation of the input object
     */
    public static String toXMLString(Object model) {
        StringWriter writer = new StringWriter();
        toXML(model, writer);
        return writer.toString();
    }

    public static <T> byte[] toXMLBytes(T model) {
        Objects.requireNonNull(model);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        toXML(model, bos);
        return bos.toByteArray();
    }

    /**
     * serialize (marshall) an Object graph to xml
     *
     * @param model  the input object to serialize
     * @param writer the target where to write the xml string to
     */
    public static void toXML(Object model, Writer writer) {
        try {
            Marshaller marshaller = cachedMarshallerFor(model.getClass());
            marshaller.marshal(model, writer);
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }


    /**
     * serialize (marshall) an Object graph to xml
     *
     * @param model the input object to serialize
     * @param out   the target where to write the xml bytes to
     */
    public static void toXML(Object model, OutputStream out) {
        try {
            Marshaller marshaller = cachedMarshallerFor(model.getClass());
            marshaller.marshal(model, out);
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }

    /**
     * Deserialize an object graph from xml string
     *
     * @param inputXml the input xml string to parse
     * @param type     the model class to un-marshaller into
     * @param <T>      generic model type
     * @return the Un-marshaller model
     */
    public static <T> T fromXML(String inputXml, Class<T> type) {
        return fromXML(new StringReader(inputXml), type);
    }

    /**
     * Deserialize an object graph from xml
     *
     * @param reader the input to read the xml source form
     * @param type   the model class to un-marshaller into
     * @param <T>    generic model type
     * @return the Un-marshaller model
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromXML(Reader reader, Class<T> type) {
        try {
            return (T) cachedUnmarshallerFor(type).unmarshal(reader);
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }

    /**
     * Deserialize an object graph from xml
     *
     * @param inputStream the input to read the xml source form
     * @param type        the model class to un-marshaller into
     * @param <T>         generic model type
     * @return the Un-marshaller model
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromXML(InputStream inputStream, Class<T> type) {
        try {
            return (T) cachedUnmarshallerFor(type).unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }


    public static <T> T formXml(byte[] modelAsBytes, Class<T> type) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(modelAsBytes);
        return fromXML(new ByteArrayInputStream(modelAsBytes), type);
    }


    private static JAXBContext newJaxbContextFor(Class<?> type) {
        try {
            return JAXBContext.newInstance(type);
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }

    private static Marshaller newMarshaller(JAXBContext jaxbContext) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, true);
            return marshaller;
        } catch (JAXBException e) {
            throw new UncheckedXMLException(e);
        }
    }

    private static Unmarshaller newUnmarshaller(JAXBContext jaxbContext, Class<?> type) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Schema schema = SchemaNodeGenerator.generateSchemaFor(type);
            unmarshaller.setSchema(schema);
            return unmarshaller;
        } catch (JAXBException | SAXException e) {
            throw new UncheckedXMLException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Marshaller cachedMarshallerFor(Class<?> type) {
        return SCOPED_JAXB_CONTEXTS.computeIfAbsent(type, CachedJaxbXmlMapper::newScopedJAXB).getMarshaller();
    }

    private static Unmarshaller cachedUnmarshallerFor(Class<?> type) {
        return SCOPED_JAXB_CONTEXTS.computeIfAbsent(type, CachedJaxbXmlMapper::newScopedJAXB).getUnmarshaller();
    }

    private static ScopedJAXB newScopedJAXB(Class<?> type) {
        JAXBContext jaxbContext = newJaxbContextFor(type);
        ThreadLocal<Marshaller> marshaller = ThreadLocal.withInitial(() -> newMarshaller(jaxbContext));
        ThreadLocal<Unmarshaller> unmarshaller = ThreadLocal.withInitial(() -> newUnmarshaller(jaxbContext, type));
        return new ScopedJAXB(jaxbContext, marshaller, unmarshaller);
    }

    /*
     *java:S5164:"ThreadLocal variables cleaned up" -> not in this case, we want them to life the entire app lifetime
     */
    @Value
    @AllArgsConstructor
    @SuppressWarnings("java:S5164")
    private static class ScopedJAXB {
        JAXBContext jaxbContext;
        ThreadLocal<Marshaller> marshaller;
        ThreadLocal<Unmarshaller> unmarshaller;

        public Marshaller getMarshaller() {
            return marshaller.get();
        }

        public Unmarshaller getUnmarshaller() {
            return unmarshaller.get();
        }
    }

}

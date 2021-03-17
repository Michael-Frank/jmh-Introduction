package de.frank.impl.jaxb;

import lombok.*;
import org.apache.commons.lang3.*;
import org.xml.sax.*;

import javax.xml.*;
import javax.xml.bind.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.validation.*;
import java.io.*;

/**
 * Generates an XSD schema from the model (jaxb annotated classes them self)
 * <p>
 * Usage (preferred! - internally creates a new unpolluted jaxb context):
 * <pre>{@code
 *  Schema xsdSchema = SchemaNodeGenerator.generateSchemaFor(rootType)
 * }</pre>
 * <p>
 * <p>
 * or with an existing context:
 * <pre>{@code
 *  JAXBContext context = JAXBContext.newInstance(rootType);
 *  SchemaNodeGenerator gen = new SchemaNodeGenerator();
 *  context.generateSchema(gen);
 *  Schema schemaForType = gen.getSchema();
 * }</pre>
 */
@NoArgsConstructor
public class SchemaNodeGenerator extends SchemaOutputResolver {

    private final DOMResult result = new DOMResult();

    @Override
    public Result createOutput(String namespaceURI, String suggestedFileName) {
        result.setSystemId("schema.xsd");
        return result;
    }

    /**
     * The generated xml Schema node
     * <p>
     * Only valid if:
     * <pre>{@code
     *  JAXBContext context = JAXBContext.newInstance(rootType);
     *  SchemaNodeGenerator gen = new SchemaNodeGenerator();
     *  context.generateSchema(gen);
     *  Schema schemaForType = gen.getSchema();
     * }</pre>
     * was called on this object before
     *
     * @return the XML Schema node
     * @throws SAXException if something went wrong during schema generation
     */
    public Schema getSchema() throws SAXException {
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        //prevent XXE attacks:
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        return schemaFactory.newSchema(new DOMSource(result.getNode()));
    }

    /**
     * Generates a Schema  for provided root type
     *
     * @param rootTypes type to generate xsd for
     * @return the generated xsd
     * @throws IOException   as defined by {@link JAXBContext#generateSchema(SchemaOutputResolver)}
     * @throws JAXBException as defined by {@link JAXBContext#newInstance(Class[])}
     * @throws SAXException  if something went wrong during schema generation
     */
    @SuppressWarnings({
            "java:S923",//"dont use varargs" -> same interface as JAXBContext.newInstance(types)
            "java:S1160",//"only one checked exception" ->  handle in usage class in this case
    })
    public static Schema generateSchemaFor(Class<?>... rootTypes)
            throws IOException, SAXException, JAXBException {
        Validate.notEmpty(rootTypes);

        JAXBContext context = JAXBContext.newInstance(rootTypes);
        SchemaNodeGenerator out = new SchemaNodeGenerator();
        context.generateSchema(out);
        return out.getSchema();
    }
}

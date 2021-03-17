package de.frank.impl.jaxb;

/**
 * Unchecked {@link RuntimeException} wrapper for exceptions during xml (Un-)marshalling
 */
public class UncheckedXMLException extends RuntimeException {
    /**
     * Unchecked {@link RuntimeException} wrapper for exceptions during xml (Un-)marshalling
     *
     * @param cause the (checked) cause
     */
    public UncheckedXMLException(Exception cause) {
        super(cause);
    }
}

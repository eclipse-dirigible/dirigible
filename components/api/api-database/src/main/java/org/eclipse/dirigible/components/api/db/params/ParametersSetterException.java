package org.eclipse.dirigible.components.api.db.params;

public class ParametersSetterException extends RuntimeException {
    public ParametersSetterException(String message) {
        super(message);
    }

    public ParametersSetterException(String message, Throwable cause) {
        super(message, cause);
    }
}

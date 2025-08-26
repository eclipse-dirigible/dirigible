package org.eclipse.dirigible.components.data.processes.schema.export;

public class InvalidProcessParameterException extends RuntimeException {

    public InvalidProcessParameterException(String message) {
        super(message);
    }

    public InvalidProcessParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}

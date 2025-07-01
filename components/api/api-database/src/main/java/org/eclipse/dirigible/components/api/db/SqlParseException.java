package org.eclipse.dirigible.components.api.db;

public class SqlParseException extends RuntimeException {

    public SqlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public SqlParseException(String message) {
        super(message);
    }
}

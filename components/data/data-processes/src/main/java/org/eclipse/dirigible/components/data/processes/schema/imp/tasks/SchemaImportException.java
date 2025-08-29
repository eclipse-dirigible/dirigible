package org.eclipse.dirigible.components.data.processes.schema.imp.tasks;

public class SchemaImportException extends RuntimeException {

    public SchemaImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaImportException(String message) {
        super(message);
    }
}

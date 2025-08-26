package org.eclipse.dirigible.components.data.processes.schema.export.tasks;

public class SchemaExportException extends RuntimeException {

    public SchemaExportException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaExportException(String message) {
        super(message);
    }
}

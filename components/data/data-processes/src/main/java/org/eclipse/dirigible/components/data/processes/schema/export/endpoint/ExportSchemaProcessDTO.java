package org.eclipse.dirigible.components.data.processes.schema.export.endpoint;

class ExportSchemaProcessDTO {

    private final String processId;

    public ExportSchemaProcessDTO(String processId) {
        this.processId = processId;
    }

    public String getProcessId() {
        return processId;
    }

}

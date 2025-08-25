package org.eclipse.dirigible.components.data.processes.schema.imp.endpoint;

class ImportSchemaProcessDTO {

    private final String processId;

    public ImportSchemaProcessDTO(String processId) {
        this.processId = processId;
    }

    public String getProcessId() {
        return processId;
    }

}

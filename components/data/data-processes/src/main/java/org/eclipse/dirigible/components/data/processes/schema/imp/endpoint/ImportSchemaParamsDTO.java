package org.eclipse.dirigible.components.data.processes.schema.imp.endpoint;

import jakarta.validation.constraints.NotBlank;

class ImportSchemaParamsDTO {

    @NotBlank
    private String dataSource;

    @NotBlank
    private String exportPath;

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }
}

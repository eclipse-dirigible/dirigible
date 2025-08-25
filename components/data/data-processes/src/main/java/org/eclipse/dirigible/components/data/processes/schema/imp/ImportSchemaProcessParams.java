package org.eclipse.dirigible.components.data.processes.schema.imp;

public class ImportSchemaProcessParams {
    private final String dataSource;
    private final String exportPath;

    public ImportSchemaProcessParams(String dataSource, String exportPath) {
        this.dataSource = dataSource;
        this.exportPath = exportPath;
    }

    public String getDataSource() {
        return dataSource;
    }

    public String getExportPath() {
        return exportPath;
    }

}

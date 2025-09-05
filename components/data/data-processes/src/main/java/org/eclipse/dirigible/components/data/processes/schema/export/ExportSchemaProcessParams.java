package org.eclipse.dirigible.components.data.processes.schema.export;

import java.util.Set;

public class ExportSchemaProcessParams {
    private final String dataSource;
    private final String schema;
    private final String exportPath;
    private final Set<String> includedTables;
    private final Set<String> excludedTables;

    public ExportSchemaProcessParams(String dataSource, String schema, String exportPath, Set<String> includedTables,
            Set<String> excludedTables) {
        this.dataSource = dataSource;
        this.schema = schema;
        this.exportPath = exportPath;
        this.includedTables = includedTables;
        this.excludedTables = excludedTables;
    }

    public String getDataSource() {
        return dataSource;
    }

    public String getSchema() {
        return schema;
    }

    public String getExportPath() {
        return exportPath;
    }

    public Set<String> getIncludedTables() {
        return includedTables;
    }

    public Set<String> getExcludedTables() {
        return excludedTables;
    }
}

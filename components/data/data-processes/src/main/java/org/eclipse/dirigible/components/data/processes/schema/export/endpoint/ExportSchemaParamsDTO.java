package org.eclipse.dirigible.components.data.processes.schema.export.endpoint;

import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.Set;

class ExportSchemaParamsDTO {

    @NotBlank
    private String dataSource;

    @NotBlank
    private String schema;

    @NotBlank
    private String exportPath;

    private Set<String> includedTables;

    private Set<String> excludedTables;

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public Set<String> getIncludedTables() {
        return includedTables == null ? Collections.emptySet() : includedTables;
    }

    public void setIncludedTables(Set<String> includedTables) {
        this.includedTables = includedTables;
    }

    public Set<String> getExcludedTables() {
        return excludedTables == null ? Collections.emptySet() : excludedTables;
    }

    public void setExcludedTables(Set<String> excludedTables) {
        this.excludedTables = excludedTables;
    }
}

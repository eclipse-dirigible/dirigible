package org.eclipse.dirigible.components.data.processes.schema.imp.tasks;

import org.eclipse.dirigible.components.data.processes.schema.imp.ImportSchemaProcessParams;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.JsonProcessVariablesBuilder;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;

import java.util.List;
import java.util.Map;

public class ImportProcessContext {

    private static final String DATA_SOURCE_CTX_PARAM = "dataSource";
    private static final String EXPORT_PATH_CTX_PARAM = "exportPath";
    private static final String TABLE_SCHEMA_CTX_PARAM = "tableSchema";

    // caution: these values are used in the BPMN process definition
    private static final String TABLES_CTX_PARAM = "tables";
    private static final String TABLE_CTX_PARAM = "table";

    private final TaskExecution execution;

    public ImportProcessContext(TaskExecution execution) {
        this.execution = execution;
    }

    public String getDataSource() {
        return execution.getMandatoryVariable(DATA_SOURCE_CTX_PARAM, String.class);
    }

    public String getExportPath() {
        return execution.getMandatoryVariable(EXPORT_PATH_CTX_PARAM, String.class);
    }

    public static Map<String, Object> createInitialVariables(ImportSchemaProcessParams params) {
        JsonProcessVariablesBuilder variablesBuilder = new JsonProcessVariablesBuilder();

        return variablesBuilder//
                               .addVariable(DATA_SOURCE_CTX_PARAM, params.getDataSource())
                               .addVariable(EXPORT_PATH_CTX_PARAM, params.getExportPath())
                               .build();
    }

    public void setTables(List<String> tables) {
        execution.setVariable(TABLES_CTX_PARAM, tables);
    }

    public String getTable() {
        return execution.getMandatoryVariable(TABLE_CTX_PARAM, String.class);
    }

    public String getTableSchema() {
        return execution.getMandatoryVariable(TABLE_SCHEMA_CTX_PARAM, String.class);
    }

    public void setTableSchema(String schema) {
        execution.setVariable(TABLE_SCHEMA_CTX_PARAM, schema);
    }
}

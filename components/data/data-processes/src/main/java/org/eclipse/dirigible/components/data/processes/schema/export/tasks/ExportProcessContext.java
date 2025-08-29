package org.eclipse.dirigible.components.data.processes.schema.export.tasks;

import com.google.gson.reflect.TypeToken;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportSchemaProcessParams;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.JsonProcessVariablesBuilder;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExportProcessContext {

    private static final String INCLUDED_TABLES_CTX_PARAM = "includedTables";
    private static final String EXCLUDED_TABLES_CTX_PARAM = "excludedTables";
    private static final String DATA_SOURCE_CTX_PARAM = "dataSource";
    private static final String SCHEMA_CTX_PARAM = "schema";
    private static final String EXPORT_PATH_CTX_PARAM = "exportPath";

    // caution: these values are used in the BPMN process definition
    private static final String TARGET_TABLE_CTX_PARAM = "targetTable";
    private static final String EXPORT_TOPOLOGY_CTX_PARAM = "exportTopology";

    private final TaskExecution execution;

    public ExportProcessContext(TaskExecution execution) {
        this.execution = execution;
    }

    public Set<String> getIncludedTables() {
        TypeToken<Set<String>> typeToken = new TypeToken<>() {};

        return execution.getVariable(INCLUDED_TABLES_CTX_PARAM, typeToken)
                        .orElse(Collections.emptySet());
    }

    public String getDataSource() {
        return execution.getMandatoryVariable(DATA_SOURCE_CTX_PARAM, String.class);
    }

    public String getSchema() {
        return execution.getMandatoryVariable(SCHEMA_CTX_PARAM, String.class);
    }

    public String getExportPath() {
        return execution.getMandatoryVariable(EXPORT_PATH_CTX_PARAM, String.class);
    }

    public Set<String> getExcludedTables() {
        TypeToken<Set<String>> typeToken = new TypeToken<>() {};

        return execution.getVariable(EXCLUDED_TABLES_CTX_PARAM, typeToken)
                        .orElse(Collections.emptySet());
    }

    public static Map<String, Object> createInitialVariables(ExportSchemaProcessParams params) {
        JsonProcessVariablesBuilder variablesBuilder = new JsonProcessVariablesBuilder();

        return variablesBuilder//
                               .addVariable(INCLUDED_TABLES_CTX_PARAM, params.getIncludedTables())
                               .addVariable(EXCLUDED_TABLES_CTX_PARAM, params.getExcludedTables())
                               .addVariable(DATA_SOURCE_CTX_PARAM, params.getDataSource())
                               .addVariable(SCHEMA_CTX_PARAM, params.getSchema())
                               .addVariable(EXPORT_PATH_CTX_PARAM, params.getExportPath())
                               .build();
    }

    public List<String> getExportTopology() {
        TypeToken<List<String>> typeToken = new TypeToken<>() {};

        return execution.getMandatoryVariable(EXPORT_TOPOLOGY_CTX_PARAM, typeToken);
    }

    public void setExportTopology(List<String> exportTopology) {
        execution.setVariable(EXPORT_TOPOLOGY_CTX_PARAM, exportTopology);
    }

    public String getCurrentTable() {
        return execution.getMandatoryVariable(TARGET_TABLE_CTX_PARAM, String.class);
    }
}

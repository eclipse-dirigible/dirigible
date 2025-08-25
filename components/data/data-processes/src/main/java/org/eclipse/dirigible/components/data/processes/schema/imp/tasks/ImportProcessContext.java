package org.eclipse.dirigible.components.data.processes.schema.imp.tasks;

import org.eclipse.dirigible.components.data.processes.schema.imp.ImportSchemaProcessParams;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.JsonProcessVariablesBuilder;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;

import java.util.Map;

public class ImportProcessContext {

    private static final String DATA_SOURCE_CTX_PARAM = "dataSource";
    private static final String EXPORT_PATH_CTX_PARAM = "exportPath";

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
}

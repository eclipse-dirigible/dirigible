package org.eclipse.dirigible.components.data.processes.schema.imp;

import org.eclipse.dirigible.components.data.processes.schema.imp.tasks.ImportProcessContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImportSchemaProcessTrigger {

    private final BpmService bpmService;

    ImportSchemaProcessTrigger(BpmService bpmService) {
        this.bpmService = bpmService;
    }

    public String trigger(ImportSchemaProcessParams params) {
        validateParams(params);

        Map<String, Object> variables = ImportProcessContext.createInitialVariables(params);

        return bpmService.startProcess("import-schema", null, variables);

    }

    private void validateParams(ImportSchemaProcessParams params) {
        // TODO: validate params from business perspective
    }
}

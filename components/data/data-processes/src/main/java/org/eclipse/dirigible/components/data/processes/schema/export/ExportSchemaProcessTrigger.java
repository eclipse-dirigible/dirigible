package org.eclipse.dirigible.components.data.processes.schema.export;

import org.eclipse.dirigible.components.data.processes.schema.export.tasks.ExportProcessContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExportSchemaProcessTrigger {

    private final BpmService bpmService;

    ExportSchemaProcessTrigger(BpmService bpmService) {
        this.bpmService = bpmService;
    }

    public String trigger(ExportSchemaProcessParams params) {
        // TODO: validate params from business perspective

        Map<String, Object> variables = ExportProcessContext.createInitialVariables(params);

        return bpmService.startProcess("export-schema", null, variables);
    }
}

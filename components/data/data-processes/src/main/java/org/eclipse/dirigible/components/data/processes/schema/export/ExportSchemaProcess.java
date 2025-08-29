package org.eclipse.dirigible.components.data.processes.schema.export;

import org.eclipse.dirigible.components.engine.bpm.SystemBpmProcess;
import org.springframework.stereotype.Component;

@Component
class ExportSchemaProcess implements SystemBpmProcess {

    @Override
    public String getIdentifier() {
        return "export-schema";
    }

    @Override
    public String getDeploymentKey() {
        return "export-schema";
    }

    @Override
    public String getResourcePath() {
        return "/system-processes/export-schema.bpmn";
    }
}

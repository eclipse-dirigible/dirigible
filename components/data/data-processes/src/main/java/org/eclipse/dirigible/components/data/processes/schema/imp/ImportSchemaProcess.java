package org.eclipse.dirigible.components.data.processes.schema.imp;

import org.eclipse.dirigible.components.engine.bpm.SystemBpmProcess;
import org.springframework.stereotype.Component;

@Component
class ImportSchemaProcess implements SystemBpmProcess {

    @Override
    public String getIdentifier() {
        return "import-schema";
    }

    @Override
    public String getDeploymentKey() {
        return "import-schema";
    }

    @Override
    public String getResourcePath() {
        return "/system-processes/import-schema.bpmn";
    }
}

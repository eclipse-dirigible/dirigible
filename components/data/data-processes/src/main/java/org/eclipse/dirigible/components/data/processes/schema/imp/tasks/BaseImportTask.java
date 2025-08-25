package org.eclipse.dirigible.components.data.processes.schema.imp.tasks;

import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.BPMTask;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;

abstract class BaseImportTask extends BPMTask {

    @Override
    protected final void execute(TaskExecution execution) {
        ImportProcessContext context = new ImportProcessContext(execution);
        execute(context);
    }

    protected abstract void execute(ImportProcessContext context);

}

package org.eclipse.dirigible.components.data.processes.schema.export.tasks;

import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.BPMTask;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;

abstract class BaseExportTask extends BPMTask {

    @Override
    protected final void execute(TaskExecution execution) {
        ExportProcessContext context = new ExportProcessContext(execution);
        execute(context);
    }

    protected abstract void execute(ExportProcessContext context);

}

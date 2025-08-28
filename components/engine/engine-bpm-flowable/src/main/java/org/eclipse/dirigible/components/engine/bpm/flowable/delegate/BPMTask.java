package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.transaction.annotation.Transactional;

public abstract class BPMTask implements JavaDelegate {

    @Transactional
    @Override
    public final void execute(DelegateExecution delegateExecution) {
        TaskExecution execution = new TaskExecution(delegateExecution);
        execute(execution);
    }

    protected abstract void execute(TaskExecution execution);

}

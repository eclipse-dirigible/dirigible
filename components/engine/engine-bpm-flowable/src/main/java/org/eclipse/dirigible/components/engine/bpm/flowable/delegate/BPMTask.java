package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.transaction.annotation.Transactional;

public abstract class BPMTask implements JavaDelegate {

    @Transactional
    @Override
    public final void execute(DelegateExecution delegateExecution) {
        TaskExecution execution = new TaskExecution(delegateExecution);

        String tenantId = getTenantId(delegateExecution);

        TenantContext tenantContext = BeanProvider.getBean(TenantContext.class);// since filed injection doesn't work
        tenantContext.execute(tenantId, () -> {
            execute(execution);
            return null;
        });
    }

    private String getTenantId(DelegateExecution delegateExecution) {
        String tenantId = delegateExecution.getTenantId();
        if (null == tenantId) {
            String message = "Missing tenant id for execution [" + delegateExecution.getId() + "] in process instance ["
                    + delegateExecution.getProcessInstanceId() + "]";
            throw new IllegalStateException(message);
        }
        return tenantId;
    }

    protected abstract void execute(TaskExecution execution);

}

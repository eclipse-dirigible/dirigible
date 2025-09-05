package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActionData.Action.SKIP;
import static org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService.DIRIGIBLE_BPM_INTERNAL_SKIP_STEP;

public abstract class BPMTask implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(BPMTask.class);

    @Transactional
    @Override
    public final void execute(DelegateExecution delegateExecution) {
        TaskExecution execution = new TaskExecution(delegateExecution);

        String tenantId = getTenantId(delegateExecution);

        TenantContext tenantContext = BeanProvider.getBean(TenantContext.class);// since filed injection doesn't work
        tenantContext.execute(tenantId, () -> {
            executeForTenant(execution);
            return null;
        });
    }

    private void executeForTenant(TaskExecution execution) {
        Optional<String> action = execution.getVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP, String.class);
        if (action.isPresent() && SKIP.getActionName()
                                      .equals(action.get())) {
            LOGGER.debug("Skipping task execution since it is marked for skip. Execution id [{}], process instance id [{}]",
                    execution.getId(), execution.getProcessInstanceId());
            execution.removeVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP);
            return;
        }

        execute(execution);
    }

    protected abstract void execute(TaskExecution execution);

    private String getTenantId(DelegateExecution delegateExecution) {
        String tenantId = delegateExecution.getTenantId();
        if (null == tenantId) {
            String message = "Missing tenant id for execution [" + delegateExecution.getId() + "] in process instance ["
                    + delegateExecution.getProcessInstanceId() + "]";
            throw new IllegalStateException(message);
        }
        return tenantId;
    }

}

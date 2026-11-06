/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActionData.Action.SKIP;
import static org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService.DIRIGIBLE_BPM_INTERNAL_SKIP_STEP;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.dirigible.components.base.script.ScriptEngineUnavailableException;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.eclipse.dirigible.components.tracing.TaskState;
import org.eclipse.dirigible.components.tracing.TaskStateUtil;
import org.eclipse.dirigible.components.tracing.TaskType;
import org.eclipse.dirigible.components.tracing.TracingFacade;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@Component("JSListener")
// A new instance per resolution (#8392): Flowable resolves the delegate expression on every
// execution and injects the step's handler field into the instance it got - on a shared singleton a
// concurrent job's injection could land before this one read its handler and run the wrong step.
@org.springframework.context.annotation.Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DirigibleTaskListener implements TaskListener {

    private final TenantContext tenantContext;
    /**
     * The executor of the script handler, absent where no script engine is deployed.
     */
    private final ObjectProvider<BpmScriptExecutor> scriptExecutor;
    /**
     * The handler.
     */
    private FixedValue handler;
    /**
     * The type.
     */
    private FixedValue type;

    DirigibleTaskListener(TenantContext tenantContext, ObjectProvider<BpmScriptExecutor> scriptExecutor) {
        this.tenantContext = tenantContext;
        this.scriptExecutor = scriptExecutor;
    }

    /**
     * Getter for the handler attribute.
     *
     * @return the handler
     */
    public FixedValue getHandler() {
        return handler;
    }

    /**
     * Setter of the handler attribute.
     *
     * @param handler the handler
     */
    public void setHandler(FixedValue handler) {
        this.handler = handler;
    }

    /**
     * Getter for the engine attribute.
     *
     * @return the type
     */
    public FixedValue getType() {
        return type;
    }

    /**
     * Setter of the engine attribute.
     *
     * @param type the type
     */
    public void setType(FixedValue type) {
        this.type = type;
    }

    @Override
    public void notify(DelegateTask task) {
        TaskState taskState = null;
        if (TracingFacade.isTracingEnabled()) {
            Map<String, String> input = TaskStateUtil.getVariables(task.getVariables());
            taskState = TracingFacade.taskStarted(TaskType.BPM, task.getId(), task.getTaskDefinitionKey(), input);

            taskState.setDefinition(task.getProcessDefinitionId());
            taskState.setInstance(task.getProcessInstanceId());
            taskState.setTenant(task.getTenantId());
        }
        Tracer tracer = OpenTelemetryProvider.get()
                                             .getTracer("eclipse-dirigible");
        Span span = tracer.spanBuilder("flowable_task_listener")
                          .startSpan();
        try (Scope scope = span.makeCurrent()) {
            addSpanAttributes(task, span);

            executeInternal(task);

            if (TracingFacade.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(task.getVariables());
                TracingFacade.taskSuccessful(taskState, output);
            }
        } catch (RuntimeException e) {
            if (e instanceof BpmnError bpmnError) {
                throw bpmnError;
            }

            if (TracingFacade.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(task.getVariables());
                TracingFacade.taskFailed(taskState, output, e.getMessage());
            }
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred during task listener execution");

            throw e;
        } finally {
            span.end();
        }
    }

    private void addSpanAttributes(DelegateTask task, Span span) {
        String taskId = task.getId();
        span.setAttribute("task.id", taskId);

        String processInstanceId = task.getProcessInstanceId();
        span.setAttribute("process.instance.id", processInstanceId);

        String processDefinitionId = task.getProcessDefinitionId();
        span.setAttribute("process.definition.id", processDefinitionId);
    }

    private void executeInternal(DelegateTask task) {
        String action = (String) task.getVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP);
        if (SKIP.getActionName()
                .equals(action)) {
            task.removeVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP);
            return;
        }

        Map<Object, Object> context = new HashMap<>();
        context.put("task", task);
        if (type == null) {
            type = new FixedValue("javascript");
        }
        if (handler == null) {
            throw new BpmnRuntimeException("Handler cannot be null at the task delegate.");
        }
        String tenantId = getTenantId(task);
        executeJSHandlerInTenantContext(tenantId, context);
    }

    private static String getTenantId(DelegateTask task) {
        String tenantId = task.getTenantId();
        if (null == tenantId) {
            String taskId = task.getId();
            String processInstanceId = task.getProcessInstanceId();
            String processDefinitionId = task.getProcessDefinitionId();
            throw new IllegalStateException("Missing tenant id for task with id [" + taskId + "], process instance id [" + processInstanceId
                    + "] and process definition id [" + processDefinitionId + "]");
        }
        return tenantId;
    }

    @Transactional
    private void executeJSHandlerInTenantContext(String tenantId, Map<Object, Object> context) {
        tenantContext.execute(tenantId, () -> {
            executeJSHandler(context);
            return null;
        });
    }

    /**
     * Execute JS handler.
     *
     * @param context the context
     */
    private void executeJSHandler(Map<Object, Object> context) {
        RepositoryPath path = new RepositoryPath(handler.getExpressionText());

        Span.current()
            .setAttribute("handler", path.toString())
            .setAttribute("tenantId", tenantContext.getCurrentTenant()
                                                   .getId());

        BpmScriptExecutor executor = scriptExecutor.getIfAvailable();
        if (executor == null) {
            throw new ScriptEngineUnavailableException(path.getPath());
        }
        executor.execute(path.getPath(), context);
    }
}

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

import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.eclipse.dirigible.components.tracing.TaskState;
import org.eclipse.dirigible.components.tracing.TaskStateUtil;
import org.eclipse.dirigible.components.tracing.TaskType;
import org.eclipse.dirigible.components.tracing.TracingFacade;
import org.eclipse.dirigible.engine.java.runtime.ClientClassLoader;
import org.eclipse.dirigible.engine.java.runtime.ClientClassLoaderHolder;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Counterpart of {@link DirigibleCallDelegate} for client Java tasks.
 *
 * <p>
 * Bound to the Spring bean name {@code JavaTask} so BPMN service tasks can use
 * {@code flowable:delegateExpression="${JavaTask}"} together with a {@code handler} field whose
 * value is the fully-qualified class name of a client class implementing {@link JavaDelegate}. The
 * class is resolved through the currently-installed {@link ClientClassLoader} (managed by
 * {@link ClientClassLoaderHolder}), instantiated via its public no-arg constructor on every
 * execution, and invoked with the engine-provided {@link DelegateExecution}.
 *
 * <p>
 * For the alternative {@code flowable:class="..."} approach, see the classloader configured on the
 * {@code SpringProcessEngineConfiguration} in
 * {@code org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmFlowableConfig}.
 */
// don't change the name of the bean or the class name and package
// otherwise processes will stop working
@Component("JavaTask")
public class DirigibleJavaCallDelegate implements JavaDelegate {

    private final TenantContext tenantContext;
    private final ClientClassLoaderHolder clientClassLoaderHolder;

    /** The handler. */
    private FixedValue handler;

    DirigibleJavaCallDelegate(TenantContext tenantContext, ClientClassLoaderHolder clientClassLoaderHolder) {
        this.tenantContext = tenantContext;
        this.clientClassLoaderHolder = clientClassLoaderHolder;
    }

    public FixedValue getHandler() {
        return handler;
    }

    public void setHandler(FixedValue handler) {
        this.handler = handler;
    }

    @Override
    public void execute(DelegateExecution execution) {
        TaskState taskState = null;
        if (TracingFacade.isTracingEnabled()) {
            Map<String, String> input = TaskStateUtil.getVariables(execution.getVariables());
            taskState = TracingFacade.taskStarted(TaskType.BPM,
                    execution.getProcessInstanceBusinessKey() != null ? execution.getProcessInstanceBusinessKey()
                            : execution.getProcessInstanceId(),
                    execution.getCurrentFlowElement()
                             .getName(),
                    input);
            taskState.setDefinition(execution.getProcessDefinitionId());
            taskState.setInstance(execution.getProcessInstanceId());
            taskState.setTenant(execution.getTenantId());
        }
        Tracer tracer = OpenTelemetryProvider.get()
                                             .getTracer("eclipse-dirigible");
        Span span = tracer.spanBuilder("flowable_java_task_execution")
                          .startSpan();
        try (Scope scope = span.makeCurrent()) {
            addSpanAttributes(execution, span);

            executeInternal(execution);

            if (TracingFacade.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(execution.getVariables());
                TracingFacade.taskSuccessful(taskState, output);
            }
        } catch (RuntimeException e) {
            if (TracingFacade.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(execution.getVariables());
                TracingFacade.taskFailed(taskState, output, e.getMessage());
            }
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred during Java task execution");

            throw e;
        } finally {
            span.end();
        }
    }

    private void addSpanAttributes(DelegateExecution execution, Span span) {
        span.setAttribute("execution.id", execution.getId());
        span.setAttribute("process.instance.id", execution.getProcessInstanceId());
        span.setAttribute("process.instance.business.key", execution.getProcessInstanceBusinessKey());
        span.setAttribute("process.definition.id", execution.getProcessDefinitionId());
    }

    private void executeInternal(DelegateExecution execution) {
        String action = (String) execution.getVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP);
        if (SKIP.getActionName()
                .equals(action)) {
            execution.removeVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP);
            return;
        }

        if (handler == null) {
            throw new BpmnRuntimeException("Handler cannot be null at the Java call delegate.");
        }
        String fqn = handler.getExpressionText();
        if (fqn == null || fqn.isBlank()) {
            throw new BpmnRuntimeException("Handler cannot be blank at the Java call delegate.");
        }
        String tenantId = getTenantId(execution);
        executeJavaHandlerInTenantContext(tenantId, fqn.trim(), execution);
    }

    private static String getTenantId(DelegateExecution execution) {
        String tenantId = execution.getTenantId();
        if (null == tenantId) {
            throw new IllegalStateException("Missing tenant id for execution with id [" + execution.getId() + "], process instance id ["
                    + execution.getProcessInstanceId() + "] and process definition id [" + execution.getProcessDefinitionId() + "]");
        }
        return tenantId;
    }

    @Transactional
    private void executeJavaHandlerInTenantContext(String tenantId, String fqn, DelegateExecution execution) {
        tenantContext.execute(tenantId, () -> {
            executeJavaHandler(fqn, execution);
            return null;
        });
    }

    private void executeJavaHandler(String fqn, DelegateExecution execution) {
        Span.current()
            .setAttribute("handler", fqn)
            .setAttribute("tenantId", tenantContext.getCurrentTenant()
                                                   .getId());

        ClientClassLoader loader = clientClassLoaderHolder.current();
        if (loader == null) {
            throw new BpmnRuntimeException("No client Java code has been compiled yet; cannot resolve handler [" + fqn
                    + "]. Add a .java source under " + "the project's registry path and let the synchronizer pick it up.");
        }

        Class<?> handlerClass;
        try {
            handlerClass = loader.loadClass(fqn);
        } catch (ClassNotFoundException e) {
            throw new BpmnRuntimeException("Client Java class [" + fqn + "] is not loaded. Ensure the source is present under "
                    + "/registry/public/<project>/ and compiles successfully.", e);
        }

        if (!JavaDelegate.class.isAssignableFrom(handlerClass)) {
            throw new BpmnRuntimeException("Client Java class [" + fqn + "] does not implement " + JavaDelegate.class.getName() + ".");
        }

        JavaDelegate delegate;
        try {
            delegate = (JavaDelegate) handlerClass.getDeclaredConstructor()
                                                  .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new BpmnRuntimeException(
                    "Failed to instantiate client Java class [" + fqn + "]. A public no-arg " + "constructor is required.", e);
        }

        delegate.execute(execution);
    }

}

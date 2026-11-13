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

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.tenant.TenantConfigurationScope;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * The {@link ClassDelegate} every {@code flowable:class} element runs through - a service task, and
 * since #7222 an execution or task listener too (all created by
 * {@link ResilientClassDelegateFactory}).
 *
 * <p>
 * On a service task it adds the intent DSL's step resilience: when the delegate's FINAL failed
 * attempt happens on a task carrying an intent {@code onError} error boundary, the failure is
 * converted into the caught BPMN error instead of dead-lettering - see
 * {@link IntentStepResilience}. A {@code BpmnError} the delegate throws itself, and any failure on
 * a task without the intent boundary, keep the stock behaviour (the superclass handles both).
 *
 * <p>
 * It is also where a client class gets its collaborators: {@link #instantiateDelegate} routes the
 * class through the client bean container, so a {@code flowable:class} delegate <em>or
 * listener</em> is wired like every other client class - see {@link ClientDelegateBeans}. A
 * listener's failure keeps the stock behaviour: {@link #execute} is the service-task entry point,
 * and Flowable's own {@code notify} paths never reach it, so nothing about a listener is converted
 * into a step error.
 *
 * <p>
 * Every entry point that runs the client class - the service task's {@link #execute} and
 * {@link #trigger}, and both listener kinds' {@code notify} - runs it in the scope of the tenant
 * stamped on its execution or task, with that tenant's configuration overrides (#8549), exactly as
 * {@code ${JavaTask}} does. Synchronously the thread usually carries the request's tenant already;
 * a step on the async executor carries none, and without the scope its repositories resolved to the
 * default tenant's schema.
 */
class ResilientClassDelegate extends ClassDelegate {

    ResilientClassDelegate(String id, String className, List<FieldDeclaration> fieldDeclarations, boolean triggerable,
            Expression skipExpression, List<MapExceptionEntry> mapExceptions) {
        super(id, className, fieldDeclarations, triggerable, skipExpression, mapExceptions);
    }

    ResilientClassDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        super(className, fieldDeclarations);
    }

    /**
     * Construct the delegate through the client bean container (constructor and {@code @Inject} field
     * injection over the container's singletons), falling back to Flowable's own reflective
     * instantiation when the class declares no injection point.
     *
     * <p>
     * Two things are deliberate. The {@code <flowable:field>} declarations are applied <b>last</b>, so
     * a BPMN-declared literal still wins for its own field, as it always did - a {@code fields:} name
     * and an injected member must therefore not collide. And an unsatisfiable dependency throws from
     * here, which Flowable calls lazily from {@code getActivityBehaviorInstance()} inside
     * {@link #execute}: the failure lands on the step, routed by its {@code retry:} / {@code onError:},
     * never on the deployment.
     */
    @Override
    protected Object instantiateDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        Class<?> type = ReflectUtil.loadClass(className);
        Optional<?> wired = ClientDelegateBeans.createUnmanaged(type);
        Object instance = wired.isPresent() ? wired.get() : defaultInstantiateDelegate(type, List.of());
        applyFieldDeclaration(fieldDeclarations, instance);
        return instance;
    }

    @Override
    public void execute(DelegateExecution execution) {
        try {
            inTenantScope(execution.getTenantId(), describe(execution), () -> super.execute(execution));
        } catch (RuntimeException | Error failure) {
            // A BpmnError never reaches here (the superclass propagates it to its boundary itself);
            // this is a plain failure, normally destined for the retry cycle / dead-letter path. An
            // Error passes the superclass untouched, so it is caught here too (#7840).
            if (!IntentStepResilience.convertFinalFailure(execution, failure)) {
                throw failure;
            }
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        inTenantScope(execution.getTenantId(), describe(execution), () -> super.trigger(execution, signalName, signalData));
    }

    @Override
    public void notify(DelegateExecution execution) {
        inTenantScope(execution.getTenantId(), describe(execution), () -> super.notify(execution));
    }

    @Override
    public void notify(DelegateTask task) {
        inTenantScope(task.getTenantId(), "task [" + task.getId() + "] in process instance [" + task.getProcessInstanceId() + "]",
                () -> super.notify(task));
    }

    /**
     * Runs the client class in the tenant's scope with the tenant's configuration overrides. A
     * standalone engine (no Spring context) has no tenants, and runs it as it is.
     */
    private static void inTenantScope(String tenantId, String owner, Runnable work) {
        if (!BeanProvider.isInitialzed()) {
            work.run();
            return;
        }
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant id for " + owner);
        }
        TenantContext tenantContext = BeanProvider.getTenantContext();
        Optional<TenantConfigurationScope> configurationScope = BeanProvider.getOptionalBean(TenantConfigurationScope.class);
        tenantContext.execute(tenantId, () -> {
            if (configurationScope.isPresent()) {
                configurationScope.get()
                                  .executeWithTenantOverrides(() -> {
                                      work.run();
                                      return null;
                                  });
            } else {
                work.run();
            }
            return null;
        });
    }

    private static String describe(DelegateExecution execution) {
        return "execution [" + execution.getId() + "] in process instance [" + execution.getProcessInstanceId() + "]";
    }
}

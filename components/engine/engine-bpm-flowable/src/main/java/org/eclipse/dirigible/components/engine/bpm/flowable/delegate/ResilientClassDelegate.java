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

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;

/**
 * The {@link ClassDelegate} every {@code flowable:class} service task runs through (created by
 * {@link ResilientClassDelegateFactory}), adding the intent DSL's step resilience: when the
 * delegate's FINAL failed attempt happens on a task carrying an intent {@code onError} error
 * boundary, the failure is converted into the caught BPMN error instead of dead-lettering - see
 * {@link IntentStepResilience}. A {@code BpmnError} the delegate throws itself, and any failure on
 * a task without the intent boundary, keep the stock behaviour (the superclass handles both).
 *
 * <p>
 * It is also where a client delegate gets its collaborators: {@link #instantiateDelegate} routes
 * the class through the client bean container, so a {@code flowable:class} delegate is wired like
 * every other client class - see {@link ClientDelegateBeans}.
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
            super.execute(execution);
        } catch (RuntimeException exception) {
            // A BpmnError never reaches here (the superclass propagates it to its boundary itself);
            // this is a plain failure, normally destined for the retry cycle / dead-letter path.
            if (!IntentStepResilience.convertFinalFailure(execution, exception)) {
                throw exception;
            }
        }
    }
}

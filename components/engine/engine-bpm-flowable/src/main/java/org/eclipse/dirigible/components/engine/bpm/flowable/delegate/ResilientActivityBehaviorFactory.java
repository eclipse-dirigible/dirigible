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

import org.flowable.bpmn.model.ServiceTask;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.impl.bpmn.behavior.ServiceTaskDelegateExpressionActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ClassDelegateFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;

/**
 * The behaviour factory the engine is configured with, so the intent DSL's {@code onError} error
 * routing has its conversion hook on <em>both</em> service-task paths: the {@code flowable:class}
 * one through the {@link ClassDelegateFactory} this is constructed with (which yields
 * {@link ResilientClassDelegate}s), and the {@code flowable:delegateExpression} one through the
 * override below (which yields {@link ResilientServiceTaskDelegateExpressionActivityBehavior}s).
 *
 * <p>
 * Flowable exposes no {@code ClassDelegateFactory} analogue for the delegate-expression path, so
 * the hook has to be the overridable factory method itself. The construction mirrors the
 * superclass's own exactly - only the behaviour type differs - and {@code expressionManager} is the
 * inherited field the engine's {@code initBehaviorFactory} injects into this instance after
 * configuration.
 *
 * <p>
 * Both {@code ${JavaTask}} and {@code ${JSTask}} therefore run through the conversion. That is
 * wider than the DSL currently allows anyone to ask for, and deliberately harmless: the conversion
 * fires only on a task carrying a boundary event catching {@code INTENT_STEP_FAILED}, which nothing
 * but the intent BPMN generator emits, and which shapes may declare an {@code onError:} at all is
 * decided by the intent parser.
 */
public class ResilientActivityBehaviorFactory extends DefaultActivityBehaviorFactory {

    public ResilientActivityBehaviorFactory(ClassDelegateFactory classDelegateFactory) {
        super(classDelegateFactory);
    }

    @Override
    public ServiceTaskDelegateExpressionActivityBehavior createServiceTaskDelegateExpressionActivityBehavior(ServiceTask serviceTask) {
        Expression delegateExpression = expressionManager.createExpression(serviceTask.getImplementation());
        return new ResilientServiceTaskDelegateExpressionActivityBehavior(serviceTask.getId(), delegateExpression,
                getSkipExpressionFromServiceTask(serviceTask), createFieldDeclarations(serviceTask.getFieldExtensions()),
                serviceTask.getMapExceptions(), serviceTask.isTriggerable());
    }
}

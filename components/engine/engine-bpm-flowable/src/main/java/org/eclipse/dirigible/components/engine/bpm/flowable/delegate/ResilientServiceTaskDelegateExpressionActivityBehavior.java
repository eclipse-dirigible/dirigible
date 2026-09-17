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

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.ServiceTaskDelegateExpressionActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;

/**
 * The behaviour every {@code flowable:delegateExpression} service task runs through (created by
 * {@link ResilientActivityBehaviorFactory}), adding the intent DSL's step resilience to the second
 * of the two service-task paths: when the resolved delegate's FINAL failed attempt happens on a
 * task carrying an intent {@code onError} error boundary, the failure is converted into the caught
 * BPMN error instead of dead-lettering - see {@link IntentStepResilience}.
 *
 * <p>
 * This is the twin of {@link ResilientClassDelegate}, which covers the {@code flowable:class} path.
 * It exists because the generated senders, setters, resolvers and loaders are all bound through the
 * {@code ${JavaTask}} / {@code ${JSTask}} dispatchers, which never pass through
 * {@code ClassDelegate} - so until dirigible #7056 a {@code notify:} step could not declare
 * {@code retry:} / {@code onError:} at all, while its generated sender did fail the task on a
 * delivery error (the send was the one step in a process whose failure had nowhere to go).
 *
 * <p>
 * The hook is {@link #handleException}, the single funnel the superclass routes {@code execute},
 * {@code trigger} and the future-delegate completion through. Wrapping it - rather than
 * re-implementing the catch - is what keeps the stock semantics intact: the superclass propagates a
 * {@link org.flowable.engine.delegate.BpmnError} the delegate threw itself and applies a matching
 * {@code flowable:mapException} on its own, and only rethrows a plain, unmapped failure. That
 * rethrow is exactly the failure destined for the retry cycle / dead-letter path, i.e. the same
 * predicate the {@code flowable:class} twin relies on.
 */
class ResilientServiceTaskDelegateExpressionActivityBehavior extends ServiceTaskDelegateExpressionActivityBehavior {

    private static final long serialVersionUID = 1L;

    ResilientServiceTaskDelegateExpressionActivityBehavior(String serviceTaskId, Expression expression, Expression skipExpression,
            List<FieldDeclaration> fieldDeclarations, List<MapExceptionEntry> mapExceptions, boolean triggerable) {
        super(serviceTaskId, expression, skipExpression, fieldDeclarations, mapExceptions, triggerable);
    }

    @Override
    protected void handleException(Throwable exception, DelegateExecution execution, boolean loggingSessionEnabled) {
        try {
            super.handleException(exception, execution, loggingSessionEnabled);
        } catch (RuntimeException unhandled) {
            if (!IntentStepResilience.convertFinalFailure(execution, unhandled)) {
                throw unhandled;
            }
        }
    }
}

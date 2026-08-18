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
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;

/**
 * The {@link ClassDelegate} every {@code flowable:class} service task runs through (created by
 * {@link ResilientClassDelegateFactory}), adding the intent DSL's step resilience: when the
 * delegate's FINAL failed attempt happens on a task carrying an intent {@code onError} error
 * boundary, the failure is converted into the caught BPMN error instead of dead-lettering - see
 * {@link IntentStepResilience}. A {@code BpmnError} the delegate throws itself, and any failure on
 * a task without the intent boundary, keep the stock behaviour (the superclass handles both).
 */
class ResilientClassDelegate extends ClassDelegate {

    ResilientClassDelegate(String id, String className, List<FieldDeclaration> fieldDeclarations, boolean triggerable,
            Expression skipExpression, List<MapExceptionEntry> mapExceptions) {
        super(id, className, fieldDeclarations, triggerable, skipExpression, mapExceptions);
    }

    ResilientClassDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        super(className, fieldDeclarations);
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

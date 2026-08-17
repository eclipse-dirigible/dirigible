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
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.bpmn.helper.ClassDelegateFactory;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;

/**
 * Creates {@link ResilientClassDelegate}s for every {@code flowable:class} service task (the shape
 * of Flowable's own {@code DefaultClassDelegateFactory}), so the intent DSL's {@code onError} error
 * routing has its conversion hook on the one path all {@code delegate:} steps run through. Wired
 * into the engine by {@code BpmFlowableConfig} via a {@code DefaultActivityBehaviorFactory}
 * carrying this factory.
 */
public class ResilientClassDelegateFactory implements ClassDelegateFactory {

    @Override
    public ClassDelegate create(String id, String className, List<FieldDeclaration> fieldDeclarations, boolean triggerable,
            Expression skipExpression, List<MapExceptionEntry> mapExceptions) {
        return new ResilientClassDelegate(id, className, fieldDeclarations, triggerable, skipExpression, mapExceptions);
    }

    @Override
    public ClassDelegate create(String className, List<FieldDeclaration> fieldDeclarations) {
        return new ResilientClassDelegate(className, fieldDeclarations);
    }
}

/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

import java.nio.file.Path;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * The JUnit side of {@link IntentSlice}: starts each project's application once per JVM (closed
 * with the JVM's root context), makes it the live one for the test about to run, resets it before
 * each test, and hands the test its {@link Slice} - or any of the application's components - as a
 * parameter.
 */
public final class IntentSliceExtension implements BeforeEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(IntentSliceExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        runtime(context).reset();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter()
                                        .getType();
        return type == Slice.class || runtime(extensionContext).container()
                                                               .get(type)
                                                               .isPresent();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter()
                                        .getType();
        Slice slice = runtime(extensionContext).slice();
        return type == Slice.class ? slice : slice.bean(type);
    }

    private static SliceRuntime runtime(ExtensionContext context) {
        Path project = Path.of("")
                           .toAbsolutePath()
                           .resolve(annotation(context).project())
                           .normalize();
        SliceRuntime runtime = context.getRoot()
                                      .getStore(NAMESPACE)
                                      .computeIfAbsent(project, SliceRuntime::new, SliceRuntime.class);
        runtime.activate();
        return runtime;
    }

    private static IntentSlice annotation(ExtensionContext context) {
        for (Class<?> type = context.getRequiredTestClass(); type != null; type = type.getEnclosingClass()) {
            var annotation = AnnotationSupport.findAnnotation(type, IntentSlice.class);
            if (annotation.isPresent()) {
                return annotation.get();
            }
        }
        throw new ExtensionContextException(
                IntentSliceExtension.class.getSimpleName() + " needs @" + IntentSlice.class.getSimpleName() + " on the test class");
    }
}

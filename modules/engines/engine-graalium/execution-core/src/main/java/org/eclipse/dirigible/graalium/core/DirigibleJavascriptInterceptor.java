/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core;

import java.nio.file.Path;

import org.eclipse.dirigible.graalium.core.javascript.GraalJSInterceptor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * The Class DirigibleJavascriptInterceptor.
 */
public class DirigibleJavascriptInterceptor implements GraalJSInterceptor {

    /** The code runner. */
    private DirigibleJavascriptCodeRunner codeRunner;

    /**
     * Gets the code runner.
     *
     * @return the code runner
     */
    public DirigibleJavascriptCodeRunner getCodeRunner() {
        return codeRunner;
    }

    /**
     * Instantiates a new dirigible javascript interceptor.
     *
     * @param codeRunner the code runner
     */
    public DirigibleJavascriptInterceptor(DirigibleJavascriptCodeRunner codeRunner) {
        this.codeRunner = codeRunner;
    }

    /**
     * On before run.
     *
     * @param sourceFilePath the source file path
     * @param absoluteSourcePath the absolute source path
     * @param source the source
     * @param context the context
     */
    @Override
    public void onBeforeRun(String sourceFilePath, Path absoluteSourcePath, Source source, Context context) {

    }

    /**
     * On after run.
     *
     * @param sourceFilePath the source file path
     * @param absoluteSourcePath the absolute source path
     * @param source the source
     * @param context the context
     * @param value the value
     */
    @Override
    public void onAfterRun(String sourceFilePath, Path absoluteSourcePath, Source source, Context context, Value value) {

    }

}

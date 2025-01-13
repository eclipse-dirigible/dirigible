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

/**
 * The Interface CodeRunner.
 *
 * @param <TSource> the generic type
 * @param <TResult> the generic type
 */
public interface CodeRunner<TSource, TResult> extends AutoCloseable {

    /**
     * Prepare the source.
     *
     * @param codeFilePath the code file path
     * @return the source
     */
    TSource prepareSource(Path codeFilePath);

    /**
     * Run.
     *
     * @param codeSource the code source
     * @return the t result
     */
    TResult run(TSource codeSource);

    /**
     * Close.
     */
    @Override
    void close();
}

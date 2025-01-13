/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.process.execution.output;

/**
 * The Class ProcessResult.
 *
 * @param <TOut> the generic type
 */
public class ProcessResult<TOut> {

    /** The exit code. */
    private final int exitCode;

    /** The output. */
    private final TOut output;

    /**
     * Instantiates a new process result.
     *
     * @param exitCode the exit code
     * @param output the output
     */
    public ProcessResult(int exitCode, TOut output) {
        this.exitCode = exitCode;
        this.output = output;
    }

    /**
     * Gets the exit code.
     *
     * @return the exit code
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Gets the process outputs.
     *
     * @return the process outputs
     */
    public TOut getProcessOutputs() {
        return output;
    }
}

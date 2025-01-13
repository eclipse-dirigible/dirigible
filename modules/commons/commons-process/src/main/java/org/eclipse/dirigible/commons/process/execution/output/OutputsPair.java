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
 * The Class OutputsPair.
 */
public class OutputsPair {

    /** The standard output. */
    private final String standardOutput;

    /** The error output. */
    private final String errorOutput;

    /**
     * Instantiates a new outputs pair.
     *
     * @param standardOutput the standard output
     * @param errorOutput the error output
     */
    public OutputsPair(String standardOutput, String errorOutput) {
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
    }

    /**
     * Gets the standard output.
     *
     * @return the standard output
     */
    public String getStandardOutput() {
        return standardOutput;
    }

    /**
     * Gets the error output.
     *
     * @return the error output
     */
    public String getErrorOutput() {
        return errorOutput;
    }
}

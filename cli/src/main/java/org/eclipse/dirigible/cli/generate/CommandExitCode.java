/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.generate;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * The process exit code a command asks for. A failing command answers through it rather than by
 * throwing: the shell would turn an exception into a stack trace, and a CI step reading {@code
 * generate --check} wants the diff and a status, nothing else.
 */
@Component
class CommandExitCode implements ExitCodeGenerator {

    private volatile int exitCode;

    /**
     * Sets the exit code the process ends with.
     *
     * @param exitCode the exit code
     */
    void set(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}

/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.process.execution;

/**
 * The Class ProcessExecutionException.
 */
public class ProcessExecutionException extends RuntimeException {

    /**
     * Instantiates a new process execution exception.
     *
     * @param cause the cause
     */
    public ProcessExecutionException(Throwable cause) {
        super(cause);
    }
}

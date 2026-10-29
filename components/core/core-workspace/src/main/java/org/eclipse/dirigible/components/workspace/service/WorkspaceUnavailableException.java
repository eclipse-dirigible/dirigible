/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.workspace.service;

/**
 * Thrown where an operation needs the workspaces of the IDE and the IDE is not deployed on the
 * instance.
 */
public class WorkspaceUnavailableException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new exception for an operation that cannot run.
     *
     * @param operation the operation that needs the workspaces
     */
    public WorkspaceUnavailableException(String operation) {
        super("The operation [" + operation + "] needs the workspaces of the IDE, which is not deployed on this instance");
    }
}

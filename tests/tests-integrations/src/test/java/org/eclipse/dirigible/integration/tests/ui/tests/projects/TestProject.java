/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.projects;

public interface TestProject {

    /**
     * Opens IDE and publishes the project.
     */
    void publish();

    /**
     * Copy project files to the workspace
     */
    void copyToWorkspace();

    /**
     * Opens IDE and publishes the project. If waitForSynchronizationExecution is true, it waits for
     * synchronization to complete its execution.
     *
     * @param waitForSynchronizationExecution whether to wait for synchronization execution
     */
    void publish(boolean waitForSynchronizationExecution);

    /**
     * Verify test project is working
     */
    void verify();

}

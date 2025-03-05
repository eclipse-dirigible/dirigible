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
     * Configure the test project. Add logic like regenerate edm, forms, copy the project to workspace,
     * publish the project, etc.
     */
    void configure();

    /**
     * Execute all the needed steps to configure and verify the project to assert that it works
     * properly.
     */
    void test() throws Exception;

    /**
     * Verify test project is working. Perform the test scenario and assert that it the project works as
     * expected.
     */
    void verify() throws Exception;

    /**
     * Implement cleanup logic if needed
     */
    void cleanup();
}

/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.projects;

import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IntegrationTest;
import org.eclipse.dirigible.tests.framework.Browser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class PredefinedProjectIT extends IntegrationTest {

    @Autowired
    protected Browser browser;

    @Autowired
    protected IDE ide;

    @Test
    final void test() throws Exception {
        getTestProject().test();
    }

    abstract protected TestProject getTestProject();

    @AfterEach
    final void cleanupTestProject() {
        getTestProject().cleanup();
    }
}

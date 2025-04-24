/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.PredefinedProjectIT;
import org.eclipse.dirigible.tests.base.TestProject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test replaces the packaged test project which was part of the Dirigible distribution. It
 * just publishes the test project and asserts that the synchronization passes.
 */
public class DirigibleTestProjectIT extends PredefinedProjectIT {

    @Autowired
    private DirigibleTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}

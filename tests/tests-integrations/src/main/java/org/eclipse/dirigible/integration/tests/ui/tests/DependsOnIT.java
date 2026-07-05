/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("Superseded by the Harmonia+Java DependsOnHarmoniaIT, which covers the same Country->City "
        + "depends-on cascade against the modern generated UI (~52s vs ~16min here, and without the "
        + "AngularJS IDE perspective-load flakiness). Kept in the tree while the AngularJS stack is " + "still around.")
public class DependsOnIT extends PredefinedProjectIT {

    @Autowired
    private DependsOnTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}

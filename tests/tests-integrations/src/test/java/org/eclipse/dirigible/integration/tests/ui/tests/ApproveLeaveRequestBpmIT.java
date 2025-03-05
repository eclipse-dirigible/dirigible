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

import org.eclipse.dirigible.integration.tests.ui.tests.projects.PredefinedProjectIT;
import org.eclipse.dirigible.integration.tests.ui.tests.projects.TestProject;
import org.eclipse.dirigible.tests.mail.GreenMailConfig;
import org.springframework.beans.factory.annotation.Autowired;

class ApproveLeaveRequestBpmIT extends PredefinedProjectIT {

    static {
        GreenMailConfig.configureDirigibleEmailService();
    }

    @Autowired
    private ApproveLeaveRequestTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}

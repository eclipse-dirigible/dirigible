/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.camel;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.eclipse.dirigible.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Lazy
@Component
class CamelDirigibleTwoStepsJSInvokerCronRouteTestProject extends BaseTestProject {

    private final LogsAsserter logsAsserter;

    CamelDirigibleTwoStepsJSInvokerCronRouteTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super("CamelDirigibleTwoStepsJSInvokerCronRouteIT", ide, projectUtil, edmView);
        this.logsAsserter = new LogsAsserter("TwoStepsLogger", Level.INFO);
    }

    @Override
    public void verify() throws SQLException {
        // this log message is expected to be logged by the final camel log step
        await().atMost(20, TimeUnit.SECONDS)
               .until(() -> logsAsserter.containsMessage("Completed execution. Body: [THIS IS A TEST BODY]", Level.INFO));
    }

}

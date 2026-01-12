/*
 * Copyright (c) 2022 codbex or an codbex affiliate company and contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 codbex or an codbex affiliate company and contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class JobDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private LogsAsserter consoleLogAsserter;

    @BeforeEach
    void setUp() {
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Override
    protected void verifyProject() {
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(3, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("MyJob executed!", Level.INFO));
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-job-decorator.git";
    }

}

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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.integration.tests.ui.tests.projects.PredefinedProjectIT;
import org.eclipse.dirigible.integration.tests.ui.tests.projects.TestProject;
import org.springframework.beans.factory.annotation.Autowired;

class MailIT extends PredefinedProjectIT {

    static {
        configureEmail();
    }

    @Autowired
    private MailITTestProject testProject;

    private static void configureEmail() {
        DirigibleConfig.MAIL_USERNAME.setStringValue(org.eclipse.dirigible.integration.tests.ui.tests.MailITTestProject.MAIL_USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(org.eclipse.dirigible.integration.tests.ui.tests.MailITTestProject.MAIL_PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_PORT.setIntValue(org.eclipse.dirigible.integration.tests.ui.tests.MailITTestProject.MAIL_PORT);
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
    }

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}

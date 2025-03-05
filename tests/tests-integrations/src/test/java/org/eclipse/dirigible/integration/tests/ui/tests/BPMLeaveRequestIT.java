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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BPMLeaveRequestIT extends UserInterfaceIntegrationTest {

    static {
        configureEmail();
    }

    @Autowired
    private DeclineLeaveRequestTestProject declineLeaveRequestTestProject;

    @Autowired
    private ApproveLeaveRequestTestProject approveLeaveRequestTestProject;

    private static void configureEmail() {
        DirigibleConfig.MAIL_USERNAME.setStringValue(BPMLeaveRequestTestProject.MAIL_USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(BPMLeaveRequestTestProject.MAIL_PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_PORT.setIntValue(BPMLeaveRequestTestProject.MAIL_PORT);
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
    }

    @Test
    void testApproveLeaveRequest() throws Exception {
        approveLeaveRequestTestProject.test();
    }

    @Test
    void testDeclineLeaveRequest() throws Exception {
        declineLeaveRequestTestProject.test();
    }

    @AfterEach
    void tearDown() {
        approveLeaveRequestTestProject.cleanup();
        declineLeaveRequestTestProject.cleanup();
    }
}

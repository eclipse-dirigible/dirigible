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
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IDEFactory;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.SecurityUtil;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CustomSecurityIT extends UserInterfaceIntegrationTest {

    private static final String EMPLOYEE_ROLE = "employee";
    private static final String EMPLOYEE_USERNAME = "test-employee";

    private static final String EMPLOYEE_MANAGER_ROLE = "employee-manager";
    private static final String EMPLOYEE_MANAGER_USERNAME = "test-employee-manager";

    private static final String PROTECTED_PAGE_PATH = "/services/web/dirigible-test-project/security/protected_page.html";
    private static final String PROTECTED_PAGE_HEADER = "This is a protected page";

    @Autowired
    private TestProject testProject;

    @Autowired
    private IDEFactory ideFactory;

    @Autowired
    private SecurityUtil securityUtil;

    @BeforeEach
    void setUp() {
        testProject.publish();
        browser.clearCookies();

        // wait some time synchronizers to complete their execution
        SleepUtil.sleepSeconds(12);
    }

    @Test
    void testAccessProtectedPage_withUserWithRole() {
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME, EMPLOYEE_ROLE);

        IDE ide = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME);
        ide.openPath(PROTECTED_PAGE_PATH);
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER1, PROTECTED_PAGE_HEADER);
    }

    @Test
    void testAccessProtectedPage_withUserWithoutRole() {
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_ROLE);

        IDE ide = ideFactory.create(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME);
        ide.openPath(PROTECTED_PAGE_PATH);
        browser.assertElementExistsByTypeAndText(HtmlElementType.DIV, "Access Denied");
    }

}

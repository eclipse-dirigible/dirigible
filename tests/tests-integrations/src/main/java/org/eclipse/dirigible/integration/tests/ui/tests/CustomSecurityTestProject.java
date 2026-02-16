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

import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.util.SleepUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class CustomSecurityTestProject extends BaseTestProject {

    private static final String EMPLOYEE_ROLE = "employee";
    private static final String EMPLOYEE_USERNAME = "test-employee";
    private static final String EMPLOYEE_PASSWORD = "test-employee-password";

    private static final String EMPLOYEE_MANAGER_ROLE = "employee-manager";
    private static final String EMPLOYEE_MANAGER_USERNAME = "test-employee-manager";
    private static final String EMPLOYEE_MANAGER_PASSWORD = "test-employee-manager-password";

    private static final String PROTECTED_PAGE_PATH = "/services/web/CustomSecurityIT/security/protected_page.html";
    private static final String PROTECTED_PAGE_HEADER = "This is a protected page";

    private final SecurityUtil securityUtil;
    private final IDEFactory ideFactory;

    CustomSecurityTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, SecurityUtil securityUtil, IDEFactory ideFactory) {
        super("CustomSecurityIT", ide, projectUtil, edmView);
        this.securityUtil = securityUtil;
        this.ideFactory = ideFactory;
    }

    @Override
    public void verify() {
        // SleepUtil.sleepSeconds(10000);
        testAccessProtectedPageWithUserWithoutRole();
        testAccessProtectedPageWithUserWithRole();
    }

    private void testAccessProtectedPageWithUserWithRole() {
        securityUtil.createUserInDefaultTenant(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD, EMPLOYEE_ROLE);

        IDE ide = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);
        ide.openPath(PROTECTED_PAGE_PATH);
        ide.getBrowser()
           .assertElementExistsByTypeAndText(HtmlElementType.HEADER1, PROTECTED_PAGE_HEADER);
        ide.close();
    }

    private void testAccessProtectedPageWithUserWithoutRole() {
        securityUtil.createUserInDefaultTenant(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD, EMPLOYEE_MANAGER_ROLE);

        IDE ide = ideFactory.create(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD);
        ide.openPath(PROTECTED_PAGE_PATH);
        ide.getBrowser()
           .assertElementExistsByTypeAndText(HtmlElementType.DIV, "Access Denied");
        ide.close();
    }

}

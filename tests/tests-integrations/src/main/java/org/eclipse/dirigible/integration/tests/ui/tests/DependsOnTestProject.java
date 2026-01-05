/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class DependsOnTestProject extends BaseTestProject {

    private static final String EDM_FILE_NAME = "edm.edm";
    private static final String PROJECT_RESOURCES_PATH = "DependsOnIT";
    private static final String VERIFICATION_URI = "/services/web/dashboard/index.html";

    private final Browser browser;

    DependsOnTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        super(PROJECT_RESOURCES_PATH, ide, projectUtil, edmView);
        this.browser = browser;
    }

    @Override
    public void configure() {
        copyToWorkspace();
        generateEDM(EDM_FILE_NAME);
        publish();
    }

    @Override
    public void verify() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.SPAN, "Orders");
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");
        browser.enterTextInElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Country...", "Bulgaria");

        // click out of the input field to trigger the search
        browser.clickOnElementContainingText(HtmlElementType.HEADER1, "Create Order");

        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search City...");

        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Sofia");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Varna");
        browser.assertElementDoesNotExistsByTypeAndContainsText(HtmlElementType.SPAN, "Milano");
    }
}

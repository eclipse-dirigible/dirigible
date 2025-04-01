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

import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Lazy
@Component
class DependsOnScenariosTestProject extends BaseTestProject {

    private static final String PROJECT_RESOURCES_PATH = "DependsOnScenariosTestProject";
    private static final String VERIFICATION_URI = "/services/web/" + PROJECT_RESOURCES_PATH + "/gen/sales-order/ui/Customer/index.html";

    private final Browser browser;

    DependsOnScenariosTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        super(PROJECT_RESOURCES_PATH, ide, projectUtil, edmView);
        this.browser = browser;
    }

    @Override
    public void configure() {
        copyToWorkspace();
        publish();

    }

    @Override
    public void verify() {
        browser.openPath(VERIFICATION_URI);

        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.GLYPH, "sap-icon--add", HtmlAttribute.CLASS, "fd-button fd-button--transparent fd-button--compact"));
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button", HtmlAttribute.NGCLICK, "refreshCountry()"));

        // Bulgaria should only have Sofia
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Country ...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", "Bulgaria");
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button", HtmlAttribute.NGCLICK, "refreshCity()"));
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search City ...");

        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Sofia");

        // Italy should only have Rome
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Country ...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-list__title", "Italy");
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button", HtmlAttribute.NGCLICK, "refreshCity()"));
        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search City ...");

        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Rome");
    }
}

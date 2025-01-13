/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class Workbench {

    public static final String PROJECTS_VIEW_ID = "dgProjects";
    public static final String PROJECT_NAME_INPUT_ID = "pgfi1";
    private static final String PROJECTS_CONTEXT_MENU_NEW_PROJECT = "New Project";
    private static final String CREATE_PROJECT_BUTTON_TEXT = "Create";
    private final Browser browser;

    public Workbench(Browser browser) {
        this.browser = browser;
    }

    public void expandProject(String projectName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, projectName);
    }

    public void openFile(String fileName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, fileName);
    }

    public void publishAll() {
        clickPublishAll();
        browser.assertElementExistsByTypeAndTextPattern(HtmlElementType.SPAN, "Published all projects in");
    }

    public void clickPublishAll() {
        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Publish all");
    }

    public WelcomeView openWelcomeView() {
        focusOnOpenedFile("Welcome");
        return new WelcomeView(browser);
    }

    public WelcomeView focusOnOpenedFile(String fileName) {
        browser.clickOnElementContainingText(HtmlElementType.ANCHOR, fileName);
        return new WelcomeView(browser);
    }

    public FormView getFormView() {
        return new FormView(browser);
    }

    public void createNewProject(String projectName) {
        browser.rightClickOnElementById(PROJECTS_VIEW_ID);

        browser.clickOnElementContainingText(HtmlElementType.SPAN, PROJECTS_CONTEXT_MENU_NEW_PROJECT);

        browser.enterTextInElementById(PROJECT_NAME_INPUT_ID, projectName);

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, CREATE_PROJECT_BUTTON_TEXT);
    }

}

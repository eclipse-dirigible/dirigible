/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;

public class Workbench {

    public static final String PROJECTS_VIEW_ID = "pvtree";
    public static final String PROJECT_NAME_INPUT_ID = "pgfi1";
    public static final String FILE_NAME_INPUT_ID = "fdti1";
    private static final String PROJECTS_CONTEXT_MENU_NEW_PROJECT = "New Project";
    private static final String CREATE_BUTTON_TEXT = "Create";

    private final Browser browser;
    private final WelcomeViewFactory welcomeViewFactory;
    private final TerminalFactory terminalFactory;

    protected Workbench(Browser browser, WelcomeViewFactory welcomeViewFactory, TerminalFactory terminalFactory) {
        this.browser = browser;
        this.welcomeViewFactory = welcomeViewFactory;
        this.terminalFactory = terminalFactory;
    }

    public void publishAll(boolean waitForSynchronizationExecution) {
        clickPublishAll();
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Published all projects in");

        if (waitForSynchronizationExecution) {
            SynchronizationUtil.waitForSynchronizationExecution();
        }
    }

    public void clickPublishAll() {
//        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Publish all");
        browser.rightClickOnElementContainingText(HtmlElementType.ANCHOR, "test.mjs");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-menu__title", "Publish");
    }

    public WelcomeView openWelcomeView() {
        focusOnOpenedFile("Welcome");
        return welcomeViewFactory.create(browser);
    }

    public WelcomeView focusOnOpenedFile(String fileName) {
        browser.clickOnElementContainingText(HtmlElementType.ANCHOR, fileName);
        return welcomeViewFactory.create(browser);
    }

    public FormView getFormView() {
        return new FormView(browser);
    }

    public void createNewProject(String projectName) {
        browser.rightClickOnElementById(PROJECTS_VIEW_ID);

        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.ROLE, "menuitem",
                PROJECTS_CONTEXT_MENU_NEW_PROJECT);

        browser.enterTextInElementById(PROJECT_NAME_INPUT_ID, projectName);

        browser.clickOnElementWithText(HtmlElementType.BUTTON, CREATE_BUTTON_TEXT);
    }

    public void createFileInProject(String projectName, String newFileType) {
        expandProject(projectName);
        browser.rightClickOnElementContainingText(HtmlElementType.ANCHOR, projectName);

        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-menu__title", newFileType);
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "CREATE_BUTTON_TEXT");
    }

    public void expandProject(String projectName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, projectName);
    }

    public void openFile(String projectName, String fileName) {
        expandProject(projectName);
        openFile(fileName);
    }

    public void openFile(String fileName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, fileName);
    }

    public Terminal openTerminal() {
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Terminal");
        return terminalFactory.create(browser);
    }

    public void addContentToFile(String content) {
        browser.clickOnElementWithExactClass(HtmlElementType.DIV, "view-line");

        browser.type(content);
    }

    public void createCustomElementInProject(String projectName, String fileName, String elementType) {
        browser.rightClickOnElementContainingText(HtmlElementType.ANCHOR, projectName);
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-menu__title", elementType);

        browser.enterTextInElementById(FILE_NAME_INPUT_ID, fileName);
        browser.clickOnElementWithText(HtmlElementType.BUTTON, CREATE_BUTTON_TEXT);
    }

    public void saveAll() {
        browser.clickOnElementByAttributeValue(HtmlElementType.BUTTON, HtmlAttribute.GLYPH, "sap-icon--save");
    }

}


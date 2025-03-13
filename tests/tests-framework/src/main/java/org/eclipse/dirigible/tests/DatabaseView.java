/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
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
import org.openqa.selenium.Keys;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DatabaseView {
    private final Browser browser;

    protected DatabaseView(Browser browser) {
        this.browser = browser;
    }

    public void expandSubviews() {
        expandSchema("PUBLIC");
        expandSchema("Tables");
    }

    public void assertAvailabilityOfSubitems() {
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Tables");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Views");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Procedures");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Functions");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Sequences");
    }

    public void assertEmptyTable() {
        // browser.rightClickOnElementContainingText(HtmlElementType.ANCHOR, "STUDENT");
        browser.rightClickOnElementById("j1_215_anchor"); // Student table id
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Show contents");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.CLASS, "fd-message-page__title",
                "Empty result");
    }

    public void assertResult() {
        browser.rightClickOnElementById("j1_215_anchor"); // Student table id
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Show contents");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.CLASS, "tdSingleLine", "1"); // Assert if
                                                                                                                            // table ID is 1
                                                                                                                            // ->
                                                                                                                            // successfully
                                                                                                                            // inserted
    }

    private void expandSchema(String schemaName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, schemaName);
    }

    public void createTestTable() {
        String testCreateTableStatement = "CREATE TABLE IF NOT EXISTS STUDENT (" + " id SERIAL PRIMARY KEY, " + " name TEXT NOT NULL, "
                + " address TEXT NOT NULL" + ");";
        insertIntoEditor(testCreateTableStatement);
        browser.pressMultipleKeys(Keys.COMMAND, "a");
        browser.pressKey(Keys.F8);
    }

    public void createTestRecord() {
        String testInsertStatement = "INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')";
        insertIntoEditor(testInsertStatement);
        browser.pressMultipleKeys(Keys.COMMAND, "a");
        browser.pressKey(Keys.F8);
    }

    private void insertIntoEditor(String text) {
        // Click in the editor to focus it. Does not work with browser.enterText...
        browser.clickOnElementWithExactClass(HtmlElementType.DIV, "view-line");

        browser.pressMultipleKeys(Keys.COMMAND, "a");
        browser.pressKey(Keys.DELETE);

        browser.type(text);
    }
}


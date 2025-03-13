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

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.openqa.selenium.Keys;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

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

        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button fd-button--transparent", HtmlAttribute.TITLE, "Refresh"));
    }

    public void assertAvailabilityOfSubitems() {
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Tables");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Views");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Procedures");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Functions");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, "Sequences");
    }

    public void assertEmptyTable() {
        browser.rightClickOnElementByText(HtmlElementType.ANCHOR, "STUDENT");
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Show contents");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.CLASS, "fd-message-page__title",
                "Empty result");
    }

    public void assertResult() {
        browser.rightClickOnElementByText(HtmlElementType.ANCHOR, "STUDENT");
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Show contents");

        // Assert if table id is 1 -> correct insertion
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.CLASS, "tdSingleLine", "1");
    }

    private void expandSchema(String schemaName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, schemaName);
    }

    public void createTestTable() {
        insertIntoEditor("CREATE TABLE IF NOT EXISTS STUDENT (" + " id SERIAL PRIMARY KEY, " + " name TEXT NOT NULL, "
                + " address TEXT NOT NULL" + ");");
        selectAll();
        browser.pressKey(Keys.F8);
    }

    public void createTestRecord() {
        insertIntoEditor("INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')");

        selectAll();
        browser.pressKey(Keys.F8);
    }

    private void insertIntoEditor(String text) {
        // Click in the editor to focus it. Does not work with browser.enterText...
        browser.clickOnElementWithExactClass(HtmlElementType.DIV, "view-line");

        selectAll();
        browser.pressKey(Keys.DELETE);

        browser.type(text);
    }

    private void selectAll() {
        if (SystemUtils.IS_OS_MAC)
            browser.pressMultipleKeys(Keys.COMMAND, "a");
        else
            browser.pressMultipleKeys(Keys.CONTROL, "a");

    }
}


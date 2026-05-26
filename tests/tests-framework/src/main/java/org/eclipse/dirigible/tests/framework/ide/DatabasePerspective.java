/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import com.codeborne.selenide.Condition;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Lazy
@Component
public class DatabasePerspective {
    private final Browser browser;

    protected DatabasePerspective(Browser browser) {
        this.browser = browser;
    }

    public void expandSubmenu(String schemaName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, schemaName);
    }

    /**
     * Expands the "Tables" tree node and waits for {@code tableName} to appear as a child entry.
     *
     * <p>
     * A plain double-click (used by {@link #expandSubmenu}) can race against the tree's click-to-expand
     * / click-to-collapse toggle: the first half of the double-click expands the node while the second
     * half immediately collapses it, so the child entries never become visible. This method retries
     * with a single click if the double-click does not produce the expected child within a short
     * window.
     */
    public void expandTablesUntilVisible(String tableName) {
        By anchorSelector = By.tagName(HtmlElementType.ANCHOR.getType());

        expandSubmenu("Tables");
        if (browser.findOptionalElementInAllFrames(anchorSelector, 10, Condition.exist, Condition.exactText(tableName))
                   .isPresent()) {
            return;
        }

        // Double-click likely collapsed the node — fall back to single click.
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Tables");
        if (browser.findOptionalElementInAllFrames(anchorSelector, 10, Condition.exist, Condition.exactText(tableName))
                   .isPresent()) {
            return;
        }

        // One more double-click attempt; the caller's own 60 s assertion will handle the rest.
        expandSubmenu("Tables");
    }

    public void assertSubmenu(String submenu) {
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, submenu);
    }

    public void assertEmptyTable(String tableName) {
        showTableContents(tableName);
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.CLASS, "fd-message-page__title",
                "Empty result");
    }

    public void showTableContents(String tableName) {
        browser.rightClickOnElementByText(HtmlElementType.ANCHOR, tableName);
        browser.clickOnElementWithText(HtmlElementType.ANCHOR, "Show contents");
    }

    public void assertHasColumn(String columnName) {
        browser.assertElementExistsByTypeAndText(HtmlElementType.TH, columnName);
    }

    public void assertCellContent(String content) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.CLASS, "tdSingleLine", content);
    }

    public void refreshTables() {
        browser.clickElementByAttributes(HtmlElementType.BUTTON,
                Map.of(HtmlAttribute.CLASS, "fd-button fd-button--transparent", HtmlAttribute.TITLE, "Refresh"));
    }

    public void executeSql(String sql) {
        // Click in the editor to focus it. Does not work with browser.enterText...
        browser.clickOnElementWithExactClass(HtmlElementType.DIV, "view-line");

        selectAll();
        browser.pressKey(Keys.DELETE);

        browser.type(sql);
        selectAll();
        browser.pressKey(Keys.F8);
    }

    private void selectAll() {
        if (SystemUtils.IS_OS_MAC)
            browser.pressMultipleKeys(Keys.COMMAND, "a");
        else
            browser.pressMultipleKeys(Keys.CONTROL, "a");

    }
}


/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for the Database shell. It asserts what only a real browser can: that the Harmonia +
 * Alpine bootstrap completed and each page rendered content produced by its store.
 * <p>
 * This matters more than it looks - a single mistake in the markup (an Alpine binding on an
 * {@code <i x-h-lucide>}, for instance) aborts Alpine's walk with no DOM or server-side symptom,
 * and every binding below that node silently stays raw markup.
 * <p>
 * The page roots carry a stable id, so the assertions do not depend on what the instance happens to
 * have deployed. The one thing that IS depended on is DefaultDB, which every instance has.
 */
public class DatabaseShellIT extends UserInterfaceIntegrationTest {

    private static final String DATABASE_PATH = "/services/web/database/index.html";

    /**
     * A one-row query that needs no table and returns the same thing on every dialect the CI runs. A
     * platform table would have been a stronger read, but the grid derives its columns from the first
     * row, so a table that happens to be empty would assert nothing - and the column NAME is unusable
     * either way, since an unquoted alias folds to upper case on H2 and to lower case on PostgreSQL.
     * The literal is distinctive enough that finding it in the grid means it came from the database and
     * not from the page.
     */
    private static final String SMOKE_QUERY = "SELECT 'dirigible-smoke' AS SMOKE";

    @Test
    void theExplorerReadsTheDefaultDatasource() {
        ide.openPath(DATABASE_PATH);

        // The shell chrome, rendered by Alpine from the shell component. Asserted on the sidebar
        // entry's id: "Explorer" is also the page's toolbar title, so a bare label match is two
        // elements, which the finder rejects.
        browser.assertElementExistsByIdAndContainsText("database-nav-explorer", "Explorer");
        // The datasource picker's options are rendered from /services/data/metadata, so the name
        // being on the page at all means the store's first read resolved and its bindings evaluated.
        // Asserted on the page root rather than on the input's value: the schema names below it
        // differ between H2 and PostgreSQL, and the picker's displayed text is Harmonia's business.
        browser.assertElementExistsByIdAndContainsText("database-explorer-page", "DefaultDB");
    }

    @Test
    void everySectionIsReachable() {
        ide.openPath(DATABASE_PATH);

        openSection("explorer", "Explorer");
        openSection("sql", "SQL Console");
    }

    /**
     * The one end-to-end journey worth a browser: type a statement, run it, and see rows come back. It
     * goes the whole way through the client's statement dispatch, the text/plain POST that the shared
     * JSON fetch client cannot make, and the results grid's arbitrary-column rendering.
     */
    @Test
    void theConsoleRunsAQueryAndRendersTheRows() {
        ide.openPath(DATABASE_PATH);

        browser.clickOnElementById("database-nav-sql");
        // Not enterTextInElementById - that helper looks for an <input>, and the console is a
        // <textarea> (a statement is multi-line by nature).
        browser.enterTextInElementByAttributePattern(HtmlElementType.TEXTAREA, HtmlAttribute.ID, "database-sql-statement", SMOKE_QUERY);
        browser.clickOnElementById("database-sql-run");

        // The value in the grid proves the row travelled all the way from the database into a cell
        // the page built from columns it did not know in advance; the message strip proves the shell
        // counted the rows rather than only drawing a table.
        browser.assertElementExistsByIdAndContainsText("database-sql-results", "dirigible-smoke");
        browser.assertElementExistsByIdAndContainsText("database-sql-message", "Rows: 1");
    }

    /**
     * Click a sidebar entry and assert its page rendered. The entry is addressed by its id, not its
     * label, so a label that also appears in page content cannot satisfy the assertion by accident.
     *
     * @param section the sidebar entry / page name
     * @param expectedText text the page renders regardless of what the instance holds
     */
    private void openSection(String section, String expectedText) {
        browser.clickOnElementById("database-nav-" + section);
        browser.assertElementExistsByIdAndContainsText("database-" + section + "-page", expectedText);
    }
}

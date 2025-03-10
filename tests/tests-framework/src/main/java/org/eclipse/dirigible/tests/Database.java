package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;

public class Database {
    private final Browser browser;

    private final String testInsertStatement = "INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')";
    private final String testCreateTableStatement = "CREATE TABLE STUDENT (" +
            "    id SERIAL PRIMARY KEY," +
            "    name TEXT NOT NULL," +
            "    address TEXT NOT NULL" +
            ");";

    protected Database(Browser browser) {
        this.browser = browser;
    }

    public void openExplorerView() {

    }

    public void openResultView() {

    }

    public void openStatementsView() {

    }

    public void expandSchema(String schemaName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.LI, schemaName);
    }

    public void createTestTable() {
        browser.enterTextInElementByAttributePattern(HtmlElementType.TEXT_AREA, HtmlAttribute.CLASS, "inputarea monaco-mouse-cursor-text", testCreateTableStatement);

    }

    public void createTestRecord() {
        browser.enterTextInElementByAttributePattern(HtmlElementType.TEXT_AREA, HtmlAttribute.CLASS, "inputarea monaco-mouse-cursor-text", testInsertStatement);
    }
}

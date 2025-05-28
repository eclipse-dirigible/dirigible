package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.DatabasePerspective;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CreateDatabaseViewIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT_NAME = "CreateDatabaseViewIT";
    private static final String TABLE_FILE_NAME = "test1.table";
    private static final String DB_VIEW_FILE_NAME = "test1.view";
    private static final String BUTTON_TEXT = "Add";
    private static final String DIALOG_LABEL_NAME = "Add column";

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        createTable(workbench);

        workbench.createCustomElementInProject(PROJECT_NAME, DB_VIEW_FILE_NAME, "Database View");
        workbench.openFile(DB_VIEW_FILE_NAME);
        assertFileTabIsOpen(DB_VIEW_FILE_NAME);

        removeDependencies();

        workbench.saveAll();
        workbench.publishAll(true);

        String schema = getSchema();

        DatabasePerspective databasePerspective = ide.openDatabasePerspective();
        expandSubviews(schema, databasePerspective);

        assertColumnExistsInDatabasePerspective();
    }


    private void removeDependencies() {
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.GLYPH, "sap-icon--delete", "");
        browser.clickOnElementByAttributeValue(HtmlElementType.BUTTON, HtmlAttribute.LABEL, "Delete");
        browser.clickOnElementByAttributeValue(HtmlElementType.BUTTON, HtmlAttribute.LABEL, "Save");
    }

    private void createTable(Workbench workbench) {
        workbench.createCustomElementInProject(PROJECT_NAME, TABLE_FILE_NAME, "Database Table");
        workbench.openFile(TABLE_FILE_NAME);
        assertFileTabIsOpen(TABLE_FILE_NAME);

        workbench.openDialogFromButton(BUTTON_TEXT);
        assertDialogExists(DIALOG_LABEL_NAME);

        workbench.addContentToDbTableField();
        assertColumnExistsInWorkbench();
    }

    private String getSchema() {
        boolean postgreSQL = dataSourcesManager.getDefaultDataSource()
                                               .isOfType(DatabaseSystem.POSTGRESQL);
        return postgreSQL ? "public" : "PUBLIC";
    }

    private void expandSubviews(String schema, DatabasePerspective databasePerspective) {
        databasePerspective.refreshTables();

        databasePerspective.expandSubmenu(schema);

        databasePerspective.expandSubmenu("Views");

        databasePerspective.expandSubmenu("MYVIEW");

        databasePerspective.expandSubmenu("Columns");
    }

    private void assertColumnExistsInDatabasePerspective() {
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.ANCHOR, "ID - ");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.ANCHOR, "Name - ");
    }

    private void assertColumnExistsInWorkbench() {
        browser.assertElementExistsByTypeAndText(HtmlElementType.TD, "Name");
    }

    private void assertDialogExists(String dialogLabelName) {
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER2, dialogLabelName);
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }
}

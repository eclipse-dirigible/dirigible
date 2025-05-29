package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;

public class CreateNewFolderIT extends UserInterfaceIntegrationTest {

    private static final String FOLDER_NAME = "folder1";
    private static final String FILE_NAME = "text1.txt";
    private static final String PROJECT_NAME = "CreateNewFolderIT";
    private static final String FILE_CONTENT = "my sample text";
    private static final String PREVIEW_ID = "preview";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME, FOLDER_NAME, "Folder");
        assertFolderIsPresent(FOLDER_NAME);

        workbench.createCustomElementInFolder(FILE_NAME, "File", "j1_4_anchor");
        workbench.openFile(FILE_NAME);
        assertFileTabIsOpen(FILE_NAME);

        workbench.addContentToFile(FILE_CONTENT);
        workbench.saveAll();
        assertTextIsPresent();
    }

    private void assertFolderIsPresent(String folderName) {
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.ANCHOR, folderName);
    }

    private void assertTextIsPresent() {
        browser.clickOnElementById(PREVIEW_ID);
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PRE, FILE_CONTENT);
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }

}

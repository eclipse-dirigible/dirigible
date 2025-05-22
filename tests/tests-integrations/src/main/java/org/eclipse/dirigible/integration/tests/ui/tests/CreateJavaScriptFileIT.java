package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;

public class CreateJavaScriptFileIT extends UserInterfaceIntegrationTest {
    private static final String FILE_NAME = "test1.mjs";
    private static final String INITIAL_CONTENT = "Hello World";
    private static final String FILE_CONTENT = "Hello Dirigible!";
    private static final String PROJECT_NAME = "CreateJavaScriptFileIT";
    private static final String PREVIEW_ID = "preview";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME, FILE_NAME, "File");
        workbench.openFile(FILE_NAME);
        workbench.clickPublishAll();

        assertFileTabIsOpen(FILE_NAME);
        assertTextInPreview(INITIAL_CONTENT);

        workbench.addContentToFile(FILE_CONTENT);
        workbench.saveAll();
        workbench.clickPublishAll();
        assertTextInPreview(FILE_CONTENT);
    }

    private void assertTextInPreview(String fileContent) {
        browser.clickOnElementById(PREVIEW_ID);
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PRE, fileContent);
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }
}

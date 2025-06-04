package org.eclipse.dirigible.integration.tests.ui.tests;


import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
class CreateJavaScriptServiceIT extends UserInterfaceIntegrationTest {
    private static final String FILE_NAME = "test1.mjs";
    private static final String PROJECT_NAME = "CreateJavaScriptServiceIT";
    private static final String PREVIEW_ID = "preview";
    private static final String INITIAL_CONTENT = "Hello World";
    private static final String FILE_CONTENT = "Hello Dirigible!";
    private static final String FINAL_CONTENT = "Hello Dirigible!";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());
        workbench.createCustomElementInProject(PROJECT_NAME, FILE_NAME, "JavaScript Service");

        workbench.openFile(FILE_NAME);
        assertFileTabIsOpen(FILE_NAME);
        workbench.publishFile("j1_4_anchor");
        workbench.openFile(FILE_NAME);
        assertFileContentIsPresent(INITIAL_CONTENT);


        workbench.openFile(FILE_NAME);
        assertFileTabIsOpen(FILE_NAME);
        workbench.addContentToFile(FILE_CONTENT);
//        workbench.saveAll();
//        workbench.publishFile("j1_4_anchor");
//        assertFileContentIsPresent(FINAL_CONTENT);
    }

    private void assertFileContentIsPresent(String fileContent) {
        browser.clickOnElementById(PREVIEW_ID);
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PRE, fileContent);
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }
}

package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;

public class CreateTextFileIT extends UserInterfaceIntegrationTest {

    private static final String FILE_NAME = "test.txt";
    private static final String FILE_CONTENT = "my sample text";
    private static final String PROJECT_NAME = "CreateTextFileIT";
    private static final String PREVIEW_ID = "preview";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME,FILE_NAME,"File");
        workbench.openFile(FILE_NAME);

        assertFileTabIsOpen(FILE_NAME);

        workbench.addContentToFile(FILE_CONTENT);
        workbench.saveAll();

        assertTextIsPresent();
    }

    private void assertTextIsPresent() {
        browser.clickOnElementById(PREVIEW_ID);
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PRE,FILE_CONTENT);
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }

}





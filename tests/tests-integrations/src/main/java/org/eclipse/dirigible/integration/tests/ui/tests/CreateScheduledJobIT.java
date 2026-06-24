package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.junit.jupiter.api.Test;

public class CreateScheduledJobIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT_NAME = "CreateScheduledJobIT";
    private static final String JS_FILE_NAME = "test1.mjs";
    private static final String JOB_FILE_NAME = "test1.job";
    private static final String JS_CONTENT = "console.log(\"Job executed\");";
    private static final String HANDLER_PATH = "CreateScheduledJobIT/test1.mjs";
    private static final String CONSOLE_ID = "console";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME, JS_FILE_NAME, "JavaScript Service");
        workbench.openFile(JS_FILE_NAME);

        assertFileTabIsOpen(JS_FILE_NAME);

        workbench.addContentToFile(JS_CONTENT);

        workbench.createCustomElementInProject(PROJECT_NAME, JOB_FILE_NAME, "Scheduled Job");
        workbench.openFile(JOB_FILE_NAME);

        assertFileTabIsOpen(JOB_FILE_NAME);

        changeHandlerPath(workbench);

        workbench.saveAll();
        workbench.clickPublishAll();

        assertLogIsPresent();
    }

    private void changeHandlerPath(Workbench workbench) {
        browser.clickOnElementById("idHandler");
        workbench.selectAll();
        browser.type(HANDLER_PATH);
        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.LABEL, "Save");
    }

    private void assertLogIsPresent() {
        browser.clickOnElementById(CONSOLE_ID);
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.DIV, "Job executed");
    }


    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }
}

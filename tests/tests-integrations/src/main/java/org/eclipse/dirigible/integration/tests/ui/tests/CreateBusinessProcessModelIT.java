package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.BpmWorkspacePerspective;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;

public class CreateBusinessProcessModelIT extends UserInterfaceIntegrationTest {

    private static final String FILE_NAME = "test1.bpmn";
    private static final String PROJECT_NAME = "CreateBusinessProcessModelIT";
    private static final String FIELD_CONTENT = "test1";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME, FILE_NAME, "Business Process Model");

        workbench.openFile(FILE_NAME);

        assertFileTabIsOpen(FILE_NAME);

        workbench.addContentToBpmnField(FIELD_CONTENT, "Process identifier :");
        workbench.addContentToBpmnField(FIELD_CONTENT, "Name :");

        workbench.saveAll();
        workbench.publishAll(true);

        BpmWorkspacePerspective bpmWorkspacePerspective = ide.openBpmPerspective();

        assertProcessNotificationIsPresent();
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }

    private void assertProcessNotificationIsPresent() {
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PARAGRAPH, "A new process definition has been added.");
    }

}


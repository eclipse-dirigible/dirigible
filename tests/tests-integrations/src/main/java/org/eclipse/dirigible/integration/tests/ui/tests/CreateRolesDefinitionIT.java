package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.SecurityPerspective;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.junit.jupiter.api.Test;

public class CreateRolesDefinitionIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT_NAME = "CreateRolesDefinitionIT";
    private static final String FILE_NAME = "test1.roles";
    private static final String ROLE_NAME = "test";
    private static final String ROLE_DESCRIPTION = "Test role";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME, FILE_NAME, "Roles Definitions");
        workbench.openFile(FILE_NAME);

        assertFileTabIsOpen(FILE_NAME);

        workbench.openDialogFromButton("Add");

        browser.clickOnElementById("reriName");
        browser.type(ROLE_NAME);

        browser.clickOnElementById("reriRoles");
        browser.type(ROLE_DESCRIPTION);

        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.LABEL, "Add");
        workbench.saveAll();

        workbench.publishAll(true);

        SecurityPerspective securityPerspective = ide.openSecurityPerspective();
        securityPerspective.assertRoleIsPresent(ROLE_NAME, ROLE_DESCRIPTION);
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }
}

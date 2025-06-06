package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.OperationsPerspective;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;



public class CreateExtensionPointIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT_NAME = "CreateExtensionPointIT";
    private static final String EXTENSION_POINT_FILE_NAME = "test1.extensionpoint";
    private static final String JS_FILE_NAME = "test1.mjs";
    private static final String EXTENSION_FILE_NAME = "test1.extension";
    private static final String EXTENSION_POINT_NAME = "test1";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME, EXTENSION_POINT_FILE_NAME, "Extension Point");
        workbench.openFile(EXTENSION_POINT_FILE_NAME);
        assertFileTabIsOpen(EXTENSION_POINT_FILE_NAME);

        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.ID, "idName");
        workbench.selectAll();
        browser.type(EXTENSION_POINT_NAME);

        workbench.saveAll();
        workbench.publishAll(true);

        workbench.createCustomElementInProject(PROJECT_NAME, JS_FILE_NAME, "JavaScript Service");
        workbench.openFile(JS_FILE_NAME);
        assertFileTabIsOpen(JS_FILE_NAME);
        workbench.publishAll(true);

        workbench.createCustomElementInProject(PROJECT_NAME, EXTENSION_FILE_NAME, "Extension");
        browser.doubleClickOnElementById("j1_6_anchor");
        assertFileTabIsOpen(EXTENSION_FILE_NAME);

        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.GLYPH, "sap-icon--navigation-down-arrow");
        browser.clickOnElementWithText(HtmlElementType.SPAN, EXTENSION_POINT_NAME);

        workbench.addContentToField("idModule", "/CreateExtensionPointIT/test1.mjs");

        workbench.saveAll();
        workbench.publishAll(true);

        OperationsPerspective operationsPerspective = ide.openOperationsPerspective();
        operationsPerspective.assertExtensionPointIsPresent(EXTENSION_POINT_FILE_NAME, EXTENSION_FILE_NAME);
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }
}

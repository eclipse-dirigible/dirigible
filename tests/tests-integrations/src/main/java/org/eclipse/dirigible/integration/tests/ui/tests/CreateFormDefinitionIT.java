package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;

public class CreateFormDefinitionIT extends UserInterfaceIntegrationTest {

    private static final String FILE_NAME = "test1.form";
    private static final String PROJECT_NAME = "CreateFormDefinitionIT";
    private static final String PREVIEW_ID = "preview";
    private static final String FILE_CONTENT = "<p>Hello World!</p>";

    @Test
    void test() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());

        workbench.createCustomElementInProject(PROJECT_NAME, FILE_NAME, "Form Definition");
        workbench.openFile(FILE_NAME);

        assertFileTabIsOpen(FILE_NAME);

        workbench.generateFormDefinition();
        assertFormIsGenerated();

        workbench.clickPublishAll();
        workbench.openFormIndexHTML();
        workbench.addContentToFormIndexHtml(FILE_CONTENT);

        workbench.saveAll();
        workbench.publishFile("j1_6_anchor");
        workbench.openFile("index.html");

        assertFileContentIsPresent("Hello World!");
    }

    private void assertFileContentIsPresent(String fileContent) {
        browser.clickOnElementById(PREVIEW_ID);
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PARAGRAPH, fileContent);
    }

    private void assertFormIsGenerated() {
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.ANCHOR, "test1.gen");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.ANCHOR, HtmlAttribute.ID, "j1_2_anchor", "gen");
    }

    private void assertFileTabIsOpen(String fileName) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag", fileName);
    }

}


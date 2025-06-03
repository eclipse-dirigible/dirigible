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
    private static final String FILE_CONTENT = "<!DOCTYPE HTML>\n"
            + "<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\" ng-app=\"forms\" ng-controller=\"FormController\">\n" + "\n"
            + "\t<head>\n" + "\t\t<meta charset=\"utf-8\" />\n"
            + "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
            + "        <link rel=\"icon\" sizes=\"any\" href=\"data:;base64,iVBORw0KGgo=\">\n" + "\t\t<title config-title></title>\n"
            + "\t\t<script type=\"text/javascript\" src=\"view.js\"></script>\n"
            + "\t\t<script type=\"text/javascript\" src=\"/services/js/platform-core/services/loader.js?id=view-js\"></script>\n"
            + "\t\t<link type=\"text/css\" rel=\"stylesheet\" href=\"/services/js/platform-core/services/loader.js?id=view-css\" />\n"
            + "\t\t<script type=\"text/javascript\" src=\"controller.js\"></script>\n" + "\t</head>\n" + "\n"
            + "\t<body class=\"bk-vbox\">\n" + "\t\t<bk-fieldset class=\"fd-margin--tiny\" ng-form=\"forms.form\">\n"
            + "\t\t\t<bk-form-group>\n" + "\t\t\t\t<p>Hello World!</p>\n" + "\t\t\t</bk-form-group>\n" + "\t\t</bk-fieldset>\n"
            + "\t\t<theme></theme>\n" + "\t</body>\n" + "\n" + "</html>";

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


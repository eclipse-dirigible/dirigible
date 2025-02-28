package org.eclipse.dirigible.integration.tests.ui;

import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

@Lazy
@Component
public class DependsOnTestProject {

    private static final String EDM_FILE_NAME = "edm.edm";
    private static final String PROJECT_ROOT_FOLDER_DEPENDS_ON = "dirigible-depends-on-test-project";
    private static final String PROJECT_RESOURCES_PATH_DEPENDS_ON = "dirigible-depends-on-test-project";
    private static final String VERIFICATION_URI = "/services/web/dirigible-depends-on-test-project/gen/edm/ui/Orders/index.html";


    private final IDE ide;
    private final ProjectUtil projectUtil;
    private final EdmView edmView;
    private final Browser browser;

    DependsOnTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        this.ide = ide;
        this.projectUtil = projectUtil;
        this.edmView = edmView;
        this.browser = browser;
    }

    public void publishDependsOn() {
        setupAndPublish(PROJECT_RESOURCES_PATH_DEPENDS_ON, PROJECT_ROOT_FOLDER_DEPENDS_ON, EDM_FILE_NAME);
        verifyDependsOn();
    }

    public void setupAndPublish(String projectResourcesPath, String projectRootFolder, String edmFileName) {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(projectResourcesPath);

        Workbench workbench = ide.openWorkbench();
        workbench.expandProject(projectRootFolder);
        workbench.openFile(edmFileName);

        edmView.regenerate();

        workbench.publishAll(false);
    }


    public void verifyDependsOn() {
        browser.openPath(VERIFICATION_URI);
        browser.clickOnElementWithText(HtmlElementType.BUTTON, "Create");
        browser.enterTextInElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search Country ...", "Bulgaria");

        // click out of the input field to trigger the search
        browser.clickOnElementContainingText(HtmlElementType.HEADER1, "Create Order");

        browser.clickOnElementByAttributePattern(HtmlElementType.INPUT, HtmlAttribute.PLACEHOLDER, "Search City ...");

        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Sofia");
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.SPAN, "Varna");
        browser.assertElementDoesNotExistsByTypeAndContainsText(HtmlElementType.SPAN, "Milano");
    }
}

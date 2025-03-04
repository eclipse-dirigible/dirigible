package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.ui.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DependsOnTestProject extends BaseTestProject {

    private static final String EDM_FILE_NAME = "edm.edm";
    private static final String PROJECT_ROOT_FOLDER_DEPENDS_ON = "dirigible-depends-on-test-project";
    private static final String PROJECT_RESOURCES_PATH_DEPENDS_ON = "dirigible-depends-on-test-project";
    private static final String VERIFICATION_URI = "/services/web/dirigible-depends-on-test-project/gen/edm/ui/Orders/index.html";

    private final EdmView edmView;
    private final Browser browser;

    DependsOnTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        super(PROJECT_RESOURCES_PATH_DEPENDS_ON, ide, projectUtil);
        this.edmView = edmView;
        this.browser = browser;
    }

    public void publishDependsOn() {
        setupAndPublish(PROJECT_RESOURCES_PATH_DEPENDS_ON, PROJECT_ROOT_FOLDER_DEPENDS_ON, EDM_FILE_NAME);
        verify();
    }

    public void setupAndPublish(String projectResourcesPath, String projectRootFolder, String edmFileName) {
        copyToWorkspace();

        Workbench workbench = getIde().openWorkbench();
        workbench.expandProject(projectRootFolder);
        workbench.openFile(edmFileName);

        edmView.regenerate();

        workbench.publishAll(false);
    }

    @Override
    public void verify() {
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

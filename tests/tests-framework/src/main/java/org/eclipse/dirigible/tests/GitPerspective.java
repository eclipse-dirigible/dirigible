package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.SleepUtil;

public class GitPerspective {
    private final Browser browser;

    public GitPerspective(Browser browser) {
        this.browser = browser;
    }

    public void cloneRepository(String repositoryUrl) {
        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.TITLE, "Clone");

        browser.enterTextInElementById("curli", repositoryUrl); // Git repository URL input field

        browser.clickOnElementByAttributePattern(HtmlElementType.BUTTON, HtmlAttribute.LABEL, "Clone");

        SleepUtil.sleepMillis(2000);

        assertClonedRepository();
    }

    private void assertClonedRepository() {
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER4, "Repository cloned");
    }
}


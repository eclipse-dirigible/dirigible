package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class OperationsPerspective {
    private final Browser browser;

    protected OperationsPerspective(Browser browser) {
        this.browser = browser;
    }

    public void assertExtensionPointIsPresent(String extensionPointFileName, String extensionFileName) {
        browser.doubleClickOnElementContainingText(HtmlElementType.ANCHOR, "test1");
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, extensionPointFileName);
        browser.assertElementExistsByTypeAndText(HtmlElementType.ANCHOR, extensionFileName);
    }
}

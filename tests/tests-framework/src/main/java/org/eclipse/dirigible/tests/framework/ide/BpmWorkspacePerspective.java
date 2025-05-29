package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class BpmWorkspacePerspective {
    private final Browser browser;

    protected BpmWorkspacePerspective(Browser browser) {
        this.browser = browser;
    }
}

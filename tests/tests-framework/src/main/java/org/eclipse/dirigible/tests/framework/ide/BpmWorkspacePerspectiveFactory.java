package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Lazy
@Component
public class BpmWorkspacePerspectiveFactory {
    private final Browser browser;

    protected BpmWorkspacePerspectiveFactory(Browser browser) {
        this.browser = browser;
    }

    public BpmWorkspacePerspective create() {
        return create(browser);
    }

    public BpmWorkspacePerspective create(Browser browser) {
        return new BpmWorkspacePerspective(browser);
    }
}

package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Lazy
@Component
public class SecurityPerspectiveFactory {
    private final Browser browser;

    protected SecurityPerspectiveFactory(Browser browser) {
        this.browser = browser;
    }

    public SecurityPerspective create() {
        return create(browser);
    }

    public SecurityPerspective create(Browser browser) {
        return new SecurityPerspective(browser);
    }
}

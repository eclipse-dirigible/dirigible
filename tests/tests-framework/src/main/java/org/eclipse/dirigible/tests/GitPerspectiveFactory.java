package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class GitPerspectiveFactory {
    private final Browser browser;

    protected GitPerspectiveFactory(Browser browser) {
        this.browser = browser;
    }

    public GitPerspective create() {
        return create(browser);
    }

    public GitPerspective create(Browser browser) {
        return new GitPerspective(browser);
    }
}

package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Lazy
@Component
public class OperationsPerspectiveFactory {
    private final Browser browser;

    protected OperationsPerspectiveFactory(Browser browser) {
        this.browser = browser;
    }

    public OperationsPerspective create() {
        return create(browser);
    }

    public OperationsPerspective create(Browser browser) {
        return new OperationsPerspective(browser);
    }
}

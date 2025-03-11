package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DatabaseFactory {
    private final Browser browser;


    protected DatabaseFactory(Browser browser) {
        this.browser = browser;
    }

    public Database create() {
        return create(browser);
    }

    public Database create(Browser browser) {
        return new Database(browser);
    }
}

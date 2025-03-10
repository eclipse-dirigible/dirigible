package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;

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

package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;

public class DatabaseFactory {
    private final Browser browser;
    private final WelcomeViewFactory welcomeViewFactory;
    private final TerminalFactory terminalFactory;

    protected DatabaseFactory(Browser browser, WelcomeViewFactory welcomeViewFactory, TerminalFactory terminalFactory) {
        this.browser = browser;
        this.welcomeViewFactory = welcomeViewFactory;
        this.terminalFactory = terminalFactory;
    }

    public Database create() {
        return create(browser);
    }

    public Database create(Browser browser) {
        return new Database(browser, welcomeViewFactory, terminalFactory);
    }
}

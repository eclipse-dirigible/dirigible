package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class WelcomeViewFactory {

    private final Browser defaultBrowser;

    WelcomeViewFactory(Browser defaultBrowser) {
        this.defaultBrowser = defaultBrowser;
    }

    public WelcomeView create() {
        return create(defaultBrowser);
    }

    public WelcomeView create(Browser browser) {
        return new WelcomeView(browser);
    }
}

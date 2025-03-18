package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.GitPerspective;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GitPerspectiveIT extends UserInterfaceIntegrationTest {
    private GitPerspective gitPerspective;

    @BeforeEach
    void setUp() {
        browser.clearCookies();
    }

    @Test
    void testGitFunctionality() {
        this.gitPerspective = ide.openGitPerspective();

        gitPerspective.cloneRepository("https://github.com/codbex/codbex-sample-model-depends-on");
    }
}

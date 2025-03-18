package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.GitPerspective;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.Workbench;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GitPerspectiveIT extends UserInterfaceIntegrationTest {
    private GitPerspective gitPerspective;
    private Workbench workbench;

    @BeforeEach
    void setUp() {
        browser.clearCookies();
    }

    @Test
    void testGitFunctionality() {
        this.gitPerspective = ide.openGitPerspective();

        gitPerspective.cloneRepository("https://github.com/codbex/codbex-sample-model-depends-on");

        this.workbench = ide.openWorkbench();
        workbench.publishAll(true);
    }
}

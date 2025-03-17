package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.GitPerspective;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;

public class GitPerspectiveIT extends UserInterfaceIntegrationTest {
    private GitPerspective gitPerspective;

    @Test
    void testGitFunctionality() {
        this.gitPerspective = ide.openGitPerspective();
    }
}

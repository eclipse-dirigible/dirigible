package org.eclipse.dirigible.integration.tests.ui.tests;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.GitPerspective;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class GitPerspectiveIT extends UserInterfaceIntegrationTest {
    private LogsAsserter consoleLogAsserter;
    private GitPerspective gitPerspective;
    private Workbench workbench;

    @BeforeEach
    void setUp() {
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
        browser.clearCookies();
    }

    @Test
    void testGitFunctionality() {
        this.gitPerspective = ide.openGitPerspective();

        gitPerspective.cloneRepository("https://github.com/codbex/codbex-sample-test-git");

        this.workbench = ide.openWorkbench();
        workbench.publishAll(true);

        assertWorkingProject();
    }

    void assertWorkingProject() {
        await().atMost(5, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("GIT-PERSPECTIVE-VALIDATION-OK", Level.INFO));
    }
}

package org.eclipse.dirigible.integration.tests.ui.tests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DependsOnIT extends UserInterfaceIntegrationTest {
    @Autowired
    private DependsOnTestProject testProject;

    @Test
    void dependsOnTest() {
        testProject.publishDependsOn();
    }
}

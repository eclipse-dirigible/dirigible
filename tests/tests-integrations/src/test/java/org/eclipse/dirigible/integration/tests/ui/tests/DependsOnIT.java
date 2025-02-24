package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.eclipse.dirigible.tests.*;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DependsOnIT extends UserInterfaceIntegrationTest {
    @Autowired
    private TestProject testProject;

    @Test
    void dependsOnTest() {
        testProject.publishDependsOn();
    }
}

package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreateEmptyEdmFileIT extends UserInterfaceIntegrationTest {
    @Autowired
    private TestProject testProject;

     @Test
     void dependsOnTest() {
         testProject.publishEmptyEdm();
     }
}

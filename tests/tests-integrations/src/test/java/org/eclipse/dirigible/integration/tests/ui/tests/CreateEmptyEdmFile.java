package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.ui.CreateEmptyFileTestProject;
import org.eclipse.dirigible.tests.framework.FileTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreateEmptyEdmFileIT extends UserInterfaceIntegrationTest {
    @Autowired
    private CreateEmptyFileTestProject testProject;

    @Test
    void dependsOnTest() {
        testProject.publishEmptyFile("edm.edm", FileTypes.ENTITY_DATA_MODEL);
    }
}

package org.eclipse.dirigible.integration.tests.ui.tests.camel;

import org.eclipse.dirigible.tests.PredefinedProjectIT;
import org.eclipse.dirigible.tests.projects.TestProject;
import org.springframework.beans.factory.annotation.Autowired;

public class CamelExtractTransformLoadTypescriptIT extends PredefinedProjectIT {

    @Autowired
    private CamelExtractTransformLoadTypescriptTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}

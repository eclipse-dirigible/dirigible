package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.PredefinedProjectIT;
import org.eclipse.dirigible.tests.projects.TestProject;
import org.springframework.beans.factory.annotation.Autowired;

public class DatabaseAvailabilityIT extends PredefinedProjectIT {

    @Autowired
    private DatabaseAvailabilityTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }
}

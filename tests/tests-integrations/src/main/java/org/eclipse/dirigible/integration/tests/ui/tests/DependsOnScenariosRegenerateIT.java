package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.PredefinedProjectIT;
import org.eclipse.dirigible.tests.framework.projects.TestProject;
import org.springframework.beans.factory.annotation.Autowired;

public class DependsOnScenariosRegenerateIT extends PredefinedProjectIT {

    @Autowired
    private DependsOnScenariosRegenerateTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }

}

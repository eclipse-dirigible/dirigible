package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.base.PredefinedProjectIT;
import org.eclipse.dirigible.tests.framework.projects.TestProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class DependsOnScenariosIT extends PredefinedProjectIT {

    @Autowired
    @Qualifier("dependsOnScenariosTestProject")
    private DependsOnScenariosTestProject testProject;

    @Override
    protected TestProject getTestProject() {
        return testProject;
    }

}

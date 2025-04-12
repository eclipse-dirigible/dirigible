package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DependsOnScenariosRegenerateTestProject extends DependsOnScenariosTestProject {
    DependsOnScenariosRegenerateTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        super(ide, projectUtil, edmView, browser);
    }

    @Override
    public void configure() {
        copyToWorkspace();
        generateEDM("sales-order.edm");
        publish();
    }
}

package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.util.ProjectUtil;
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

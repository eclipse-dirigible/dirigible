package org.eclipse.dirigible.integration.tests.ui;

import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class CamelTestProject {
    private static final String PROJECT_CAMEL_RESOURCES_PATH = "orders-etl";

    private final IDE ide;
    private final ProjectUtil projectUtil;

    public CamelTestProject(IDE ide, ProjectUtil projectUtil) {
        this.ide = ide;
        this.projectUtil = projectUtil;

    }

    public void publishCamel() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(PROJECT_CAMEL_RESOURCES_PATH);

        Workbench workbench = ide.openWorkbench();

        workbench.publishAll(false);
    }
}

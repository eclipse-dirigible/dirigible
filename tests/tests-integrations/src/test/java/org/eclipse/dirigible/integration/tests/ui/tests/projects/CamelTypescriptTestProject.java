package org.eclipse.dirigible.integration.tests.ui.tests.projects;

import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class CamelTypescriptTestProject {
    private final IDE ide;
    private final ProjectUtil projectUtil;

    public CamelTypescriptTestProject(IDE ide, ProjectUtil projectUtil) {
        this.ide = ide;
        this.projectUtil = projectUtil;
    }

    public void publish() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace("ApacheCamelITTypescript");

        Workbench workbench = ide.openWorkbench();

        workbench.publishAll(false);
    }

}

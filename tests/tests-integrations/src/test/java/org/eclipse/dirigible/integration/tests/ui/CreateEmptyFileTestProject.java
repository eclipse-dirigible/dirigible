package org.eclipse.dirigible.integration.tests.ui;

import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.framework.FileTypes;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class CreateEmptyFileTestProject {
    private final IDE ide;
    private final ProjectUtil projectUtil;

    private static final String UI_PROJECT_TITLE = "dirigible-test-project";

    public CreateEmptyFileTestProject(IDE ide, ProjectUtil projectUtil) {
        this.ide = ide;
        this.projectUtil = projectUtil;
    }

    public void publishEmptyFile(String fileName, FileTypes fileType) {
        publishEmptyFile(fileName, fileType.getType());
    }

    public void publishEmptyFile(String fileName, String fileType) {
        ide.createNewBlankProject(UI_PROJECT_TITLE);
        Workbench workbench = ide.openWorkbench();
        workbench.createFileInProject(UI_PROJECT_TITLE, fileName, fileType);
    }

    public void verifyFileCreated(String fileName) {
        Workbench workbench = ide.openWorkbench();
        workbench.openFile(fileName);
    }

}

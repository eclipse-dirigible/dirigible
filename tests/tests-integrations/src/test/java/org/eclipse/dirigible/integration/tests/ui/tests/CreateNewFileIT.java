package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CreateNewFileIT extends UserInterfaceIntegrationTest {

    @BeforeEach
    void setUp() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(this.getClass()
                                       .getSimpleName());
    }

    @ParameterizedTest
    @EnumSource(NewFileOption.class)
    void test(NewFileOption newFileOption) {
        Workbench workbench = ide.openWorkbench();
        workbench.createFileInProject(this.getClass()
                                          .getSimpleName(),
                newFileOption.getOptionName());

        workbench.openFile(newFileOption.getNewFileName());

        assertFileTabIsOpen(newFileOption);
    }

    private void assertFileTabIsOpen(NewFileOption newFileOption) {
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, "fd-icon-tab-bar__tag",
                newFileOption.getNewFileName());
    }

}

/*
 private static final String PROJECT_NAME = "DropdownTestProject";
 private static final String EDM_FILE_NAME = "test.edm";
 private static final String COUNTRY_ENTITY = "Country";
 private static final String CITY_ENTITY = "City";
 private static final String COUNTRY_PROPERTY = "Name";
 private static final String CITY_PROPERTY = "Name";
 */


package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.*;
import org.junit.jupiter.api.Test;

class dependsOnIT extends UserInterfaceIntegrationTest {
    private static final String PROJECT_NAME = "DropdownTestProject";
    private static final String EDM_FILE_NAME = "test.edm";
    private static final String COUNTRY_ENTITY = "Country";
    private static final String CITY_ENTITY = "City";
    private static final String COUNTRY_PROPERTY = "Name";
    private static final String CITY_PROPERTY = "Name";

     @Test
     void dependsOnTest() {
         ide.createNewBlankProject(PROJECT_NAME);
         Workbench workbench = ide.openWorkbench();
         ide.create(EDM_FILE_NAME);
         workbench.openFile(EDM_FILE_NAME);

//         FormView edmView = workbench.getFormView();
//         edmView.addEntity(COUNTRY_ENTITY);
     }
}

/*
 private static final String PROJECT_NAME = "DropdownTestProject";
 private static final String EDM_FILE_NAME = "test.edm";
 private static final String COUNTRY_ENTITY = "Country";
 private static final String CITY_ENTITY = "City";
 private static final String COUNTRY_PROPERTY = "Name";
 private static final String CITY_PROPERTY = "Name";
 */


package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.eclipse.dirigible.tests.*;
import org.eclipse.dirigible.tests.framework.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DependsOnIT extends UserInterfaceIntegrationTest {
    private static final String PROJECT_NAME = "DropdownTestProject";
    private static final String EDM_FILE_NAME = "test.edm";
    private static final String COUNTRY_ENTITY = "Country";
    private static final String CITY_ENTITY = "City";
    private static final String COUNTRY_PROPERTY = "Name";
    private static final String CITY_PROPERTY = "Name";

//    private static final edmView;

    @Autowired
    private TestProject testProject;

     @Test
     void dependsOnTest() {
         this.testProject.publishEmptyEdm();
     }
}

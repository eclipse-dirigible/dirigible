package org.eclipse.dirigible.integration.tests.ui.tests;

import org.assertj.db.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.projects.TestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Lazy
@Component
public class DatabaseAvailabilityTestProject extends BaseTestProject implements TestProject {

    @Autowired
    private DataSourcesManager dataSourcesManager;

    protected DatabaseAvailabilityTestProject(String projectResourcesFolder, IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super(projectResourcesFolder, ide, projectUtil, edmView);
    }

    @Override
    public void configure() {
        getIde().login();
        //        getIde().openPath("databases");
        // verify     Check availability of the views:
        //        Explorer
        //        Statements
        //        Result
        //    Find PUBLIC element within the Explorer view
        //    Double-click on PUBLIC
        //    Check availability of the sub-items
        //        Tables
        //        Views
        //        Procedures
        //        Functions
        //        Sequences
        //    Double-click on Tables
        //    Find & right-click on STUDENT
        //    Find & click on Show Content
        //    Check the content in the Result view as Empty result
        //    Enter in the Statements view
        //
        //INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')
        //
        //    Select all and click F8
        //    Repeat the steps of Show Content by checking one record in the Result view
    }

    @Override
    public void verify() throws Exception {
        //        assertDatabaseInsertion();
    }

    private void assertDatabaseInsertion() {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        AssertDbConnection connection = AssertDbConnectionFactory.of(dataSource)
                                                                 .create();

        Table studentsTable = connection.table("\"STUDENT\"")
                                        .build();

        Assertions.assertThat(studentsTable)
                  .hasNumberOfRows(1)
                  .row(0)
                  .value("ID")
                  .isEqualTo(1)
                  .value("NAME")
                  .isEqualTo("John Smith")
                  .value("CITY")
                  .isEqualTo("Sofia, Bulgaria");
    }
}

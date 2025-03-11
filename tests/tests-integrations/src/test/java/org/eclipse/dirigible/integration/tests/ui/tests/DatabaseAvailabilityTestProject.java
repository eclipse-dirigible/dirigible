package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.Database;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IDEFactory;
import org.eclipse.dirigible.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.projects.TestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DatabaseAvailabilityTestProject extends BaseTestProject implements TestProject {
    private final IDEFactory ideFactory;

    protected DatabaseAvailabilityTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, IDEFactory ideFactory) {
        super("DatabaseIT", ide, projectUtil, edmView);
        this.ideFactory = ideFactory;
    }

    @Override
    public void configure() {
        IDE ide = ideFactory.create();
        Database database = ide.openDatabase();

        database.expandSubviews();

        database.assertAvailabilityOfSubitems();
        // getIde().openPath("databases");
        // verify Check availability of the views:
        // Explorer
        // Statements
        // Result
        // Find PUBLIC element within the Explorer view
        // Double-click on PUBLIC
        // Check availability of the sub-items
        // Tables
        // Views
        // Procedures
        // Functions
        // Sequences
        // Double-click on Tables
        // Find & right-click on STUDENT
        // Find & click on Show Content
        // Check the content in the Result view as Empty result
        // Enter in the Statements view
        //
        // INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')
        //
        // Select all and click F8
        // Repeat the steps of Show Content by checking one record in the Result view
    }

    @Override
    public void verify() throws Exception {

    }
}

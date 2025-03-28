package org.eclipse.dirigible.integration.tests;

import org.eclipse.dirigible.integration.tests.ui.tests.GitPerspectiveIT;
import org.eclipse.dirigible.integration.tests.ui.tests.QuartzTransactionsIT;
import org.eclipse.dirigible.integration.tests.ui.tests.RestTransactionsIT;
import org.eclipse.dirigible.integration.tests.ui.tests.camel.CamelTransactionsIT;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({//
        RestTransactionsIT.class, //
        QuartzTransactionsIT.class, //
        GitPerspectiveIT.class, //
        CamelTransactionsIT.class, //

})
public class TransactionsTestSuite {
    // static {
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER", "org.postgresql.Driver");
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_URL",
    // "jdbc:postgresql://localhost:5432/postgres");
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_USERNAME", "postgres");
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_PASSWORD", "postgres");
    //
    // }
    // use this suite class to run all transaction related tests in the IDE
}

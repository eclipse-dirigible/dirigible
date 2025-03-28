package org.eclipse.dirigible.integration.tests;

import org.eclipse.dirigible.integration.tests.ui.tests.QuartzTransactionsIT;
import org.eclipse.dirigible.integration.tests.ui.tests.camel.CamelTransactionsIT;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({//
        // GitPerspectiveIT.class, //
        QuartzTransactionsIT.class, //
        CamelTransactionsIT.class, //
// RestTransactionsIT.class, //
})
public class TransactionsTestSuite {
    // use this suite class to run all transaction related tests in the IDE
}

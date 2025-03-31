package org.eclipse.dirigible.integration.tests;

import org.eclipse.dirigible.integration.tests.ui.tests.GitPerspectiveIT;
import org.eclipse.dirigible.integration.tests.ui.tests.QuartzTransactionsCommitIT;
import org.eclipse.dirigible.integration.tests.ui.tests.QuartzTransactionsRollbackIT;
import org.eclipse.dirigible.integration.tests.ui.tests.RestTransactionsIT;
import org.eclipse.dirigible.integration.tests.ui.tests.camel.CamelTransactionsCommitIT;
import org.eclipse.dirigible.integration.tests.ui.tests.camel.CamelTransactionsRollbackIT;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({//
        QuartzTransactionsRollbackIT.class, //
        QuartzTransactionsCommitIT.class, //
        CamelTransactionsRollbackIT.class, //
        CamelTransactionsCommitIT.class, //
        GitPerspectiveIT.class, //
        RestTransactionsIT.class, //
})
public class TransactionsTestSuite {
    // use this suite class to run all transaction related tests in the IDE
}

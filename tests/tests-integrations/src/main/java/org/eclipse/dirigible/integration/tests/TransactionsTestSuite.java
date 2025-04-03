/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
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

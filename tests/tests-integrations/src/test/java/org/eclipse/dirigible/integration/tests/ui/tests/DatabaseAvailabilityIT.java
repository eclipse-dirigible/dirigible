/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.tests.DatabaseView;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.junit.jupiter.api.Test;

class DatabaseAvailabilityIT extends UserInterfaceIntegrationTest {
    @Test
    void testDatabaseFunctionality() {
        DatabaseView database = ide.openDatabase();
        SleepUtil.sleepSeconds(2);

        database.expandSubviews();
        database.assertAvailabilityOfSubitems();

        database.createTestTable();
        database.assertEmptyTable();

        database.createTestRecord();
        database.assertResult();

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

}

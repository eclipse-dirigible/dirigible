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

import org.eclipse.dirigible.tests.DatabasePerspective;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;

class DatabasePerspectiveIT extends UserInterfaceIntegrationTest {
    @Test
    void testDatabaseFunctionality() {
        DatabasePerspective databasePerspective = ide.openDatabasePerspective();

        databasePerspective.createTestTable(); // Creating test table first to show in the database view

        databasePerspective.expandSubviews();
        databasePerspective.assertAvailabilityOfSubitems();

        databasePerspective.assertEmptyTable("STUDENT");
        databasePerspective.createTestRecord();
        databasePerspective.assertResult();
    }

}

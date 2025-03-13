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
        SleepUtil.sleepMillis(2000);

        database.expandSubviews();
        database.assertAvailabilityOfSubitems();
        SleepUtil.sleepMillis(2000);

        database.createTestTable();
        database.assertEmptyTable();
        SleepUtil.sleepMillis(2000);

        database.createTestRecord();
        database.assertResult();
    }

}

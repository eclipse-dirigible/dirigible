/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class CreateNewProjectIT extends UserInterfaceIntegrationTest {

    private static final String initialDirigibleHomeUrl;

    static {
        initialDirigibleHomeUrl = Configuration.get("DIRIGIBLE_HOME_URL");
        Configuration.set("DIRIGIBLE_HOME_URL", "services/web/ide/");
    }

    // TODO - method to be removed once the test is adapted to the new UI
    @AfterAll
    public static void tearDown() {
        Configuration.set("DIRIGIBLE_HOME_URL", initialDirigibleHomeUrl);
    }

    @Test
    void testCreateNewBlankProject() {
        ide.createNewBlankProject("create-project-ui-test");
    }
}

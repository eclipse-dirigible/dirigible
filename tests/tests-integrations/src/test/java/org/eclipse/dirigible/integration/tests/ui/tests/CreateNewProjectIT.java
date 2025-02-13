/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.Test;

class CreateNewProjectIT extends UserInterfaceIntegrationTest {

    // static {
    // Configuration.set("DIRIGIBLE_HOME_URL", "services/web/ide/");
    // }
    // if you want to use the old UI - uncomment the upper 3 lines

    @Test
    void testCreateNewBlankProject() {
        ide.createNewBlankProject("create-project-ui-test");
    }
}

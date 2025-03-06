/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.camel;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.projects.BaseCamelTestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class CamelExtractTransformLoadJdbcTestProject extends BaseCamelTestProject {
    public CamelExtractTransformLoadJdbcTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super("CamelExtractTransformLoadJdbcIT", ide, projectUtil, edmView);
    }

    @Override
    public void verify() {
        assertLogContainsMessage(camelLogAsserter, "Replicating orders from OpenCart using JDBC...", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "Successfully replicated orders from OpenCart using JDBC", Level.INFO);
        assertDatabaseETLCompletion();
    }
}

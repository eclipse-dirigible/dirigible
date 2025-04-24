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

import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.projects.BaseTestProject;
import org.eclipse.dirigible.tests.framework.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class DirigibleTestProject extends BaseTestProject {

    protected DirigibleTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super("test-project", ide, projectUtil, edmView);
    }

    @Override
    public void verify() throws Exception {
        // this is a test project which is copied from dirigible-components-test-project
        // the project was part of the Dirigible contribution
        // with the current PR the project is moved to an integration test
        // a verification should be added to verify that it works
        // in additional, separate integration tests could be introduced
    }
}

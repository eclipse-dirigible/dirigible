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

import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DependsOnScenariosRegenerateTestProject extends DependsOnScenariosTestProject {
    DependsOnScenariosRegenerateTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, Browser browser) {
        super(ide, projectUtil, edmView, browser);
    }

    @Override
    public void configure() {
        copyToWorkspace();
        generateEDM("sales-order.edm");
        publish();
    }
}

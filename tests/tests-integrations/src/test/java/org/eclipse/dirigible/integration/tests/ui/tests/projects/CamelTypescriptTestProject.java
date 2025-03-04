/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.projects;

import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class CamelTypescriptTestProject extends BaseTestProject{
    public CamelTypescriptTestProject(IDE ide, ProjectUtil projectUtil) {
        super("CamelExtractTransformLoadIT_testTypeScriptScenario", ide, projectUtil);
    }

    @Override
    public void verify() {

    }

}

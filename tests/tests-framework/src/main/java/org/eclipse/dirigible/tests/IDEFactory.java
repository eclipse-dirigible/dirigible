/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.BrowserFactory;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class IDEFactory {

    private final BrowserFactory browserFactory;
    private final RestAssuredExecutor restAssuredExecutor;
    private final ProjectUtil projectUtil;

    IDEFactory(BrowserFactory browserFactory, RestAssuredExecutor restAssuredExecutor, ProjectUtil projectUtil) {
        this.browserFactory = browserFactory;
        this.restAssuredExecutor = restAssuredExecutor;
        this.projectUtil = projectUtil;
    }

    public IDE create(String username, String password) {
        return create(browserFactory.create(), username, password);
    }

    public IDE create(Browser browser, String username, String password) {
        return new IDE(browser, restAssuredExecutor, username, password, projectUtil);
    }

}

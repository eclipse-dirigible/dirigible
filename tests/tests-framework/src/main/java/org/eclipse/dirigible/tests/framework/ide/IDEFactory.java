/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.BrowserFactory;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class IDEFactory {

    private final BrowserFactory browserFactory;
    private final RestAssuredExecutor restAssuredExecutor;
    private final WorkbenchFactory workbenchFactory;
    private final DatabasePerspectiveFactory databasePerspectiveFactory;
    private final GitPerspectiveFactory gitPerspectiveFactory;

    protected IDEFactory(BrowserFactory browserFactory, RestAssuredExecutor restAssuredExecutor, WorkbenchFactory workbenchFactory,
            DatabasePerspectiveFactory databasePerspectiveFactory, GitPerspectiveFactory gitPerspectiveFactory) {
        this.browserFactory = browserFactory;
        this.restAssuredExecutor = restAssuredExecutor;
        this.workbenchFactory = workbenchFactory;
        this.databasePerspectiveFactory = databasePerspectiveFactory;
        this.gitPerspectiveFactory = gitPerspectiveFactory;
    }

    public IDE create() {
        DirigibleTestTenant defaultTenant = DirigibleTestTenant.createDefaultTenant();
        return create(defaultTenant.getUsername(), defaultTenant.getPassword());
    }

    public IDE create(String username, String password) {
        return create(browserFactory.create(), username, password);
    }

    public IDE create(Browser browser, String username, String password) {
        return new IDE(browser, username, password, restAssuredExecutor, workbenchFactory, databasePerspectiveFactory,
                gitPerspectiveFactory);
    }

    public IDE create(String host, String username, String password) {
        return new IDE(browserFactory.createByHost(host), username, password, restAssuredExecutor, workbenchFactory,
                databasePerspectiveFactory, gitPerspectiveFactory);
    }
}

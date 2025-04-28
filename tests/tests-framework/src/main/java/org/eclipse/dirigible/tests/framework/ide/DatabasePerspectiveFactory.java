/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class DatabasePerspectiveFactory {
    private final Browser browser;
    private final DataSourcesManager dataSourcesManager;

    protected DatabasePerspectiveFactory(Browser browser, DataSourcesManager dataSourcesManager) {
        this.browser = browser;
        this.dataSourcesManager = dataSourcesManager;
    }

    public DatabasePerspective create() {
        return create(browser);
    }

    public DatabasePerspective create(Browser browser) {
        return new DatabasePerspective(browser);
    }
}

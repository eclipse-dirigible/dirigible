/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.util;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TestConditionsChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestConditionsChecker.class);

    private final DataSourcesManager dataSourcesManager;

    TestConditionsChecker(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    public boolean isH2OrPostgresDefaultDB() {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        LOGGER.debug("DefaultDB data source: {}, type [{}]", defaultDataSource, defaultDataSource.getDatabaseSystem());
        return isH2DefaultDB() || isPostgresDefaultDB();
    }

    public boolean isH2DefaultDB() {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        return defaultDataSource.isOfType(DatabaseSystem.H2);
    }

    public boolean isPostgresDefaultDB() {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        return defaultDataSource.isOfType(DatabaseSystem.POSTGRESQL);
    }

}

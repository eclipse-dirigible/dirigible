/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.config;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * The Class DataSourceConfig.
 */
@Configuration
public class TransactionManagerConfig {

    public static final String DEFAULT_DB_TRANSACTION_MANAGER = "defaultDbTransactionManager";

    @Lazy
    @Bean(name = DEFAULT_DB_TRANSACTION_MANAGER)
    PlatformTransactionManager defaultDbTransactionManagerDataSource(DataSourcesManager dataSourcesManager) {
        DataSource defaultDbDataSource = dataSourcesManager.getDefaultDataSource();
        return new DataSourceTransactionManager(defaultDbDataSource);
    }

}

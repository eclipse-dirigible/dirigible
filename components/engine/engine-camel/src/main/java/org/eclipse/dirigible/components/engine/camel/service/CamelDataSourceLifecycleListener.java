/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.service;

import org.apache.camel.spring.boot.SpringBootCamelContext;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourceInitializer;
import org.eclipse.dirigible.components.data.sources.service.DataSourceLifecycleListener;
import org.springframework.stereotype.Component;

/**
 * The listener interface for receiving camelDataSourceLifecycle events. The class that is
 * interested in processing a camelDataSourceLifecycle event implements this interface, and the
 * object created with that class is registered with a component using the component's
 * addCamelDataSourceLifecycleListener method. When the camelDataSourceLifecycle event occurs, that
 * object's appropriate method is invoked.
 *
 */
@Component
class CamelDataSourceLifecycleListener implements DataSourceLifecycleListener {

    /** The camel context. */
    private final SpringBootCamelContext camelContext;

    /** The data source initializer. */
    private final DataSourceInitializer dataSourceInitializer;

    /**
     * Instantiates a new camel data source lifecycle listener.
     *
     * @param camelContext the camel context
     * @param dataSourceInitializer the data source initializer
     */
    CamelDataSourceLifecycleListener(SpringBootCamelContext camelContext, DataSourceInitializer dataSourceInitializer) {
        this.camelContext = camelContext;
        this.dataSourceInitializer = dataSourceInitializer;
    }

    /**
     * On save.
     *
     * @param dataSource the data source
     */
    @Override
    public void onSave(DataSource dataSource) {
        String dataSourceName = dataSource.getName();
        javax.sql.DataSource sqlDataSource = dataSourceInitializer.initialize(dataSource);

        camelContext.getRegistry()
                    .bind(dataSourceName, sqlDataSource);

    }

    /**
     * On delete.
     *
     * @param dataSource the data source
     */
    @Override
    public void onDelete(DataSource dataSource) {
        String dataSourceName = dataSource.getName();
        camelContext.getRegistry()
                    .unbind(dataSourceName);
    }

}

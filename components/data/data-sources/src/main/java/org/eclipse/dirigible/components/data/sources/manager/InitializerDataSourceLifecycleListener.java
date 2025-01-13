/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.manager;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.service.DataSourceLifecycleListener;
import org.springframework.stereotype.Component;

/**
 * The listener interface for receiving initializerDataSourceLifecycle events. The class that is
 * interested in processing a initializerDataSourceLifecycle event implements this interface, and
 * the object created with that class is registered with a component using the component's
 * addInitializerDataSourceLifecycleListener method. When the initializerDataSourceLifecycle event
 * occurs, that object's appropriate method is invoked.
 *
 */
@Component
class InitializerDataSourceLifecycleListener implements DataSourceLifecycleListener {

    /** The data source initializer. */
    private final DataSourceInitializer dataSourceInitializer;

    /**
     * Instantiates a new initializer data source lifecycle listener.
     *
     * @param dataSourceInitializer the data source initializer
     */
    InitializerDataSourceLifecycleListener(DataSourceInitializer dataSourceInitializer) {
        this.dataSourceInitializer = dataSourceInitializer;
    }

    /**
     * On save.
     *
     * @param dataSource the data source
     */
    @Override
    public void onSave(DataSource dataSource) {
        // nothing to do here
    }

    /**
     * On delete.
     *
     * @param dataSource the data source
     */
    @Override
    public void onDelete(DataSource dataSource) {
        String dataSourceName = dataSource.getName();
        dataSourceInitializer.removeInitializedDataSource(dataSourceName);
    }


}

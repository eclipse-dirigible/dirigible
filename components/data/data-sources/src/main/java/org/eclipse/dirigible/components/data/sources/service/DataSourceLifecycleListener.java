/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.service;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;

/**
 * The listener interface for receiving dataSourceLifecycle events. The class that is interested in
 * processing a dataSourceLifecycle event implements this interface, and the object created with
 * that class is registered with a component using the component's addDataSourceLifecycleListener
 * method. When the dataSourceLifecycle event occurs, that object's appropriate method is invoked.
 *
 */
public interface DataSourceLifecycleListener {

    /**
     * On save.
     *
     * @param dataSource the data source
     */
    void onSave(DataSource dataSource);

    /**
     * On delete.
     *
     * @param dataSource the data source
     */
    void onDelete(DataSource dataSource);

}

/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;

/**
 * Builds the document-number service for the in-process test slice (dirigible #7643), whose
 * constructors are package-private because the platform builds it as a Spring bean.
 */
public final class IntentSliceNumbering {

    private IntentSliceNumbering() {}

    /**
     * Creates a document-number service allocating in the given datasources' default database.
     *
     * @param dataSourcesManager the source of the default database
     * @return the service
     */
    public static DocumentNumberService create(DataSourcesManager dataSourcesManager) {
        return new DocumentNumberService(new DocumentNumberStore(dataSourcesManager));
    }
}

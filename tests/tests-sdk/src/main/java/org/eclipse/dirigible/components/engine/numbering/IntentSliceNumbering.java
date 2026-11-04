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

import java.util.Map;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Builds the document-number service for the in-process test slice (dirigible #7643), whose
 * constructors are package-private because the platform builds it as a Spring bean.
 */
public final class IntentSliceNumbering {

    private IntentSliceNumbering() {}

    /**
     * Creates a document-number service allocating in the given datasources' default database, inside
     * the given write transaction when one is open - as the platform's does (#8113).
     *
     * @param dataSourcesManager the source of the default database
     * @param writeTransaction the write transaction an allocation joins - the slice's entity store
     * @return the service
     */
    public static DocumentNumberService create(DataSourcesManager dataSourcesManager, WriteTransaction writeTransaction) {
        return new DocumentNumberService(new DocumentNumberStore(dataSourcesManager,
                new StaticListableBeanFactory(Map.of("writeTransaction", writeTransaction)).getBeanProvider(WriteTransaction.class)));
    }
}

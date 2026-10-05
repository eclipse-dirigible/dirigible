/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.outbox;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;

/**
 * Builds the event outbox for the in-process test slice (dirigible #7643), whose constructors are
 * package-private because the platform builds it as a Spring bean.
 */
public final class IntentSliceOutbox {

    private IntentSliceOutbox() {}

    /**
     * Creates an outbox recording into the given datasources' default database.
     *
     * @param dataSourcesManager the source of the default database
     * @param tenantContext the tenant the outbox records for
     * @return the outbox
     */
    public static EventOutbox create(DataSourcesManager dataSourcesManager, TenantContext tenantContext) {
        return new EventOutbox(new EventOutboxStore(dataSourcesManager, tenantContext));
    }
}

/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.typescript.transpilation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.Test;

/**
 * A folder or a TypeScript source is removed from the registry with the tsc watch stopped: a
 * compile in flight would otherwise write the JavaScript of the removed sources back into the
 * removed location, where it stays published with no source.
 */
class TscWatcherServicePublisherHandlerTest {

    private final TscWatcherService tscWatcherService = mock(TscWatcherService.class);

    private final IRepository repository = mock(IRepository.class);

    private final TscWatcherServicePublisherHandler handler = new TscWatcherServicePublisherHandler(tscWatcherService, repository);

    @Test
    void theWatcherIsSuspendedWhileAFolderIsUnpublished() {
        collection("/registry/public/orders/gen/orders", true);

        handler.beforeUnpublish("/registry/public/orders/gen/orders");

        verify(tscWatcherService).suspend();
    }

    @Test
    void theWatcherIsSuspendedWhileATypeScriptSourceIsUnpublished() {
        handler.beforeUnpublish("/registry/public/orders/api/OrderService.ts");
        handler.beforeUnpublish("/registry/public/orders/api/OrderView.tsx");

        verify(tscWatcherService, times(2)).suspend();
    }

    @Test
    void anyOtherFileIsUnpublishedWithTheWatcherRunning() {
        collection("/registry/public/orders/index.html", false);
        collection("/registry/public/orders/api/types.d.ts", false);

        handler.beforeUnpublish("/registry/public/orders/index.html");
        handler.beforeUnpublish("/registry/public/orders/api/types.d.ts");

        verify(tscWatcherService, never()).suspend();
    }

    @Test
    void theWatcherIsResumedOnceTheLocationIsGone() {
        handler.afterUnpublish("/registry/public/orders/gen/orders");

        verify(tscWatcherService).resume();
    }

    private void collection(String location, boolean exists) {
        ICollection collection = mock(ICollection.class);
        when(collection.exists()).thenReturn(exists);
        when(repository.getCollection(location)).thenReturn(collection);
    }

}

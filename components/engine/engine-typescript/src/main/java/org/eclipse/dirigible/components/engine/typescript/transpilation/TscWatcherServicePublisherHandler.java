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

import org.eclipse.dirigible.components.base.publisher.PublisherHandler;
import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Same switch as TscWatcherService: without the watcher there is nothing to restart.
@Order(30)
@Component
@ConditionalOnExpression("'${DIRIGIBLE_TSC_WATCH_SERVICE_ENABLED:true}'.toLowerCase() != 'false'")
class TscWatcherServicePublisherHandler implements PublisherHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TscWatcherServicePublisherHandler.class);

    private final TscWatcherService tscWatcherService;

    private final IRepository repository;

    TscWatcherServicePublisherHandler(TscWatcherService tscWatcherService, IRepository repository) {
        this.tscWatcherService = tscWatcherService;
        this.repository = repository;
    }

    @Override
    public void afterPublish(String workspaceLocation, String registryLocation, AfterPublishMetadata metadata) {
        // Handle the cases where a project is published -> transpiled (by TscWatcherService) -> unpublished
        // -> published (example: form regeneration).
        // This way the transpiled files are lost and tsc watch service needs to be restarted to transpile
        // the files again

        if (metadata.isProjectMetadata()) { // reduce the restarts by ignoring events for files
            LOGGER.debug("Restarting tsc watcher service for metadata {}", metadata);
            tscWatcherService.restart();
        } else {
            LOGGER.debug("Ignoring tsc watcher service restart for metadata {}", metadata);
        }
    }

    @Override
    public void beforePublish(String location) {
        // not applicable
    }

    /**
     * Stops the watcher while a folder or a TypeScript source is removed: a compile in flight read the
     * sources before, and would write their JavaScript back into the removed location afterwards.
     */
    @Override
    public void beforeUnpublish(String location) {
        if (mayHoldCompiledOutput(location)) {
            tscWatcherService.suspend();
        }
    }

    @Override
    public void afterUnpublish(String location) {
        tscWatcherService.resume();
    }

    private boolean mayHoldCompiledOutput(String location) {
        String name = location.substring(location.lastIndexOf('/') + 1);
        return TscWatcherService.isTypeScriptSource(name) || repository.getCollection(location)
                                                                       .exists();
    }

}

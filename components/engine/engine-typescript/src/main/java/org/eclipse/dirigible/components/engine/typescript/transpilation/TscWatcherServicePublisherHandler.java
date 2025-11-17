/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.typescript.transpilation;

import org.eclipse.dirigible.components.base.publisher.PublisherHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class TscWatcherServicePublisherHandler implements PublisherHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TscWatcherServicePublisherHandler.class);

    private final TscWatcherService tscWatcherService;

    TscWatcherServicePublisherHandler(TscWatcherService tscWatcherService) {
        this.tscWatcherService = tscWatcherService;
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

    @Override
    public void beforeUnpublish(String location) {
        // not applicable
    }

    @Override
    public void afterUnpublish(String location) {
        // not applicable
    }

}

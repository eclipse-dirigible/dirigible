/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import org.eclipse.dirigible.components.base.publisher.PublisherHandler;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class SynchronizationWatcherPublisherHandler.
 */

@Order(50)
@Component
public class SynchronizationWatcherPublisherHandler implements PublisherHandler {

    /** The synchronization watcher. */
    @Autowired
    private SynchronizationWatcher synchronizationWatcher;

    @Autowired
    private DefinitionService definitionService;

    /**
     * Before publish.
     *
     * @param location the location
     */
    @Override
    public void beforePublish(String location) {

    }

    /**
     * After publish.
     *
     * @param workspaceLocation the workspace location
     * @param registryLocation the registry location
     * @param metadata the metadata
     */
    @Override
    public void afterPublish(String workspaceLocation, String registryLocation, AfterPublishMetadata metadata) {
        synchronizationWatcher.force();
    }

    /**
     * Before unpublish.
     *
     * @param location the location
     */
    @Override
    public void beforeUnpublish(String location) {

    }

    /**
     * After unpublish.
     *
     * @param location the location
     */
    @Override
    public void afterUnpublish(String location) {
        String path = location;
        if (location.startsWith(IRepositoryStructure.PATH_REGISTRY_PUBLIC)) {
            path = path.substring(IRepositoryStructure.PATH_REGISTRY_PUBLIC.length());
        }
        definitionService.initializeChecksums(path);
        synchronizationWatcher.force();
    }

}

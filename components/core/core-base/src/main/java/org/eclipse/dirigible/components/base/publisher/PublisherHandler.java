/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.publisher;

/**
 * The Interface PublisherHandler.
 */
public interface PublisherHandler {

    /**
     * The AfterPublishMetadata.
     */
    record AfterPublishMetadata(String workspace, String projectName, String entryPath, boolean isDirectory) {
        public boolean isProjectMetadata() {
            return isDirectory && "".equals(entryPath);
        }
    }

    /**
     * Before publish.
     *
     * @param location the location
     */
    default void beforePublish(String location) {}

    /**
     * After publish.
     *
     * @param workspaceLocation the workspace location
     * @param registryLocation the registry location
     * @param metadata the metadata
     */
    default void afterPublish(String workspaceLocation, String registryLocation, AfterPublishMetadata metadata) {}

    /**
     * Before unpublish.
     *
     * @param location the location
     */
    default void beforeUnpublish(String location) {}

    /**
     * After unpublish.
     *
     * @param location the location
     */
    default void afterUnpublish(String location) {}

}

/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.registry.watcher;

import java.nio.file.Path;

/**
 * The Interface LocalRegistryWatcherHandler.
 */
public interface LocalRegistryWatcherHandler {

    /**
     * Directory registered.
     *
     * @param path the path
     */
    public void directoryRegistered(Path path);

    /**
     * Directory created.
     *
     * @param path the path
     */
    public void directoryCreated(Path path);

    /**
     * File registered.
     *
     * @param path the path
     */
    public void fileRegistered(Path path);

    /**
     * File created.
     *
     * @param path the path
     */
    public void fileCreated(Path path);

    /**
     * File modified.
     *
     * @param path the path
     */
    public void fileModified(Path path);

    /**
     * File deleted.
     *
     * @param path the path
     */
    public void fileDeleted(Path path);

}

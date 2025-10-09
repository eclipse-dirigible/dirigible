/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.server;

import java.nio.file.Path;

public class DirigibleServerConfig {

    private final Path serverJarPath;
    private final Path projectPath;

    public DirigibleServerConfig(Path serverJarPath, Path projectPath) {
        this.serverJarPath = serverJarPath;
        this.projectPath = projectPath;
    }

    public Path getServerJarPath() {
        return serverJarPath;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    @Override
    public String toString() {
        return "DirigibleServerConfig{" + "serverJarPath=" + serverJarPath + ", projectPath=" + projectPath + '}';
    }
}

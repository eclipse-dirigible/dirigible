/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides the on-disk directory where compiled user class files are written after each rebuild.
 * The directory is created lazily on first access.
 *
 * <p>
 * Located at: {@code <repoRoot>/dirigible/java-compiled/bin/}
 *
 * <p>
 * All FQNs are globally unique (enforced by {@code JavaSynchronizer}), so a single flat output tree
 * is correct for the whole platform.
 */
@Component
public class JavaCompiledOutputDirectory {

    private static final Logger logger = LoggerFactory.getLogger(JavaCompiledOutputDirectory.class);

    private final Path directory;

    public JavaCompiledOutputDirectory() throws IOException {
        String repoRoot = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue();
        this.directory = Paths.get(repoRoot, "dirigible", "java-compiled", "bin")
                              .toAbsolutePath()
                              .normalize();
        Files.createDirectories(this.directory);
        logger.info("[java-lsp] Compiled output directory: {}", this.directory);
    }

    /** Returns the on-disk output directory for compiled user class files. */
    public Path get() {
        return directory;
    }
}

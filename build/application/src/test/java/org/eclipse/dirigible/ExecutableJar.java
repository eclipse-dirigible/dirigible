/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Locates the executable jar this module packaged, for the integration tests that verify the
 * shipped artifact itself.
 */
final class ExecutableJar {

    /**
     * Instances are not needed.
     */
    private ExecutableJar() {
        // test utility
    }

    /**
     * The executable jar this module just packaged - the newest one, so a stale jar of a previous
     * version surviving in a non-clean target directory is never picked.
     *
     * @return the jar path
     */
    static Path path() {
        try (Stream<Path> files = Files.list(Path.of("target"))) {
            return files.filter(file -> file.getFileName()
                                            .toString()
                                            .endsWith("-executable.jar"))
                        .max(Comparator.comparingLong(file -> file.toFile()
                                                                  .lastModified()))
                        .orElseThrow(() -> new IllegalStateException("the executable jar is not in target - run the package phase first"));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list the target directory", e);
        }
    }

}

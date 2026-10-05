/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.upgrade;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The release an upgrade test starts from. It is kept in ONE place, {@code tests/UPGRADE_FROM},
 * which the release workflow bumps to the version it has just released, so master is always tested
 * against the release before it. The system property {@value #OVERRIDE_PROPERTY} overrides it - to
 * try an upgrade from an older release, say.
 */
public final class PreviousRelease {

    /** The system property that overrides the file. */
    public static final String OVERRIDE_PROPERTY = "dirigible.upgrade.from";

    /** The file that names the previous release. */
    static final String FILE_NAME = "UPGRADE_FROM";

    private PreviousRelease() {}

    /**
     * The tag of the previous release's {@code dirigiblelabs/dirigible} image.
     *
     * @return the tag, e.g. {@code 14.74.0}
     */
    public static String tag() {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return override.strip();
        }
        return read(locate(Path.of("")
                               .toAbsolutePath()));
    }

    /**
     * Finds {@code tests/UPGRADE_FROM} from wherever the build runs: the tests module itself, or any
     * folder below the repository root.
     */
    static Path locate(Path start) {
        for (Path folder = start; folder != null; folder = folder.getParent()) {
            for (Path candidate : new Path[] {folder.resolve(FILE_NAME), folder.resolve("tests")
                                                                               .resolve(FILE_NAME)}) {
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException(
                "No tests/" + FILE_NAME + " above [" + start + "] - it names the release an upgrade test starts from");
    }

    private static String read(Path file) {
        try {
            String tag = Files.readString(file, StandardCharsets.UTF_8)
                              .strip();
            if (tag.isEmpty()) {
                throw new IllegalStateException("[" + file + "] is empty - it must name the release an upgrade test starts from");
            }
            return tag;
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read [" + file + "]", ex);
        }
    }
}

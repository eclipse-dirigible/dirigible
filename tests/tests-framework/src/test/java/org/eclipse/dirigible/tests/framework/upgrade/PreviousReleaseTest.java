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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The previous release is named in one place, found from wherever the build runs. */
class PreviousReleaseTest {

    @TempDir
    Path repository;

    @Test
    void theFileIsFoundFromAModuleBelowTheTestsFolder() throws IOException {
        Path file = Files.writeString(Files.createDirectories(repository.resolve("tests"))
                                           .resolve(PreviousRelease.FILE_NAME),
                "14.74.0\n");
        Path module = Files.createDirectories(repository.resolve("tests/tests-integrations"));

        assertThat(PreviousRelease.locate(module)).isEqualTo(file);
    }

    @Test
    void theFileIsFoundFromTheRepositoryRoot() throws IOException {
        Path file = Files.writeString(Files.createDirectories(repository.resolve("tests"))
                                           .resolve(PreviousRelease.FILE_NAME),
                "14.74.0\n");

        assertThat(PreviousRelease.locate(repository)).isEqualTo(file);
    }

    @Test
    void aMissingFileIsNamedInTheFailure() {
        assertThatThrownBy(() -> PreviousRelease.locate(repository)).isInstanceOf(IllegalStateException.class)
                                                                    .hasMessageContaining("tests/UPGRADE_FROM");
    }
}

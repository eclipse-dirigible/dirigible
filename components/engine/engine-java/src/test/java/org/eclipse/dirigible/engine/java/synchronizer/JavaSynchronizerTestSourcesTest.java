/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.synchronizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.eclipse.dirigible.engine.java.runtime.JavaLoader;
import org.eclipse.dirigible.engine.java.service.JavaFileService;
import org.junit.jupiter.api.Test;

/**
 * A project's unit tests under {@code custom/test/**} are compiled by its own Maven build against
 * JUnit (dirigible #7643); the platform must leave them out of the client codebase it compiles.
 */
class JavaSynchronizerTestSourcesTest {

    private static final Path REGISTRY = Path.of("/dirigible", "registry", "public");

    private final JavaSynchronizer synchronizer = new JavaSynchronizer(mock(JavaFileService.class), mock(JavaLoader.class));

    @Test
    void acceptsTheApplicationSources() {
        assertTrue(accepted("money", "custom", "LineVatAction.java"));
        assertTrue(accepted("money", "gen", "money", "data", "invoiceline", "InvoiceLineRepository.java"));
    }

    @Test
    void skipsTheUnitTestsOfTheHandWrittenJava() {
        assertFalse(accepted("money", "custom", "test", "LineVatActionTest.java"));
        assertFalse(accepted("money", "custom", "test", "pricing", "DiscountTest.java"));
    }

    @Test
    void keepsAFolderNamedTestElsewhere() {
        assertTrue(accepted("money", "gen", "test", "Fixture.java"));
        assertTrue(accepted("money", "test", "Probe.java"));
        assertTrue(accepted("money", "custom", "testing", "Helper.java"));
    }

    private boolean accepted(String... segments) {
        Path file = REGISTRY;
        for (String segment : segments) {
            file = file.resolve(segment);
        }
        return synchronizer.isAccepted(file, null);
    }
}

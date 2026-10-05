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

import java.nio.file.Path;

/**
 * Where a project keeps the unit tests of its hand-written Java: {@code custom/test/**}, beside the
 * {@code custom/} classes they test (dirigible #7643). Those sources are compiled and run by the
 * module's own Maven build against JUnit, which the platform does not ship - so the client codebase
 * the platform compiles must never contain them, and the in-process test slice must not mistake
 * them for application classes either. Both ask this one rule.
 */
public final class ClientTestSources {

    private static final String CUSTOM_FOLDER = "custom";
    private static final String TEST_FOLDER = "test";

    private ClientTestSources() {}

    /**
     * Tells whether a source file lies under a {@code custom/test} folder.
     *
     * @param file the source file - absolute, or relative to any folder above {@code custom}
     * @return {@code true} for a unit-test source of the project's hand-written Java
     */
    public static boolean isTestSource(Path file) {
        String previous = null;
        for (Path element : file) {
            String name = element.toString();
            if (CUSTOM_FOLDER.equals(previous) && TEST_FOLDER.equals(name)) {
                return true;
            }
            previous = name;
        }
        return false;
    }
}

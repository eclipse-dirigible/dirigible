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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link CompiledModuleClassProvider} discovers a
 * {@code META-INF/dirigible/<project>/.compiled} marker on the classpath, derives the project from
 * the path, and loads the listed class through the application classloader - no runtime
 * compilation.
 */
class CompiledModuleClassProviderTest {

    @Test
    void discovers_marker_and_loads_the_listed_class() {
        // javaLoader is unused by discover(); the classpath carries the test fixture marker at
        // src/test/resources/META-INF/dirigible/aot-test-mod/.compiled.
        CompiledModuleClassProvider provider = new CompiledModuleClassProvider(null);

        List<LoadedClass> discovered = provider.discover();

        LoadedClass fixture = discovered.stream()
                                        .filter(c -> c.fqn()
                                                      .equals(AotFixtureClass.class.getName()))
                                        .findFirst()
                                        .orElse(null);
        assertNotNull(fixture, "the class listed in the .compiled marker must be discovered");
        assertEquals("aot-test-mod", fixture.project(), "project is the path segment under META-INF/dirigible");
        assertSame(AotFixtureClass.class, fixture.type(), "the class is loaded via the application classloader (no compile)");
    }
}

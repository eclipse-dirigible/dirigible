/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A one-pass inspection of a module jar, feeding the swap pipeline's pre-swap validation and
 * payload bookkeeping: which registry projects the jar carries, whether it contains native
 * libraries (rejected on the module tier), and a representative class resource for the
 * platform-shadowing warning.
 */
final class ModuleJarInspector {

    /**
     * The inspection result.
     *
     * @param projects the projects carried under META-INF/dirigible
     * @param nativeLibraries the native library entries (.so / .dylib / .dll), empty for a loadable
     *        module jar
     * @param representativeClassResource a class entry usable to probe whether the parent classpath
     *        already carries this artifact, null when the jar has no classes
     */
    record Inspection(Set<String> projects, List<String> nativeLibraries, String representativeClassResource) {
    }

    /** The registry payload root inside a jar. */
    private static final String DIRIGIBLE_ROOT = "META-INF/dirigible/";

    /**
     * Instantiates a new module jar inspector.
     */
    private ModuleJarInspector() {
        // utility
    }

    /**
     * Inspects the jar in one pass over its entries.
     *
     * @param jarPath the jar
     * @return the inspection
     * @throws IOException when the jar is not a readable archive
     */
    static Inspection inspect(Path jarPath) throws IOException {
        Set<String> projects = new LinkedHashSet<>();
        List<String> nativeLibraries = new ArrayList<>();
        String representativeClassResource = null;
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.startsWith(DIRIGIBLE_ROOT)) {
                    String rest = name.substring(DIRIGIBLE_ROOT.length());
                    int slash = rest.indexOf('/');
                    if (slash > 0) {
                        projects.add(rest.substring(0, slash));
                    }
                } else if (name.endsWith(".so") || name.endsWith(".dylib") || name.endsWith(".dll")) {
                    nativeLibraries.add(name);
                } else if (representativeClassResource == null && name.endsWith(".class") && !name.startsWith("META-INF/")
                        && !"module-info.class".equals(name)) {
                    representativeClassResource = name;
                }
            }
        }
        return new Inspection(projects, nativeLibraries, representativeClassResource);
    }

}

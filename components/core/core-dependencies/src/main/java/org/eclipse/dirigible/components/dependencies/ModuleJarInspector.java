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
     * @param projects the projects carried under META-INF/dirigible - empty when the jar carries the
     *        {@code .skip} marker, because then nothing of it is laid into the registry
     * @param registryEntries the registry-relative paths the jar lays down, in
     *        {@code /<project>/<path>} form - the exact set {@link #projects()} is derived from, and
     *        the granularity at which the payload is removed again
     * @param nativeLibraries the native library entries (.so / .so.N / .dylib / .jnilib / .dll), empty
     *        for a loadable module jar
     * @param representativeClassResource a class entry usable to probe whether the parent classpath
     *        already carries this artifact, null when the jar has no classes
     */
    record Inspection(Set<String> projects, Set<String> registryEntries, List<String> nativeLibraries, String representativeClassResource) {
    }

    /** The registry payload root inside a jar. */
    private static final String DIRIGIBLE_ROOT = "META-INF/dirigible/";

    /** The marker that opts a jar out of the registry expansion entirely. */
    private static final String SKIP_MARKER = DIRIGIBLE_ROOT + ".skip";

    /**
     * A native library entry: the platform suffixes plus the versioned ELF form ({@code libfoo.so.1}).
     * A jar carrying one cannot live on the swappable module tier - the JVM binds a native library to
     * exactly one classloader.
     */
    private static final java.util.regex.Pattern NATIVE_LIBRARY = java.util.regex.Pattern.compile(
            ".*(\\.so|\\.so(\\.\\d+)+|\\.dylib|\\.jnilib|\\.dll)$", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * A registry project name must be a plain path segment - no dots-only names, no separators - so it
     * can never escape the registry when concatenated into a repository path.
     */
    private static final java.util.regex.Pattern PLAIN_PROJECT_NAME = java.util.regex.Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

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
        Set<String> registryEntries = new LinkedHashSet<>();
        List<String> nativeLibraries = new ArrayList<>();
        String representativeClassResource = null;
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            // the .skip marker opts the jar out of the registry expansion (ClasspathExpander honors it
            // too) - such a jar carries NO projects, so its removal must not delete a registry
            // collection it never created
            boolean skipped = jar.getJarEntry(SKIP_MARKER) != null;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.startsWith(DIRIGIBLE_ROOT)) {
                    if (skipped) {
                        continue;
                    }
                    String rest = name.substring(DIRIGIBLE_ROOT.length());
                    int slash = rest.indexOf('/');
                    if (slash > 0) {
                        String project = rest.substring(0, slash);
                        // The project name becomes a registry path segment: on removal it is handed to
                        // ClasspathExpander.remove, whose repository path is plain string concatenation -
                        // a crafted "META-INF/dirigible/../x" entry would name the project ".." and its
                        // removal would delete the registry root. Refuse anything that is not a plain
                        // name, loudly: a jar carrying such an entry is malformed or malicious, and the
                        // expand side already rejects its kind (the Zip-Slip guard).
                        if (!PLAIN_PROJECT_NAME.matcher(project)
                                               .matches()) {
                            throw new IOException("Module jar [" + jarPath.getFileName() + "] declares an invalid registry project name ["
                                    + project + "] under META-INF/dirigible/ - refusing the jar");
                        }
                        projects.add(project);
                        registryEntries.add("/" + rest);
                    }
                } else if (NATIVE_LIBRARY.matcher(name)
                                         .matches()) {
                    nativeLibraries.add(name);
                } else if (representativeClassResource == null && name.endsWith(".class") && !name.startsWith("META-INF/")
                        && !"module-info.class".equals(name)) {
                    representativeClassResource = name;
                }
            }
        }
        return new Inspection(projects, registryEntries, nativeLibraries, representativeClassResource);
    }

}

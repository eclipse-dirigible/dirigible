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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/**
 * The swappable classpath layer for the maven dependency JARs projects declare in
 * {@code project.json}. Sits between the application classloader (its parent) and the
 * {@link ClientClassLoader} generations (whose parent it becomes), so a dependency change takes
 * effect without restarting the platform.
 *
 * <p>
 * <b>Generation lifecycle.</b> A single instance is active at a time, owned by
 * {@link ModulesClassLoaderHolder}. A dependency change never mutates the active instance - the
 * swap pipeline builds a <em>fresh</em> loader over the new JAR set and swaps it in, exactly like a
 * {@code ClientClassLoader} rebuild:
 *
 * <pre>
 * generation N   modules loader over [a-1.0.jar, b-2.0.jar]   (serving)
 *   - project.json changes b to 2.1
 * generation N+1 modules loader over [a-1.0.jar, b-2.1.jar]   (built, validated, swapped in)
 *   - generation N drains: it stays reachable only while in-flight requests hold classes
 *     loaded from it; once those return, GC reclaims the loader and its Metaspace
 * </pre>
 *
 * The retired generation is intentionally never {@link #close() closed} while it may still serve
 * in-flight code - see the holder's javadoc.
 *
 * <p>
 * <b>Immutable-path rule.</b> The JARs are the resolver's outputs at their versioned Maven local
 * repository paths - immutable by Maven's contract. They are never copied to a mutable directory
 * and never overwritten in place, so an open loader of a retired generation always reads consistent
 * bytes. An upgrade is a <em>different path</em>, never a rewritten file.
 *
 * <p>
 * <b>Parent-first delegation</b> (the {@link URLClassLoader} default - deliberately no child-first
 * tricks): a class also present on the platform classpath resolves to the platform's version, which
 * keeps exactly one definition of shared libraries in the JVM.
 */
public final class ModulesClassLoader extends URLClassLoader {

    /** The loader name - stable, so heap dumps and logs identify the layer at a glance. */
    public static final String NAME = "dirigible-modules";

    /** The jars. */
    private final List<Path> jars;

    /**
     * Instantiates a new generation over the given JAR set.
     *
     * @param parent the parent classloader - the application classloader
     * @param jars the JAR paths inside the Maven local repository, in classpath order
     */
    public ModulesClassLoader(ClassLoader parent, List<Path> jars) {
        super(NAME, toUrls(jars), parent);
        this.jars = List.copyOf(jars);
    }

    /**
     * The JAR set this generation serves, in classpath order.
     *
     * @return the jar paths
     */
    public List<Path> jars() {
        return jars;
    }

    /**
     * To urls.
     *
     * @param jars the jars
     * @return the urls
     */
    private static URL[] toUrls(List<Path> jars) {
        URL[] urls = new URL[jars.size()];
        for (int i = 0; i < jars.size(); i++) {
            try {
                urls[i] = jars.get(i)
                              .toUri()
                              .toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Not a loadable jar path: " + jars.get(i), e);
            }
        }
        return urls;
    }

}

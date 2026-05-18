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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * Materialises the application's compile-time classpath as a list of <em>on-disk</em> jar/dir
 * entries that can be passed to {@code javac} via {@code --class-path}.
 *
 * <p>
 * <b>Why on-disk and not in-process?</b> Spring Boot 3's {@code LaunchedClassLoader} reuses pooled
 * {@code NestedJarFile} handles for nested {@code BOOT-INF/lib/*.jar} entries. Reading those
 * resources via {@code ClassLoader.getResourceAsStream} or via in-process classpath scanners (e.g.
 * ClassGraph) inadvertently closes the pooled handles and leaves the running application unable to
 * load classes from the affected jars. The symptom is sporadic {@code NoClassDefFoundError}s on
 * platform classes — a critical correctness regression.
 *
 * <p>
 * To avoid this, we crack the outer fat jar directly via {@link java.util.jar.JarFile} (which
 * bypasses Spring Boot's loader entirely) and extract every {@code BOOT-INF/lib/} entry plus the
 * {@code BOOT-INF/classes/} tree to a temp directory under
 * {@code $TMPDIR/dirigible-engine-java/<pid>/}. The extraction is one-shot per JVM and is cleaned
 * up on shutdown.
 *
 * <p>
 * For development / test runs the application is launched from a plain {@link URLClassLoader}, not
 * a fat jar. In that case we simply collect the loader's URLs — they're already on disk.
 */
@Component
public class ClassPathIndex implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathIndex.class);

    private final AtomicReference<Snapshot> snapshotRef = new AtomicReference<>();

    /** Disk paths that should be passed to {@code javac} via {@code --class-path}. */
    public List<Path> classPathEntries() {
        return snapshot().entries;
    }

    private Snapshot snapshot() {
        Snapshot s = snapshotRef.get();
        if (s != null) {
            return s;
        }
        synchronized (snapshotRef) {
            s = snapshotRef.get();
            if (s != null) {
                return s;
            }
            long start = System.currentTimeMillis();
            s = build();
            snapshotRef.set(s);
            LOGGER.info("Materialised compile-time classpath: [{}] entries, root [{}], took [{}] ms", s.entries.size(), s.extractionRoot,
                    System.currentTimeMillis() - start);
            return s;
        }
    }

    private static Snapshot build() {
        // Locate the JAR (or directory) that contains JavaHandler — that's our anchor: if it's a
        // fat jar, we know we're in fat-jar mode and the nested layout convention applies.
        Path anchor = locateAnchorOnDisk();
        if (anchor != null && anchor.toString()
                                    .endsWith(".jar")
                && isFatJar(anchor)) {
            return extractFatJar(anchor);
        }
        // Dev/test: classpath is already on disk via URLClassLoader URLs.
        return collectFromUrlClassLoader();
    }

    private static Path locateAnchorOnDisk() {
        try {
            URL location = JavaHandler.class.getProtectionDomain()
                                            .getCodeSource()
                                            .getLocation();
            if (location == null) {
                return null;
            }
            URI uri = location.toURI();
            // For Spring Boot 3 fat jars this comes back as jar:nested:/...!BOOT-INF/lib/...
            // The "outermost" path before the first '!' is the actual on-disk fat jar.
            String s = uri.toString();
            if (s.startsWith("jar:nested:")) {
                String trimmed = s.substring("jar:nested:".length());
                int bang = trimmed.indexOf('!');
                if (bang > 0) {
                    trimmed = trimmed.substring(0, bang);
                }
                return Path.of(trimmed);
            }
            if (s.endsWith("/")) {
                // file: directory — anchor is the directory itself
                return Path.of(uri);
            }
            return Path.of(uri);
        } catch (URISyntaxException | RuntimeException e) {
            LOGGER.warn("Could not locate the application's on-disk anchor: {}", e.getMessage());
            return null;
        }
    }

    private static boolean isFatJar(Path jar) {
        try (JarFile jf = new JarFile(jar.toFile())) {
            return jf.getEntry("BOOT-INF/classpath.idx") != null || jf.getEntry("BOOT-INF/lib/") != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static Snapshot extractFatJar(Path fatJar) {
        Path root;
        try {
            root = Files.createTempDirectory("dirigible-engine-java-");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create temp directory for classpath extraction", e);
        }
        Path libDir = root.resolve("lib");
        Path classesDir = root.resolve("classes");
        List<Path> entries = new ArrayList<>();

        try (JarFile jar = new JarFile(fatJar.toFile())) {
            Files.createDirectories(libDir);
            Files.createDirectories(classesDir);

            Path libBase = libDir.toAbsolutePath()
                                 .normalize();
            Path classesBase = classesDir.toAbsolutePath()
                                         .normalize();
            Enumeration<JarEntry> es = jar.entries();
            while (es.hasMoreElements()) {
                JarEntry entry = es.nextElement();
                String name = entry.getName();
                if (name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar") && !entry.isDirectory()) {
                    String jarName = name.substring("BOOT-INF/lib/".length());
                    Path target = safeResolve(libBase, jarName);
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    entries.add(target);
                } else if (name.startsWith("BOOT-INF/classes/") && !entry.isDirectory()) {
                    String relative = name.substring("BOOT-INF/classes/".length());
                    Path target = safeResolve(classesBase, relative);
                    Files.createDirectories(target.getParent());
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            // The BOOT-INF/classes tree (Spring Boot's location for the app's own classes) goes
            // on the classpath as a directory.
            if (Files.exists(classesDir)) {
                entries.add(classesDir);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract classpath from " + fatJar + ": " + e.getMessage(), e);
        }
        return new Snapshot(Collections.unmodifiableList(entries), root);
    }

    /**
     * Resolve a jar entry's relative path against the extraction base directory while guarding against
     * zip-slip: a malicious entry named {@code ../../etc/passwd} would otherwise escape the temp dir
     * and overwrite arbitrary files. Throws {@link IOException} when the resolved target does not stay
     * under {@code base}.
     */
    private static Path safeResolve(Path base, String relative) throws IOException {
        Path target = base.resolve(relative)
                          .normalize();
        if (!target.startsWith(base)) {
            throw new IOException("Unsafe jar entry [" + relative + "] would escape extraction directory [" + base + "]");
        }
        return target;
    }

    private static Snapshot collectFromUrlClassLoader() {
        URLClassLoader urlcl = findUrlClassLoader(JavaHandler.class.getClassLoader());
        if (urlcl == null) {
            LOGGER.warn("No URLClassLoader found in the loader hierarchy; compile classpath will rely on java.class.path only");
            return new Snapshot(List.of(), null);
        }
        List<Path> entries = new ArrayList<>();
        for (URL url : urlcl.getURLs()) {
            try {
                if (url.getProtocol()
                       .equals("file")) {
                    entries.add(Path.of(url.toURI()));
                }
            } catch (URISyntaxException e) {
                LOGGER.warn("Skipping unparseable classpath URL [{}]: {}", url, e.getMessage());
            }
        }
        return new Snapshot(Collections.unmodifiableList(entries), null);
    }

    private static URLClassLoader findUrlClassLoader(ClassLoader cl) {
        ClassLoader w = cl;
        while (w != null) {
            if (w instanceof URLClassLoader urlcl) {
                return urlcl;
            }
            w = w.getParent();
        }
        return null;
    }

    @Override
    public void destroy() {
        Snapshot s = snapshotRef.getAndSet(null);
        if (s == null || s.extractionRoot == null) {
            return;
        }
        try (var stream = Files.walk(s.extractionRoot)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                  .forEach(p -> {
                      try {
                          Files.deleteIfExists(p);
                      } catch (IOException ignore) {
                          // Best-effort cleanup; we're in shutdown.
                      }
                  });
        } catch (IOException ignore) {
            // Best-effort cleanup; we're in shutdown.
        }
    }

    private record Snapshot(List<Path> entries, Path extractionRoot) {
    }

}

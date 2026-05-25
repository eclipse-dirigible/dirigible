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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
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
 * {@code BOOT-INF/classes/} tree to a stable cache directory at
 * {@code $HOME/.dirigible/engine-java-classpath/}. Extraction is skipped on subsequent starts when
 * the fat jar's last-modified timestamp matches the stored stamp, so restarting the application
 * does not re-extract gigabytes of JARs. When the fat jar is rebuilt (its mtime changes) the cache
 * is invalidated and re-extracted automatically.
 *
 * <p>
 * For development / test runs the application is launched from a plain {@link URLClassLoader}, not
 * a fat jar. In that case we simply collect the loader's URLs — they're already on disk and no
 * extraction is needed.
 */
@Component
public class ClassPathIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathIndex.class);

    /** Subdirectory under {@code $HOME} where extracted JARs are cached. */
    private static final String CACHE_SUBDIR = ".dirigible/engine-java-classpath";

    /** Stamp file recording the fat-jar mtime at the time of last extraction. */
    private static final String STAMP_FILENAME = "extracted.stamp";

    private final AtomicReference<List<Path>> entriesRef = new AtomicReference<>();

    /** Disk paths that should be passed to {@code javac} via {@code --class-path}. */
    public List<Path> classPathEntries() {
        List<Path> cached = entriesRef.get();
        if (cached != null) {
            return cached;
        }
        synchronized (entriesRef) {
            cached = entriesRef.get();
            if (cached != null) {
                return cached;
            }
            long start = System.currentTimeMillis();
            cached = build();
            entriesRef.set(cached);
            LOGGER.info("Materialised compile-time classpath: [{}] entries, took [{}] ms", cached.size(),
                    System.currentTimeMillis() - start);
            return cached;
        }
    }

    private static List<Path> build() {
        Path anchor = locateAnchorOnDisk();
        if (anchor != null && anchor.toString()
                                    .endsWith(".jar")
                && isFatJar(anchor)) {
            return extractFatJar(anchor);
        }
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

    private static List<Path> extractFatJar(Path fatJar) {
        Path cacheRoot = Path.of(System.getProperty("user.home"))
                             .resolve(CACHE_SUBDIR);
        Path libDir = cacheRoot.resolve("lib");
        Path classesDir = cacheRoot.resolve("classes");
        Path stampFile = cacheRoot.resolve(STAMP_FILENAME);

        try {
            String currentStamp = String.valueOf(Files.getLastModifiedTime(fatJar)
                                                      .toMillis());

            if (isCacheValid(stampFile, currentStamp, libDir)) {
                List<Path> entries = collectExistingEntries(libDir, classesDir);
                if (!entries.isEmpty()) {
                    LOGGER.info("Reusing stable classpath cache at [{}]: [{}] entries", cacheRoot, entries.size());
                    return Collections.unmodifiableList(entries);
                }
                LOGGER.warn("Classpath cache at [{}] has valid stamp but no entries; re-extracting", cacheRoot);
            }

            LOGGER.info("Extracting platform classpath from [{}] to [{}]", fatJar, cacheRoot);
            deleteDirectory(libDir);
            deleteDirectory(classesDir);
            Files.createDirectories(libDir);
            Files.createDirectories(classesDir);

            Path libBase = libDir.toAbsolutePath()
                                 .normalize();
            Path classesBase = classesDir.toAbsolutePath()
                                         .normalize();
            List<Path> entries = new ArrayList<>();

            try (JarFile jar = new JarFile(fatJar.toFile())) {
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
            }

            if (Files.exists(classesDir)) {
                entries.add(classesDir);
            }

            // Write stamp only after successful extraction so a partial extraction is re-tried.
            Files.writeString(stampFile, currentStamp);
            LOGGER.info("Classpath extraction complete: [{}] entries cached at [{}]", entries.size(), cacheRoot);
            return Collections.unmodifiableList(entries);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract classpath from " + fatJar + ": " + e.getMessage(), e);
        }
    }

    private static boolean isCacheValid(Path stampFile, String expectedStamp, Path libDir) {
        if (!Files.isRegularFile(stampFile) || !Files.isDirectory(libDir)) {
            return false;
        }
        try {
            return expectedStamp.equals(Files.readString(stampFile)
                                             .trim());
        } catch (IOException e) {
            return false;
        }
    }

    private static List<Path> collectExistingEntries(Path libDir, Path classesDir) throws IOException {
        List<Path> entries = new ArrayList<>();
        if (Files.isDirectory(libDir)) {
            try (var stream = Files.list(libDir)) {
                stream.filter(p -> p.toString()
                                    .endsWith(".jar"))
                      .forEach(entries::add);
            }
        }
        if (Files.isDirectory(classesDir)) {
            entries.add(classesDir);
        }
        return entries;
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Resolve a jar entry's relative path against the extraction base directory while guarding against
     * zip-slip.
     */
    private static Path safeResolve(Path base, String relative) throws IOException {
        Path target = base.resolve(relative)
                          .normalize();
        if (!target.startsWith(base)) {
            throw new IOException("Unsafe jar entry [" + relative + "] would escape extraction directory [" + base + "]");
        }
        return target;
    }

    private static List<Path> collectFromUrlClassLoader() {
        URLClassLoader urlcl = findUrlClassLoader(JavaHandler.class.getClassLoader());
        if (urlcl == null) {
            LOGGER.warn("No URLClassLoader found in the loader hierarchy; compile classpath will rely on java.class.path only");
            return List.of();
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
        return Collections.unmodifiableList(entries);
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
}

/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Scans the main sources of the repository for the configuration keys they read and checks them
 * against the {@link DirigibleConfig} catalogue (#7759), so a key can no longer be read without
 * being listed, and a deprecated key cannot quietly come back into use.
 * <p>
 * A key counts as read when it is
 * <ul>
 * <li>a literal argument of {@code Configuration.get / getAsInt / setIfNull},</li>
 * <li>a {@code String} constant initialised to the key and passed to one of those by name, or</li>
 * <li>a Spring placeholder {@code ${DIRIGIBLE_...}} in a {@code .properties} file or an
 * {@code @Value}.</li>
 * </ul>
 * Keys built at runtime ({@code "DIRIGIBLE_DATABASE_CUSTOM_" + name}) are families, not entries,
 * and are not seen.
 */
public class DirigibleConfigSourceScanTest {

    /** A read through the Configuration facade. */
    private static final String READ = "Configuration\\.(?:get|getAsInt|setIfNull)\\(\\s*";

    /** A literal key read directly. */
    private static final Pattern LITERAL_READ = Pattern.compile(READ + "\"(DIRIGIBLE_[A-Z0-9_]*[A-Z0-9])\"");

    /** A constant read by name, possibly qualified. */
    private static final Pattern CONSTANT_READ = Pattern.compile(READ + "(?:\\w+\\.)*(\\w+)\\b");

    /** A String constant holding a key. */
    private static final Pattern CONSTANT = Pattern.compile("String\\s+(\\w+)\\s*=\\s*\"(DIRIGIBLE_[A-Z0-9_]*[A-Z0-9])\"\\s*;");

    /** A Spring placeholder. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(DIRIGIBLE_[A-Z0-9_]*[A-Z0-9])[:}]");

    /** The directories never scanned. */
    private static final Set<String> SKIPPED = Set.of("target", "node_modules", "tests");

    /** The keys read, each with a file that reads it. */
    private static Map<String, Path> read;

    @BeforeClass
    public static void scan() throws IOException {
        Path root = repositoryRoot();
        assumeTrue("the repository sources are not around this module", root != null);

        List<Path> sources = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName() == null ? ""
                        : dir.getFileName()
                             .toString();
                return !dir.equals(root) && (name.startsWith(".") || SKIPPED.contains(name)) ? FileVisitResult.SKIP_SUBTREE
                        : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String path = file.toString()
                                  .replace('\\', '/');
                if (path.contains("/src/main/") && (path.endsWith(".java") || path.endsWith(".properties"))) {
                    sources.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        read = new TreeMap<>();
        Map<String, List<String>> constants = new HashMap<>();
        Map<Path, String> javaSources = new HashMap<>();
        for (Path source : sources) {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            collect(PLACEHOLDER, content, source);
            if (source.toString()
                      .endsWith(".java")) {
                javaSources.put(source, content);
                collect(LITERAL_READ, content, source);
                Matcher constant = CONSTANT.matcher(content);
                while (constant.find()) {
                    constants.computeIfAbsent(constant.group(1), name -> new ArrayList<>())
                             .add(constant.group(2));
                }
            }
        }
        javaSources.forEach((source, content) -> {
            Matcher readByName = CONSTANT_READ.matcher(content);
            while (readByName.find()) {
                for (String key : constants.getOrDefault(readByName.group(1), List.of())) {
                    read.putIfAbsent(key, source);
                }
            }
        });
        assertTrue("the scan found the reads, " + read.size() + " keys", read.size() > 100);
    }

    private static void collect(Pattern pattern, String content, Path source) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            read.putIfAbsent(matcher.group(1), source);
        }
    }

    /**
     * Walks up from the working directory to the repository root - the directory that holds this
     * module.
     *
     * @return the root, or null when the module was not built from the repository
     */
    private static Path repositoryRoot() {
        Path dir = Path.of("")
                       .toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("modules/commons/commons-config")) && Files.isDirectory(dir.resolve("components"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    @Test
    public void everyKeyReadInMainCodeIsCatalogued() {
        Map<String, Path> unlisted = new TreeMap<>();
        read.forEach((key, source) -> {
            if (DirigibleConfig.fromKey(key)
                               .isEmpty()) {
                unlisted.put(key, source);
            }
        });
        assertTrue("read but not catalogued in DirigibleConfig - add an entry with its real default: " + unlisted, unlisted.isEmpty());
    }

    @Test
    public void noDeprecatedKeyIsRead() {
        Map<String, Path> deprecatedButRead = new TreeMap<>();
        read.forEach((key, source) -> DirigibleConfig.fromKey(key)
                                                     .filter(config -> config.isDeprecated() && config.getDeprecatedBy() == null)
                                                     .ifPresent(config -> deprecatedButRead.put(key, source)));
        assertTrue("deprecated as never read, but read - drop the deprecation: " + deprecatedButRead, deprecatedButRead.isEmpty());
    }
}

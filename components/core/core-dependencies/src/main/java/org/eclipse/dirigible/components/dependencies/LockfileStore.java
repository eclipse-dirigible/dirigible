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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Reads and writes the dependency lockfile - the configured path or {@code project-lock.json}
 * inside the resolved-modules directory, so baking that one directory into an image carries the
 * whole reproducibility bundle (the seed jars plus the lock that verifies them).
 *
 * <p>
 * The serialization is deliberately deterministic (sorted artifacts, stable field order, pretty
 * printed), so two locks diff line by line - the reviewability the lockfile exists for.
 */
@Component
class LockfileStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockfileStore.class);

    /** The lockfile name inside the resolved-modules directory. */
    private static final String LOCKFILE_NAME = "project-lock.json";

    /**
     * A plain Gson - the shared helpers exclude fields without {@code @Expose} and would silently
     * serialize nothing.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
                                                      .disableHtmlEscaping()
                                                      .create();

    /** The linker owning the resolved-modules directory the default path derives from. */
    private final ResolvedModulesLinker linker;

    /**
     * Instantiates a new lockfile store.
     *
     * @param linker the resolved-modules linker
     */
    LockfileStore(ResolvedModulesLinker linker) {
        this.linker = linker;
    }

    /**
     * The lockfile path - the configured location or {@code project-lock.json} inside the
     * resolved-modules directory.
     *
     * @return the path
     */
    Path path() {
        String configured = DirigibleConfig.DEPENDENCIES_LOCKFILE.getStringValue();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return linker.directory()
                     .resolve(LOCKFILE_NAME);
    }

    /**
     * Reads the lockfile.
     *
     * @return the lockfile, empty when absent or unreadable
     */
    Optional<Lockfile> read() {
        Path path = path();
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            Lockfile lockfile = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Lockfile.class);
            return Optional.ofNullable(lockfile);
        } catch (IOException | JsonParseException e) {
            LOGGER.error("The lockfile [{}] is unreadable - it is ignored until the next clean resolution rewrites it", path, e);
            return Optional.empty();
        }
    }

    /**
     * Writes the lockfile.
     *
     * @param lockfile the lockfile
     */
    void write(Lockfile lockfile) {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(lockfile), StandardCharsets.UTF_8);
            LOGGER.info("Lockfile [{}] written: [{}] artifact(s), [{}] mediation(s)", path, lockfile.artifacts()
                                                                                                    .size(),
                    lockfile.mediated()
                            .size());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write the lockfile [" + path + "]", e);
        }
    }

    /**
     * The SHA-256 of a file, hex.
     *
     * @param file the file
     * @return the digest
     * @throws IOException when the file is unreadable
     */
    static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandated by every JVM", e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of()
                        .formatHex(digest.digest());
    }

}

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

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The instance-level configuration of the Maven dependency resolution - repositories and their
 * credentials are operator configuration and deliberately never part of project.json.
 *
 * @param localRepository the local repository the artifacts are resolved into
 * @param repositories the remote repositories to resolve from, in configuration order
 * @param offline whether resolution runs offline (local repository only)
 * @param settingsXml the user's Maven settings.xml whose mirrors and proxies are honored, null when
 *        absent
 */
record MavenResolverConfig(Path localRepository, List<MavenRepository> repositories, boolean offline, Path settingsXml) {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenResolverConfig.class);

    /**
     * The id of the default Maven Central entry - a configured entry with this id overrides its URL.
     */
    private static final String CENTRAL_ID = "central";

    /** The default Maven Central URL. */
    private static final String CENTRAL_URL = "https://repo1.maven.org/maven2";

    /**
     * A remote repository definition.
     *
     * @param id the repository id
     * @param url the repository URL
     * @param username the username, null when the repository needs no authentication
     * @param password the password, null when the repository needs no authentication
     */
    record MavenRepository(String id, String url, String username, String password) {
    }

    /**
     * Reads the configuration from the environment - evaluated on every resolution so runtime
     * configuration changes take effect without a restart.
     *
     * @return the configuration
     */
    static MavenResolverConfig fromConfiguration() {
        Path userHome = Path.of(System.getProperty("user.home"));
        return new MavenResolverConfig(localRepository(userHome), configuredRepositories(), DirigibleConfig.MAVEN_OFFLINE.getBooleanValue(),
                settingsXml(userHome));
    }

    /**
     * The local repository - the configured location, else the developer's own local repository when
     * one exists (cache parity with local builds), else a platform-owned directory.
     *
     * @param userHome the user home
     * @return the local repository path
     */
    private static Path localRepository(Path userHome) {
        String configured = DirigibleConfig.MAVEN_LOCAL_REPO.getStringValue();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path m2Repository = userHome.resolve(".m2")
                                    .resolve("repository");
        if (Files.isDirectory(m2Repository)) {
            return m2Repository;
        }
        return userHome.resolve(".dirigible")
                       .resolve("m2");
    }

    /**
     * The remote repositories - Maven Central by default plus the operator-configured entries. An entry
     * with the id central overrides the default Central URL.
     *
     * @return the repositories in configuration order
     */
    private static List<MavenRepository> configuredRepositories() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put(CENTRAL_ID, CENTRAL_URL);
        String configured = DirigibleConfig.MAVEN_REPOSITORIES.getStringValue();
        if (configured != null && !configured.isBlank()) {
            for (String entry : configured.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0 || separator == trimmed.length() - 1) {
                    LOGGER.warn("Ignoring the malformed maven repository entry [{}] - expected id=url", trimmed);
                    continue;
                }
                urls.put(trimmed.substring(0, separator)
                                .trim(),
                        trimmed.substring(separator + 1)
                               .trim());
            }
        }
        List<MavenRepository> repositories = new ArrayList<>();
        urls.forEach((id, url) -> repositories.add(new MavenRepository(id, url, credential(id, "USERNAME"), credential(id, "PASSWORD"))));
        return repositories;
    }

    /**
     * A repository credential from the DIRIGIBLE_MAVEN_[ID]_USERNAME / DIRIGIBLE_MAVEN_[ID]_PASSWORD
     * pair - the id is uppercased and every non-alphanumeric character becomes an underscore.
     *
     * @param repositoryId the repository id
     * @param suffix USERNAME or PASSWORD
     * @return the credential, null when not configured
     */
    private static String credential(String repositoryId, String suffix) {
        String normalizedId = repositoryId.toUpperCase(Locale.ROOT)
                                          .replaceAll("[^A-Z0-9]", "_");
        return Configuration.get("DIRIGIBLE_MAVEN_" + normalizedId + "_" + suffix);
    }

    /**
     * The user's Maven settings.xml when it exists.
     *
     * @param userHome the user home
     * @return the settings.xml path, null when absent
     */
    private static Path settingsXml(Path userHome) {
        Path settings = userHome.resolve(".m2")
                                .resolve("settings.xml");
        return Files.isRegularFile(settings) ? settings : null;
    }

}

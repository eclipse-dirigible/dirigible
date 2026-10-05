/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * The state a previous release leaves behind, produced by that release itself: a PostgreSQL holding
 * its DefaultDB and SystemDB, and the repository folder it wrote - together what a deployment's
 * volume carries from one release to the next. The release runs as the published
 * {@code dirigiblelabs/dirigible} image; the test JVM then boots the release under test on the same
 * database and a copy of that folder.
 *
 * <p>
 * The folder is copied out of the stopped container rather than bind-mounted: the image runs as
 * root, so on a Linux runner a bind mount would leave files the test JVM cannot write.
 */
public final class PreviousReleaseEnvironment implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviousReleaseEnvironment.class);

    /** The DefaultDB database. */
    public static final String DEFAULT_DATABASE = "testdb";

    /** The SystemDB database. */
    public static final String SYSTEM_DATABASE = "systemdb";

    /** The database user. */
    public static final String USER = "testuser";

    /** The database password. */
    public static final String PASSWORD = "testpass";

    private static final String POSTGRES_ALIAS = "postgres";

    /**
     * The repository folder of the image: no WORKDIR, so the default {@code target} resolves under /.
     */
    private static final String REPOSITORY_FOLDER = "/target/dirigible/repository";

    private final String tag;
    private final Network network = Network.newNetwork();
    private final GenericContainer<?> postgres;
    private final GenericContainer<?> release;

    /**
     * An environment for the given release, not started yet.
     *
     * @param tag the tag of the {@code dirigiblelabs/dirigible} image
     */
    @SuppressWarnings("resource")
    public PreviousReleaseEnvironment(String tag) {
        this.tag = tag;
        postgres = new GenericContainer<>(DockerImageName.parse("postgres:16")).withNetwork(network)
                                                                               .withNetworkAliases(POSTGRES_ALIAS)
                                                                               .withExposedPorts(5432)
                                                                               .withEnv(Map.of("POSTGRES_DB", DEFAULT_DATABASE,
                                                                                       "POSTGRES_USER", USER, "POSTGRES_PASSWORD",
                                                                                       PASSWORD))
                                                                               .waitingFor(Wait.forLogMessage(
                                                                                       ".*database system is ready to accept connections.*\\s",
                                                                                       2));
        release = new GenericContainer<>(DockerImageName.parse("dirigiblelabs/dirigible:" + tag)).withNetwork(network)
                                                                                                 .withExposedPorts(8080)
                                                                                                 .withEnv(releaseEnvironment())
                                                                                                 .withLogConsumer(new Slf4jLogConsumer(
                                                                                                         LoggerFactory.getLogger(
                                                                                                                 "previous-release-"
                                                                                                                         + tag)))
                                                                                                 .waitingFor(Wait.forHttp(
                                                                                                         "/actuator/health/readiness")
                                                                                                                 .forPort(8080)
                                                                                                                 .forStatusCode(200)
                                                                                                                 .withStartupTimeout(
                                                                                                                         Duration.ofMinutes(
                                                                                                                                 10)));
    }

    private static Map<String, String> releaseEnvironment() {
        String host = "jdbc:postgresql://" + POSTGRES_ALIAS + ":5432/";
        return Map.of("DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER", "org.postgresql.Driver", //
                "DIRIGIBLE_DATASOURCE_DEFAULT_URL", host + DEFAULT_DATABASE, //
                "DIRIGIBLE_DATASOURCE_DEFAULT_USERNAME", USER, //
                "DIRIGIBLE_DATASOURCE_DEFAULT_PASSWORD", PASSWORD, //
                "DIRIGIBLE_DATABASE_SYSTEM_DRIVER", "org.postgresql.Driver", //
                "DIRIGIBLE_DATABASE_SYSTEM_URL", host + SYSTEM_DATABASE, //
                "DIRIGIBLE_DATABASE_SYSTEM_USERNAME", USER, //
                "DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", PASSWORD);
    }

    /**
     * Starts the database, creates the SystemDB in it, and starts the previous release on both.
     *
     * @return a client of the started release
     */
    public PreviousReleaseClient start() {
        postgres.start();
        createSystemDatabase();
        LOGGER.info("Starting the previous release [{}]...", tag);
        release.start();
        LOGGER.info("The previous release [{}] is ready", tag);
        return new PreviousReleaseClient("http://" + release.getHost() + ":" + release.getMappedPort(8080));
    }

    private void createSystemDatabase() {
        psql(DEFAULT_DATABASE, "CREATE DATABASE " + SYSTEM_DATABASE);
    }

    /**
     * Stops the previous release the way an orchestrator does - SIGTERM and a grace period - so its
     * shutdown hooks run as they would before an upgrade, and copies the repository folder it wrote.
     *
     * @param repositoryFolder where the repository goes: the {@code dirigible/repository} folder of the
     *        release under test, absent or empty
     */
    public void stopReleaseAndCopyRepository(Path repositoryFolder) {
        String containerId = release.getContainerId();
        DockerClientFactory.instance()
                           .client()
                           .stopContainerCmd(containerId)
                           .withTimeout(60)
                           .exec();
        LOGGER.info("Stopped the previous release [{}]; copying its repository into [{}]", tag, repositoryFolder);
        try (InputStream archive = DockerClientFactory.instance()
                                                      .client()
                                                      .copyArchiveFromContainerCmd(containerId, REPOSITORY_FOLDER)
                                                      .exec();
                TarArchiveInputStream tar = new TarArchiveInputStream(archive)) {
            extract(tar, repositoryFolder);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not copy the repository of the previous release", ex);
        }
    }

    /** The archive's entries are rooted at the copied folder's own name, {@code repository/...}. */
    private static void extract(TarArchiveInputStream tar, Path repositoryFolder) throws IOException {
        Path root = repositoryFolder.toAbsolutePath()
                                    .normalize();
        Files.createDirectories(root);
        TarArchiveEntry entry;
        while ((entry = tar.getNextEntry()) != null) {
            String name = entry.getName();
            int slash = name.indexOf('/');
            if (slash < 0 || slash == name.length() - 1) {
                continue;
            }
            Path target = root.resolve(name.substring(slash + 1))
                              .normalize();
            if (!target.startsWith(root)) {
                throw new IOException("The archive entry [" + name + "] leaves the repository folder");
            }
            if (entry.isDirectory()) {
                Files.createDirectories(target);
            } else if (entry.isFile()) {
                Files.createDirectories(target.getParent());
                Files.copy(tar, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Points the datasources the previous release recorded at the address the test JVM reaches the
     * database by. A deployment keeps its database address across an upgrade, but here the two releases
     * reach the same PostgreSQL by two names - the container by its network alias, the test JVM by the
     * mapped port - and a datasource stores its URL as resolved when its file was parsed, which an
     * unchanged file never is again.
     */
    public void readdressRecordedDataSources() {
        String from = "//" + POSTGRES_ALIAS + ":5432/";
        String to = "//" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/";
        String sql = "UPDATE DIRIGIBLE_DATA_SOURCES SET DS_URL = replace(DS_URL, '" + from + "', '" + to + "') WHERE DS_URL LIKE '%" + from
                + "%'";
        String output = psql(SYSTEM_DATABASE, sql);
        if (!output.matches("(?s).*UPDATE [1-9]\\d*.*")) {
            throw new IllegalStateException("The previous release recorded no datasource at [" + from + "]: " + output);
        }
        LOGGER.info("Readdressed the datasources the previous release recorded from [{}] to [{}]: {}", from, to, output.strip());
    }

    private String psql(String database, String sql) {
        try {
            ExecResult result = postgres.execInContainer("psql", "-v", "ON_ERROR_STOP=1", "-U", USER, "-d", database, "-c", sql);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("psql failed on [" + database + "]: " + result.getStderr());
            }
            return result.getStdout();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not run psql on [" + database + "]", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted running psql on [" + database + "]", ex);
        }
    }

    /**
     * The JDBC URL of one of the databases, as the test JVM reaches it.
     *
     * @param database {@link #DEFAULT_DATABASE} or {@link #SYSTEM_DATABASE}
     * @return the URL
     */
    public String jdbcUrl(String database) {
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }

    /** Stops and removes both containers and their network. */
    @Override
    public void close() {
        release.stop();
        postgres.stop();
        network.close();
    }
}

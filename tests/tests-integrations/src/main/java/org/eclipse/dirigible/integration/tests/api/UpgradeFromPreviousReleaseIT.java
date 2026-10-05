/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.upgrade.PreviousRelease;
import org.eclipse.dirigible.tests.framework.upgrade.PreviousReleaseClient;
import org.eclipse.dirigible.tests.framework.upgrade.PreviousReleaseEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * The release under test boots on a deployment the PREVIOUS release wrote (#7641) - every other IT
 * boots on an empty database, and the upgrade failures that reached users (#7008, #7370, #7435,
 * #7597, #7049) were exactly the ones only an existing database shows.
 *
 * <p>
 * Before the application context starts, the previous release ({@code tests/UPGRADE_FROM}) runs as
 * its published image on a PostgreSQL of its own: it generates and publishes an intent project,
 * takes rows in every entity, parks a process instance on a user task and fires a scheduled job; it
 * is then stopped like an orchestrator stops it and its repository folder is copied. The release
 * under test boots on that database and that folder, with
 * {@code DIRIGIBLE_READINESS_REQUIRE_CLEAN_BOOT}, and has to: accept traffic, leave no artefact
 * failed, run the system changelog forward, keep every row and the process instances, and fire the
 * job again.
 *
 * <p>
 * Tagged {@code upgrade}: it pulls a release image and needs Docker, so it runs in its own job of
 * the nightly and the release workflows, not in the shards or the PR gate.
 */
@Tag("upgrade")
class UpgradeFromPreviousReleaseIT extends IntegrationTest {

    private static final String PROJECT = "upgrade-it";
    private static final String INTENT = "app.intent";

    private static final String API = "/services/java/" + PROJECT + "/gen/upgrade/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String TICKETS = API + "/ticket/TicketController";
    private static final String DIGESTS = API + "/ticketdigest/TicketDigestController";
    private static final String PROCESS_INSTANCES = "/services/bpm/bpm-processes/instances";

    private static final String PROCESS = "TicketReview";

    private static final Duration PUBLISH_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration JOB_TIMEOUT = Duration.ofMinutes(2);
    private static final long BOOT_TIMEOUT_SECONDS = 600;

    private static final Gson GSON = new Gson();

    private static PreviousReleaseEnvironment previousRelease;

    /** What the previous release left, read through its own API just before it stopped. */
    private static List<Map<String, Object>> customers;
    private static List<Map<String, Object>> tickets;
    private static List<Map<String, Object>> digests;
    private static Set<String> processInstances;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    /**
     * Runs after the base class has wiped the Dirigible folder and before the application context
     * starts - the release under test boots on what this leaves.
     */
    @BeforeAll
    static void letThePreviousReleaseWriteTheDeployment() {
        previousRelease = new PreviousReleaseEnvironment(PreviousRelease.tag());
        PreviousReleaseClient client = previousRelease.start();

        client.createProject(PROJECT);
        client.writeFile(PROJECT, INTENT, resource(UpgradeFromPreviousReleaseIT.class.getSimpleName() + "/" + INTENT));
        client.generateFromIntent(PROJECT, INTENT);
        client.publish(PROJECT);
        client.awaitAvailable(CUSTOMERS, PUBLISH_TIMEOUT);

        int customer = id(client.create(CUSTOMERS, Map.of("Name", "Acme")));
        client.create(TICKETS, Map.of("Subject", "Printer jam", "Customer", customer));
        client.create(TICKETS, Map.of("Subject", "VPN down", "Customer", customer));

        customers = client.list(CUSTOMERS);
        // A process instance per ticket, its id stamped on the ticket once the trigger has run.
        tickets = client.awaitList(TICKETS, rows -> rows.size() == 2 && rows.stream()
                                                                            .allMatch(row -> row.get("ProcessId") != null),
                PUBLISH_TIMEOUT);
        digests = client.awaitList(DIGESTS, rows -> rows.size() == 2, JOB_TIMEOUT);
        processInstances = processInstanceIds(client.list(PROCESS_INSTANCES));
        assertThat(processInstances).as("the previous release parks one review per ticket")
                                    .hasSize(2);

        previousRelease.stopReleaseAndCopyRepository(
                Path.of(DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue(), "dirigible", "repository"));
        previousRelease.readdressRecordedDataSources();
        bootTheReleaseUnderTestOnTheSameDatabase();
    }

    private static void bootTheReleaseUnderTestOnTheSameDatabase() {
        // Runtime values win over the environment, so this holds on a CI leg that exports its own.
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER", "org.postgresql.Driver");
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_URL", previousRelease.jdbcUrl(PreviousReleaseEnvironment.DEFAULT_DATABASE));
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_USERNAME", PreviousReleaseEnvironment.USER);
        Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_PASSWORD", PreviousReleaseEnvironment.PASSWORD);
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_DRIVER", "org.postgresql.Driver");
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_URL", previousRelease.jdbcUrl(PreviousReleaseEnvironment.SYSTEM_DATABASE));
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_USERNAME", PreviousReleaseEnvironment.USER);
        Configuration.set("DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", PreviousReleaseEnvironment.PASSWORD);
        DirigibleConfig.READINESS_AVAILABILITY_BRIDGE_ENABLED.setBooleanValue(true);
        DirigibleConfig.READINESS_REQUIRE_CLEAN_BOOT.setBooleanValue(true);
    }

    @AfterAll
    static void removeThePreviousRelease() {
        if (previousRelease != null) {
            previousRelease.close();
        }
    }

    @Test
    void theReleaseUnderTestTakesOverWhatThePreviousReleaseLeft() throws SQLException {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/actuator/health/readiness")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("status", equalTo("UP")),
                BOOT_TIMEOUT_SECONDS);
        assertThat(PlatformReadiness.getInstance()
                                    .isCleanBoot()).as("a clean boot: no failed artefact, every AOT-listed class registered")
                                                   .isTrue();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/actuator/health")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.artefacts.details.failed", equalTo(0)));

        assertThat(appliedChangeSets()).as("the system changelog ran forward over the previous release's schema")
                                       .containsAll(declaredChangeSets());

        assertThat(list(CUSTOMERS)).as("customers")
                                   .isEqualTo(customers);
        assertThat(list(TICKETS)).as("tickets")
                                 .isEqualTo(tickets);
        assertThat(list(DIGESTS)).as("digests")
                                 .isEqualTo(digests);
        assertThat(processInstanceIds(list(PROCESS_INSTANCES))).as("the reviews still running")
                                                               .containsAll(processInstances);

        // The release under test serves the application, starts its process and fires its job.
        int customer = id(customers.get(0));
        AtomicReference<String> created = new AtomicReference<>();
        restAssuredExecutor.execute(() -> created.set(given().contentType("application/json")
                                                             .body(GSON.toJson(Map.of("Subject", "Disk full", "Customer", customer)))
                                                             .when()
                                                             .post(TICKETS)
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .asString()));
        int ticket = id(GSON.fromJson(created.get(), new TypeToken<Map<String, Object>>() {}.getType()));
        restAssuredExecutor.execute(() -> assertThat(list(DIGESTS)).as("the job fires again")
                                                                   .anySatisfy(
                                                                           digest -> assertThat(id(digest, "Ticket")).isEqualTo(ticket)),
                JOB_TIMEOUT.toSeconds());
        restAssuredExecutor.execute(() -> assertThat(processInstanceIds(list(PROCESS_INSTANCES))).as("a new review starts")
                                                                                                 .hasSize(3),
                JOB_TIMEOUT.toSeconds());
    }

    private List<Map<String, Object>> list(String path) {
        AtomicReference<String> body = new AtomicReference<>();
        restAssuredExecutor.execute(() -> body.set(given().when()
                                                          .get(path)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .asString()));
        return GSON.fromJson(body.get(), new TypeToken<List<Map<String, Object>>>() {}.getType());
    }

    private Set<String> appliedChangeSets() throws SQLException {
        Set<String> applied = new HashSet<>();
        try (Connection connection = dataSourcesManager.getSystemDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT ID FROM DATABASECHANGELOG")) {
            while (resultSet.next()) {
                applied.add(resultSet.getString(1));
            }
        }
        return applied;
    }

    /** The changesets of the system changelog the release under test ships that apply to PostgreSQL. */
    private static Set<String> declaredChangeSets() {
        JsonObject changelog = JsonParser.parseString(resource("db/changelog/dirigible-system.json"))
                                         .getAsJsonObject();
        Set<String> ids = new HashSet<>();
        for (JsonElement entry : changelog.getAsJsonArray("databaseChangeLog")) {
            JsonObject changeSet = entry.getAsJsonObject()
                                        .getAsJsonObject("changeSet");
            if (changeSet != null && appliesToPostgreSql(changeSet.get("dbms"))) {
                ids.add(changeSet.get("id")
                                 .getAsString());
            }
        }
        assertThat(ids).as("the system changelog declares changesets")
                       .isNotEmpty();
        return ids;
    }

    /**
     * Liquibase's {@code dbms} filter: absent or {@code all} applies everywhere, a list of names only
     * there, a list of {@code !name} exclusions everywhere else.
     */
    private static boolean appliesToPostgreSql(JsonElement dbms) {
        if (dbms == null || dbms.isJsonNull()) {
            return true;
        }
        Set<String> names = Set.of(dbms.getAsString()
                                       .replace(" ", "")
                                       .split(","));
        if (names.contains("all") || names.contains("postgresql")) {
            return true;
        }
        if (names.contains("!postgresql")) {
            return false;
        }
        return names.stream()
                    .allMatch(name -> name.startsWith("!"));
    }

    private static Set<String> processInstanceIds(List<Map<String, Object>> instances) {
        return instances.stream()
                        .filter(instance -> Objects.equals(PROCESS, instance.get("processDefinitionKey")))
                        .map(instance -> String.valueOf(instance.get("id")))
                        .collect(Collectors.toSet());
    }

    /** Gson reads every JSON number as a double. */
    private static int id(Map<String, Object> record) {
        return id(record, "Id");
    }

    private static int id(Map<String, Object> record, String property) {
        return ((Number) record.get(property)).intValue();
    }

    private static String resource(String name) {
        try (InputStream in = UpgradeFromPreviousReleaseIT.class.getClassLoader()
                                                                .getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("No resource [" + name + "] on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read [" + name + "]", ex);
        }
    }
}

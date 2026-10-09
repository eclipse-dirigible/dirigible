/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.bson.Document;
import org.eclipse.dirigible.mongodb.jdbc.Driver;
import org.eclipse.dirigible.mongodb.jdbc.MongoDBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * The MongoDB JDBC bridge against a real MongoDB (dirigible #7791). Nothing exercised
 * {@code database-mongodb-jdbc} before, which is how it could sit on an end-of-life driver
 * unnoticed; this drives it the way a caller does - through {@link Driver}, a {@link Connection}
 * and a {@link Statement} - so what is covered is the part the module owns: the connection-string
 * parsing, the find translation and the ResultSet over BSON.
 *
 * <p>
 * It lives here rather than next to the bridge on purpose: {@code modules/} is on JUnit 4, where
 * the {@code testcontainers} tag the root pom excludes by default is read as a JUnit 4 category and
 * therefore excludes nothing (dirigible #7215), so a container test there would run on every build
 * and demand Docker.
 */
@Tag("testcontainers")
@DisabledOnOs(OS.WINDOWS)
@Testcontainers
class MongoDBJdbcBridgeTest {

    private static final String DB = "testdb";

    private static final String COLLECTION = "people";

    @Container
    static GenericContainer<?> mongo = new GenericContainer<>(DockerImageName.parse("mongo:7.0")).withExposedPorts(27017);

    private static String connectionString() {
        return "mongodb://" + mongo.getHost() + ":" + mongo.getFirstMappedPort();
    }

    /** The bridge strips the "jdbc:" prefix and parses the rest as a connection string. */
    private static String jdbcUrl() {
        return "jdbc:" + connectionString() + "/" + DB;
    }

    @BeforeEach
    void seed() {
        try (MongoClient client = MongoClients.create(connectionString())) {
            client.getDatabase(DB)
                  .getCollection(COLLECTION)
                  .drop();
            client.getDatabase(DB)
                  .getCollection(COLLECTION)
                  .insertMany(List.of(new Document("name", "Ada").append("city", "London"),
                          new Document("name", "Grace").append("city", "New York")));
        }
    }

    @Test
    void the_driver_accepts_a_mongodb_url_and_connects() throws Exception {
        Driver driver = new Driver();
        assertTrue(driver.acceptsURL(jdbcUrl()), "the bridge must claim jdbc:mongodb URLs");
        assertFalse(driver.acceptsURL("jdbc:postgresql://localhost/x"), "and claim nothing else");

        try (Connection connection = driver.connect(jdbcUrl(), new Properties())) {
            assertFalse(connection.isClosed());
            // The database name comes out of the connection string - the part the 3.12
            // MongoClientURI used to do and ConnectionString does now. It is read back through
            // getMongoDatabase() and NOT through getCatalog(), which this bridge has always
            // answered with null (on 3.12 too) - asserting the name there tested the JDBC
            // method rather than the parsing this change is about.
            assertEquals(DB, ((MongoDBConnection) connection).getMongoDatabase()
                                                             .getName());
        }
    }

    @Test
    void a_find_query_returns_the_documents_as_a_result_set() throws Exception {
        try (Connection connection = new Driver().connect(jdbcUrl(), new Properties());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("{ \"find\": \"" + COLLECTION + "\" }")) {

            int rows = 0;
            boolean sawAda = false;
            while (resultSet.next()) {
                rows++;
                if ("Ada".equals(resultSet.getString("name"))) {
                    sawAda = true;
                    assertEquals("London", resultSet.getString("city"));
                }
            }
            assertEquals(2, rows, "both seeded documents must come back through the bridge");
            assertTrue(sawAda, "a field of a document must be readable by name");
        }
    }

    @Test
    void a_filtered_find_narrows_the_result_set() throws Exception {
        try (Connection connection = new Driver().connect(jdbcUrl(), new Properties());
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("{ \"find\": \"" + COLLECTION + "\", \"filter\": { \"name\": \"Grace\" } }")) {

            assertTrue(resultSet.next(), "the filter must match the seeded document");
            assertEquals("New York", resultSet.getString("city"));
            assertFalse(resultSet.next(), "and only that one");
        }
    }
}

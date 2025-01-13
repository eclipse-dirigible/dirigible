/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The Class CheckArtefactUtils.
 */
public class CheckArtefactUtils {

    /**
     * Checks if is artefact created.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isArtefactCreated(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = '/test/test.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String module = rs.getString("EXTENSION_MODULE");
                String phase = rs.getString("ARTEFACT_PHASE");
                String status = rs.getString("ARTEFACT_STATUS");
                assertEquals("/test/test", module);
                assertEquals("CREATE", phase);
                assertEquals("CREATED", status);
            } else {
                fail("No extension has been added");
            }
        }
    }

    /**
     * Checks if is artefact not created.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isArtefactNotCreated(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = '/test/test_broken.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                fail("Extension has been added, but it should not");
            }
        }
    }

    /**
     * Checks if is artefact recovered.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isArtefactRecovered(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = '/test/test_broken.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String module = rs.getString("EXTENSION_MODULE");
                String phase = rs.getString("ARTEFACT_PHASE");
                String status = rs.getString("ARTEFACT_STATUS");
                assertEquals("/test/test_broken", module);
                assertEquals("UPDATE", phase);
                assertEquals("UPDATED", status);
            } else {
                fail("No extension has been added");
            }
        }
    }

    /**
     * Checks if is artefact updated.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isArtefactUpdated(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = '/test/test.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String module = rs.getString("EXTENSION_MODULE");
                String phase = rs.getString("ARTEFACT_PHASE");
                String status = rs.getString("ARTEFACT_STATUS");
                assertEquals("/test/test_modified", module);
                assertEquals("UPDATE", phase);
                assertEquals("UPDATED", status);
            } else {
                fail("No extension has been added");
            }
        }
    }

    /**
     * Checks if is artefact for deletion created.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isArtefactForDeletionCreated(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = '/test/test_deleted.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String module = rs.getString("EXTENSION_MODULE");
                String phase = rs.getString("ARTEFACT_PHASE");
                String status = rs.getString("ARTEFACT_STATUS");
                assertEquals("/test/test_deleted", module);
                assertEquals("CREATE", phase);
                assertEquals("CREATED", status);
            } else {
                fail("No extension has been added");
            }
        }
    }

    /**
     * Checks if is artefact deleted.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isArtefactDeleted(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = '/test/test_deleted.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                fail("Extension has been added, but it should not");
            }
        }
    }

}

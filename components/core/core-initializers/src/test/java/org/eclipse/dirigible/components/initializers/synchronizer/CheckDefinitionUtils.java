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
 * The Class CheckDefinitionUtils.
 */
public class CheckDefinitionUtils {

    /**
     * Checks if is definition created.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isDefinitionParsed(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_DEFINITIONS WHERE DEFINITION_LOCATION = '/test/test.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String name = rs.getString("DEFINITION_NAME");
                String state = rs.getString("DEFINITION_STATE");
                assertEquals("test", name);
                assertEquals("PARSED", state);
            } else {
                fail("No definition has been added");
            }
        }
    }

    /**
     * Checks if is definition broken.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isDefinitionBroken(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_DEFINITIONS WHERE DEFINITION_LOCATION = '/test/test_broken.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String name = rs.getString("DEFINITION_NAME");
                String state = rs.getString("DEFINITION_STATE");
                assertEquals("test_broken", name);
                assertEquals("BROKEN", state);
            } else {
                fail("No definition has been added");
            }
        }
    }

    /**
     * Checks if is definition recovered.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isDefinitionRecovered(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_DEFINITIONS WHERE DEFINITION_LOCATION = '/test/test_broken.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String name = rs.getString("DEFINITION_NAME");
                String state = rs.getString("DEFINITION_STATE");
                assertEquals("test_broken", name);
                assertEquals("PARSED", state);
            } else {
                fail("No definition has been added");
            }
        }
    }

    /**
     * Checks if is definition for deletion exists.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isDefinitionForDeletionExists(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_DEFINITIONS WHERE DEFINITION_LOCATION = '/test/test_deleted.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String name = rs.getString("DEFINITION_NAME");
                String state = rs.getString("DEFINITION_STATE");
                assertEquals("test_deleted", name);
                assertEquals("PARSED", state);
            } else {
                fail("No definition has been added");
            }
        }
    }

    /**
     * Checks if is definition deleted.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public static void isDefinitionDeleted(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT * FROM DIRIGIBLE_DEFINITIONS WHERE DEFINITION_LOCATION = '/test/test_deleted.extension'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String name = rs.getString("DEFINITION_NAME");
                String state = rs.getString("DEFINITION_STATE");
                assertEquals("test_deleted", name);
                assertEquals("DELETED", state);
            } else {
                fail("No definition has been added");
            }
        }
    }

}

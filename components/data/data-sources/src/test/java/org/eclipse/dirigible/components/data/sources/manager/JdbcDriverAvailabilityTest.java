/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JdbcDriverAvailabilityTest {

    @Test
    void aDriverOnTheClasspathPasses() {
        assertThatCode(() -> JdbcDriverAvailability.requireDriver("DefaultDB", "org.h2.Driver")).doesNotThrowAnyException();
        assertThatCode(() -> JdbcDriverAvailability.requireDriver("DefaultDB", " org.h2.Driver ")).doesNotThrowAnyException();
    }

    @Test
    void aBlankDriverIsLeftToHikari() {
        assertThatCode(() -> JdbcDriverAvailability.requireDriver("DefaultDB", null)).doesNotThrowAnyException();
        assertThatCode(() -> JdbcDriverAvailability.requireDriver("DefaultDB", " ")).doesNotThrowAnyException();
    }

    @Test
    void theUnbundledDriversAreNotOnTheClasspath() {
        assertThatThrownBy(() -> JdbcDriverAvailability.requireDriver("ReportsDB",
                "com.mysql.cj.jdbc.Driver")).isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("[com.mysql.cj.jdbc.Driver]")
                                            .hasMessageContaining("[ReportsDB]")
                                            .hasMessageContaining("the MySQL JDBC driver (com.mysql:mysql-connector-j)")
                                            .hasMessageContaining("\"scope\": \"platform\"")
                                            .hasMessageContaining("/modules");
        assertThatThrownBy(() -> JdbcDriverAvailability.requireDriver("DefaultDB", "org.mariadb.jdbc.Driver")).hasMessageContaining(
                "org.mariadb.jdbc:mariadb-java-client");
        assertThatThrownBy(() -> JdbcDriverAvailability.requireDriver("DefaultDB",
                "com.microsoft.sqlserver.jdbc.SQLServerDriver")).hasMessageContaining("com.microsoft.sqlserver:mssql-jdbc");
        assertThatThrownBy(() -> JdbcDriverAvailability.requireDriver("DefaultDB", "com.sap.db.jdbc.Driver")).hasMessageContaining(
                "com.sap.cloud.db.jdbc:ngdbc");
    }

    @Test
    void anUnknownDriverNamesTheClass() {
        assertThat(JdbcDriverAvailability.missingDriverMessage("X", "com.example.Driver")).contains("[com.example.Driver]")
                                                                                          .contains("the jar that provides it");
    }
}

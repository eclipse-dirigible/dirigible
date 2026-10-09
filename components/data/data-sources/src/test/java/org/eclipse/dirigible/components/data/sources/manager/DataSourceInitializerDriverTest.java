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

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourceInitializerDriverTest {

    @Test
    void driverOnTheClasspathIsSet() {
        HikariConfig config = new HikariConfig();

        DataSourceInitializer.setDriverClassName(config, "DefaultDB", "org.h2.Driver");

        assertEquals("org.h2.Driver", config.getDriverClassName());
    }

    @Test
    void driverNotOnTheClasspathFailsNamingDriverAndDataSource() {
        HikariConfig config = new HikariConfig();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> DataSourceInitializer.setDriverClassName(config, "SnowflakeDB", "net.snowflake.client.jdbc.SnowflakeDriver"));

        assertTrue(ex.getMessage()
                     .contains("[net.snowflake.client.jdbc.SnowflakeDriver]"),
                ex.getMessage());
        assertTrue(ex.getMessage()
                     .contains("[SnowflakeDB]"),
                ex.getMessage());
        assertTrue(ex.getMessage()
                     .contains("not on the classpath"),
                ex.getMessage());
    }

}

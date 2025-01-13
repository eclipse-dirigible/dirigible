/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.source.snowpark;

import com.zaxxer.hikari.HikariConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.database.DatabaseConfigurator;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
public class SnowflakeDatabaseConfigurator implements DatabaseConfigurator {

    private static final String TOKEN_FILE_PATH = "/snowflake/session/token";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeDatabaseConfigurator.class);

    @Override
    public boolean isApplicable(DatabaseSystem databaseSystem) {
        return databaseSystem.isSnowflake();
    }

    @Override
    public void apply(HikariConfig config) {
        config.setConnectionTestQuery("SELECT 1"); // connection validation query
        config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(5)); // validation execution interval, must be bigger than idle timeout
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(9)); // recreate connections after specified time
        config.setLeakDetectionThreshold(TimeUnit.MINUTES.toMillis(5));

        config.addDataSourceProperty("CLIENT_SESSION_KEEP_ALIVE", true);
        config.addDataSourceProperty("CLIENT_SESSION_KEEP_ALIVE_HEARTBEAT_FREQUENCY", 900);

        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_WAREHOUSE", "warehouse", config);
        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_ACCOUNT", "account", config);
        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_DATABASE", "db", config);
        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_SCHEMA", "schema", config);

        String url;
        if (hasTokenFile()) {
            logger.info("There IS token file. OAuth will be added to [{}]", config);

            config.setUsername(null);
            config.setPassword(null);
            config.addDataSourceProperty("authenticator", "OAUTH");
            config.addDataSourceProperty("token", loadTokenFile());
            url = "jdbc:snowflake://" + Configuration.get("SNOWFLAKE_HOST") + ":" + Configuration.get("SNOWFLAKE_PORT");
        } else {
            logger.info("There is NO token file. User/password will be added to [{}]", config);

            addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_ROLE", "role", config);
            addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_USERNAME", "user", config);
            addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_PASSWORD", "password", config);

            url = Configuration.get("SNOWFLAKE_URL", config.getJdbcUrl());
        }
        logger.info("Built url [{}]", url);
        config.addDataSourceProperty("url", url);
        config.setJdbcUrl(url);
    }

    private static String loadTokenFile() {
        try {
            return new String(Files.readAllBytes(Paths.get(TOKEN_FILE_PATH)));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load token file from path " + TOKEN_FILE_PATH, ex);
        }
    }

    private static boolean hasTokenFile() {
        return Files.exists(Paths.get(TOKEN_FILE_PATH));
    }

    private void addDataSourcePropertyIfConfigAvailable(String configName, String propertyName, HikariConfig config) {
        String value = Configuration.get(configName);
        if (StringUtils.isNotBlank(value)) {
            logger.debug("Setting property [{}] from config [{}]", propertyName, configName);
            config.addDataSourceProperty(propertyName, value);
        } else {
            logger.debug("Will NOT set property [{}] since config [{}] value is [{}]", propertyName, configName, value);
        }
    }

}

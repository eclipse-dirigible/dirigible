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
import java.util.Objects;
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
        setCommonConfigurations(config);

        boolean registeredUsernameAndPass = StringUtils.isNotBlank(config.getUsername()) && StringUtils.isNotBlank(config.getPassword());

        if (registeredUsernameAndPass && userAndPassAreNotDummyValues(config)) {
            logger.info("There ARE registered username and pass for config [{}] and they  will be used.", config);
            config.addDataSourceProperty("user", config.getUsername());
            config.addDataSourceProperty("password", config.getPassword());

        } else {
            configureOAuth(config);
        }
    }

    private void setCommonConfigurations(HikariConfig config) {
        config.setConnectionTestQuery("SELECT 1"); // connection validation query
        config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(5)); // validation execution interval, must be bigger than idle timeout
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(9)); // recreate connections after specified time
        config.setLeakDetectionThreshold(TimeUnit.MINUTES.toMillis(5));

        config.addDataSourceProperty("CLIENT_SESSION_KEEP_ALIVE", true);
        config.addDataSourceProperty("CLIENT_SESSION_KEEP_ALIVE_HEARTBEAT_FREQUENCY", 900);
    }

    private void configureOAuth(HikariConfig config) {
        if (!hasTokenFile()) {
            throw new IllegalStateException("There in no username and/or password (or both are dummy values) for provided config [" + config
                    + "]. Assuming it should use oauth token but there is no token file at " + TOKEN_FILE_PATH);
        }

        logger.info("Missing username and/or password for config [{}]. OAuth token will be used.", config);

        config.setUsername(null);
        config.setPassword(null);
        config.addDataSourceProperty("authenticator", "OAUTH");
        config.addDataSourceProperty("token", loadTokenFile());

        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_WAREHOUSE", "warehouse", config);

        // automatically populated by Snowflake unless explicitly set
        // https://docs.snowflake.com/en/developer-guide/snowpark-container-services/additional-considerations-services-jobs
        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_ACCOUNT", "account", config);
        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_DATABASE", "db", config);
        addDataSourcePropertyIfConfigAvailable("SNOWFLAKE_SCHEMA", "schema", config);

        String url = "jdbc:snowflake://" + Configuration.get("SNOWFLAKE_HOST") + ":" + Configuration.get("SNOWFLAKE_PORT");

        logger.info("Will be used url [{}] for config [{}]", url, config);
        config.addDataSourceProperty("url", url);
        config.setJdbcUrl(url);
    }

    private String loadTokenFile() {
        try {
            return new String(Files.readAllBytes(Paths.get(TOKEN_FILE_PATH)));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load token file from path " + TOKEN_FILE_PATH, ex);
        }
    }

    private boolean hasTokenFile() {
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

    private boolean userAndPassAreNotDummyValues(HikariConfig config) {
        return isNotDummyValue(config.getUsername()) && isNotDummyValue(config.getPassword());
    }

    /**
     * Note: needed for backward compatibility with Snowflake native applications until they are updated
     *
     * @param value
     * @return
     */
    private boolean isNotDummyValue(String value) {
        return !Objects.equals(value, "not-used-in-snowpark-scenario");
    }

}

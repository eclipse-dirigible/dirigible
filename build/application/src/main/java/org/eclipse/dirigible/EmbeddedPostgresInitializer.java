/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;

@Order(ApplicationReadyEventListeners.EMBEDDED_POSTGRES_INITIALIZER)
@Component
class EmbeddedPostgresInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedPostgresInitializer.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // TODO: init DB only when there are not user provided configurations for DefaultDB
        try {
            LOGGER.info("Starting embedded postgres instance...");
            EmbeddedPostgres.Builder postgresBuilder = EmbeddedPostgres.builder();
            // .setDataDirectory(Files.createDirectories(tf.resolve("data-dir-parent")
            // .resolve("data-dir")))
            long startTime = System.currentTimeMillis();

            // don't use the default postgres port 5432 to prevent conflicts
            postgresBuilder.setPort(5430);
            EmbeddedPostgres postgres = postgresBuilder.start();

            LOGGER.info("Started embedded postgres {} for [{}]ms", postgres, (System.currentTimeMillis() - startTime));

            DataSource dataSource = postgres.getPostgresDatabase();
            LOGGER.info("Embedded postgres data source: {}", dataSource);

        } catch (IOException ex) {
            throw new RuntimeException("Failed to create embedded postgres instance", ex);
        }

    }

}

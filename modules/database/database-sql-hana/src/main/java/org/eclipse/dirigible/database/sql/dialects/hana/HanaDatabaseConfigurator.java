/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import com.zaxxer.hikari.HikariConfig;
import org.eclipse.dirigible.components.database.DatabaseConfigurator;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
class HanaDatabaseConfigurator implements DatabaseConfigurator {

    @Override
    public boolean isApplicable(DatabaseSystem databaseSystem) {
        return databaseSystem.isHANA();
    }

    @Override
    public void apply(HikariConfig config) {
        config.setConnectionTestQuery("SELECT 1 FROM DUMMY"); // connection validation query
        config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(5)); // validation execution interval, must be bigger than idle timeout
    }

}

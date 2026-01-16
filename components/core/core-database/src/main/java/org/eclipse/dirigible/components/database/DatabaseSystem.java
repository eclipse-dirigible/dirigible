/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

public enum DatabaseSystem {
    // when adding or changing enum values do NOT forget to
    // update the JavaScript API in the connection class located at:
    // dirigible/modules/src/db/database.ts
    UNKNOWN, DERBY, POSTGRESQL, H2, MARIADB, HANA, SNOWFLAKE, MYSQL, MONGODB, MSSQL;

    public boolean isH2() {
        return isOfType(H2);
    }

    public boolean isOfType(DatabaseSystem databaseSystem) {
        return this == databaseSystem;
    }

    public boolean isSnowflake() {
        return isOfType(SNOWFLAKE);
    }

    public boolean isMSSQL() {
        return isOfType(MSSQL);
    }

    public boolean isHANA() {
        return isOfType(HANA);
    }

    public boolean isPostgreSQL() {
        return isOfType(POSTGRESQL);
    }

    public boolean isMariaDB() {
        return isOfType(MARIADB);
    }

    public boolean isMySQL() {
        return isOfType(MYSQL);
    }

    public boolean isUnknown() {
        return isOfType(UNKNOWN);
    }

    public boolean isMongoDB() {
        return isOfType(MONGODB);
    }

    public boolean isDerby() {
        return isOfType(DERBY);
    }
}

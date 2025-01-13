/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.util.Objects;

class InUseConnectionEntry {

    private final Connection connection;
    private final long borrowedAt;

    InUseConnectionEntry(Connection connection) {
        this.connection = connection;
        this.borrowedAt = System.currentTimeMillis();
    }

    public Connection getConnection() {
        return connection;
    }

    public long getBorrowedAt() {
        return borrowedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InUseConnectionEntry that = (InUseConnectionEntry) o;
        return Objects.equals(connection, that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(connection);
    }

    @Override
    public String toString() {
        return "InUseConnectionEntry{" + "connection=" + connection + ", borrowedAt=" + borrowedAt + '}';
    }
}

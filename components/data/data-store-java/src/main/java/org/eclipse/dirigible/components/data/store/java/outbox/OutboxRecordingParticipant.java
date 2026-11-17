/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.outbox;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Something that records more rows with the events a write records - in the tenant's schema, on the
 * write's own connection, so they commit or roll back with the write and its events.
 *
 * <p>
 * Called by {@link EventOutbox}: {@link #prepare()} before the write transaction opens (where a
 * participant may create its tables, since no DDL may run inside the business transaction), and
 * {@link #recorded} inside it, right after the outbox rows were inserted. A participant that throws
 * from {@link #recorded} fails the write, exactly as a failed outbox insert does.
 */
public interface OutboxRecordingParticipant {

    /**
     * Prepares the current tenant for a write that will record events. Runs outside any business
     * transaction.
     */
    void prepare();

    /**
     * Records whatever the participant derives from the given events, on the given connection.
     *
     * @param connection the write transaction's connection
     * @param entries the events just recorded, never empty
     * @throws SQLException if a row cannot be recorded, which fails the write
     */
    void recorded(Connection connection, List<OutboxEntry> entries) throws SQLException;
}

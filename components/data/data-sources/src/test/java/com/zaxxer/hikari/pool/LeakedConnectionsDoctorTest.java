/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package com.zaxxer.hikari.pool;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeakedConnectionsDoctorTest {

    @Test
    void checkSurvivesConnectionBorrowedOnAnotherThreadWhileItRuns() throws SQLException {
        Connection borrowedDuringCheck = mock(Connection.class);
        Answer<Boolean> borrowOnAnotherThread = invocation -> {
            // a request thread borrows a connection while the check thread is iterating the registered ones
            Thread borrower = new Thread(() -> LeakedConnectionsDoctor.registerConnection(borrowedDuringCheck));
            borrower.start();
            borrower.join();
            return false;
        };
        // two registered connections, so that the iteration continues after the borrow
        List<Connection> inspected = List.of(mock(Connection.class), mock(Connection.class));
        for (Connection connection : inspected) {
            when(connection.isClosed()).thenAnswer(borrowOnAnotherThread);
            LeakedConnectionsDoctor.registerConnection(connection);
        }

        assertDoesNotThrow(LeakedConnectionsDoctor::closeLeakedConnections);

        LeakedConnectionsDoctor.closeLeakedConnections();
        for (Connection connection : inspected) {
            assertFalse(LeakedConnectionsDoctor.isRegistered(connection));
        }
        assertFalse(LeakedConnectionsDoctor.isRegistered(borrowedDuringCheck));
    }

    @Test
    void checkForgetsClosedConnection() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.isClosed()).thenReturn(true);
        LeakedConnectionsDoctor.registerConnection(connection);

        LeakedConnectionsDoctor.closeLeakedConnections();

        assertFalse(LeakedConnectionsDoctor.isRegistered(connection));
    }

    @Test
    void unregisterForgetsConnectionWithoutWaitingForCheck() {
        Connection connection = mock(Connection.class);
        LeakedConnectionsDoctor.registerConnection(connection);
        assertTrue(LeakedConnectionsDoctor.isRegistered(connection));

        LeakedConnectionsDoctor.unregisterConnection(connection);

        assertFalse(LeakedConnectionsDoctor.isRegistered(connection));
    }
}

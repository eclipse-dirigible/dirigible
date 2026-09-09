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

import com.zaxxer.hikari.pool.LeakedConnectionsDoctor;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class DirigibleConnectionImplTest {

    private final Connection connection = mock(Connection.class);
    private final DirigibleConnectionImpl dirigibleConnection = new DirigibleConnectionImpl("TestDB", connection, DatabaseSystem.H2);

    @Test
    void closeUnregistersConnectionFromLeakedConnectionsDoctor() throws SQLException {
        try (MockedStatic<LeakedConnectionsDoctor> doctor = mockStatic(LeakedConnectionsDoctor.class)) {
            dirigibleConnection.close();

            verify(connection).close();
            doctor.verify(() -> LeakedConnectionsDoctor.unregisterConnection(connection));
        }
    }

    @Test
    void failedCloseLeavesConnectionRegisteredForLeakedConnectionsDoctor() throws SQLException {
        doThrow(new SQLException("close failed")).when(connection)
                                                 .close();

        try (MockedStatic<LeakedConnectionsDoctor> doctor = mockStatic(LeakedConnectionsDoctor.class)) {
            assertThrows(SQLException.class, dirigibleConnection::close);

            doctor.verifyNoInteractions();
        }
    }
}

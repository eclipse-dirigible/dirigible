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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * A durable publish decoupled from its write - the {@code -transitioned} announcement a workflow
 * step's status setter makes after the commit - records its participants' rows in the same short
 * transaction as its outbox entry, exactly as a write-attached event does. Without it a workflow
 * transition (submit, approve, reject) never created a webhook delivery (#8689). Against a real
 * (H2) outbox table.
 */
class EventOutboxDurablePublishParticipantsTest {

    private static final String TOPIC = "hr-Leave-Leave-transitioned";

    private String url;

    private EventOutbox outbox;

    private MockedStatic<MessagingFacade> messaging;

    private final List<String> published = new ArrayList<>();

    /** Whether the broker refuses, leaving the entry in the outbox for the test to read back. */
    private boolean brokerDown = true;

    /** A participant that records one side row per entry on the connection it is given. */
    private final RecordingParticipant participant = new RecordingParticipant();

    @BeforeEach
    void setUp() throws SQLException {
        url = "jdbc:h2:mem:outbox-durable-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> h2Connection());
        DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(dataSource);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(true);
        EventOutboxStore store = new EventOutboxStore(dataSourcesManager, tenantContext);
        outbox = new EventOutbox(store,
                new StaticListableBeanFactory(Map.of("participant", participant)).getBeanProvider(OutboxRecordingParticipant.class),
                tenantContext);
        try (Connection connection = h2Connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE SIDE_ROWS (EVENT_ID VARCHAR(64), EVENT_TOPIC VARCHAR(255))");
        }

        messaging = mockStatic(MessagingFacade.class);
        messaging.when(() -> MessagingFacade.sendToTopic(anyString(), anyString()))
                 .thenAnswer(invocation -> {
                     published.add(invocation.getArgument(0));
                     if (brokerDown) {
                         throw new IllegalStateException("broker down");
                     }
                     return null;
                 });
    }

    @AfterEach
    void tearDown() {
        messaging.close();
    }

    @Test
    void aDurablePublishRecordsItsParticipantsRowsWithItsOutboxEntry() throws SQLException {
        outbox.publishToTopic(TOPIC, "{\"Id\":7}");

        List<String> outboxIds = column("SELECT \"EVENT_ID\" FROM \"DIRIGIBLE_EVENT_OUTBOX\"");
        assertThat(outboxIds).hasSize(1);
        assertThat(participant.entries).containsExactly(new OutboxEntry(outboxIds.get(0), TOPIC, "{\"Id\":7}"));
        assertThat(participant.sameConnectionAsTransaction).containsExactly(true);
        assertThat(column("SELECT EVENT_ID FROM SIDE_ROWS")).containsExactly(outboxIds.get(0));
    }

    @Test
    void aFailingParticipantRecordsNeitherRowAndTheEventStillGoesOutDirectly() throws SQLException {
        participant.failWith = new SQLException("no room");
        brokerDown = false;

        outbox.publishToTopic(TOPIC, "{}");

        assertThat(column("SELECT \"EVENT_ID\" FROM \"DIRIGIBLE_EVENT_OUTBOX\"")).isEmpty();
        assertThat(column("SELECT EVENT_ID FROM SIDE_ROWS")).isEmpty();
        // The caller's work has committed: the documented fallback is the direct, at-most-once publish.
        assertThat(published).containsExactly(TOPIC);
    }

    private List<String> column(String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = h2Connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
        }
        return values;
    }

    private static final class RecordingParticipant implements OutboxRecordingParticipant {

        private final List<OutboxEntry> entries = new ArrayList<>();

        private final List<Boolean> sameConnectionAsTransaction = new ArrayList<>();

        private SQLException failWith;

        @Override
        public void prepare() {}

        @Override
        public void recorded(Connection connection, List<OutboxEntry> recorded) throws SQLException {
            entries.addAll(recorded);
            // Inside the insert's transaction: the outbox row is visible here, uncommitted.
            sameConnectionAsTransaction.add(!connection.getAutoCommit());
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO SIDE_ROWS VALUES (?, ?)")) {
                for (OutboxEntry entry : recorded) {
                    statement.setString(1, entry.id());
                    statement.setString(2, entry.topic());
                    statement.executeUpdate();
                }
            }
            if (failWith != null) {
                throw failWith;
            }
        }
    }

    /**
     * A plain H2 connection, typed as the platform's own so the SQL dialect is resolved as in
     * production.
     */
    private DirigibleConnection h2Connection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        return (DirigibleConnection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DirigibleConnection.class},
                (proxy, method, args) -> {
                    if ("getDatabaseSystem".equals(method.getName())) {
                        return DatabaseSystem.H2;
                    }
                    if ("isOfType".equals(method.getName())) {
                        return args[0] == DatabaseSystem.H2;
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.store.java.repository.DomainEvent;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * An event remembers the user whose write raised it, and both deliveries - the in-process one after
 * the commit and the relay's redelivery - publish it as that user, whatever thread they run on
 * (#8228). Against a real (H2) outbox table, including one an earlier version created without the
 * user column.
 */
class EventOutboxOriginatingUserTest {

    private static final String TOPIC = "outbox-user-test";

    private String url;

    private EventOutboxStore store;

    private EventOutbox outbox;

    private Session session;

    private MockedStatic<MessagingFacade> messaging;

    /** The originating user bound while each message was handed to the broker, in order. */
    private final List<String> publishedAs = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        url = "jdbc:h2:mem:outbox-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> h2Connection());
        DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(dataSource);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(true);
        store = new EventOutboxStore(dataSourcesManager, tenantContext);
        outbox = new EventOutbox(store);

        session = mock(Session.class);
        doAnswer(invocation -> {
            try (Connection connection = h2Connection()) {
                ((Work) invocation.getArgument(0)).execute(connection);
            }
            return null;
        }).when(session)
          .doWork(any());

        messaging = mockStatic(MessagingFacade.class);
        messaging.when(() -> MessagingFacade.sendToTopic(anyString(), anyString()))
                 .thenAnswer(invocation -> publishedAs.add(OriginatingUser.get()
                                                                          .orElse(null)));
        // Due at once, so the relay can be asked to redeliver what a write has just recorded.
        DirigibleConfig.EVENT_OUTBOX_RELAY_GRACE_SECONDS.setStringValue("0");
    }

    @AfterEach
    void tearDown() {
        messaging.close();
        DirigibleConfig.EVENT_OUTBOX_RELAY_GRACE_SECONDS.setStringValue(DirigibleConfig.EVENT_OUTBOX_RELAY_GRACE_SECONDS.getDefaultValue());
    }

    @Test
    void the_in_process_delivery_publishes_as_the_user_whose_write_recorded_the_event() {
        outbox.prepare();
        EventOutbox.Batch batch;
        try (OriginatingUser.Binding binding = OriginatingUser.bind("alice")) {
            batch = outbox.record(session, List.of(new DomainEvent(TOPIC, "{}")));
        }

        // Dispatched off the writer's binding: the user comes from the entry, not from the thread.
        batch.dispatch();

        assertThat(publishedAs).containsExactly("alice");
    }

    @Test
    void the_relay_redelivers_as_the_user_whose_write_recorded_the_event() throws SQLException {
        outbox.prepare();
        try (OriginatingUser.Binding binding = OriginatingUser.bind("alice")) {
            outbox.record(session, List.of(new DomainEvent(TOPIC, "{}")));
        }

        // The relay job's thread has no user of its own.
        outbox.relay();

        assertThat(publishedAs).containsExactly("alice");
        assertThat(count()).isZero();
    }

    @Test
    void an_event_raised_without_a_user_is_published_without_one() throws SQLException {
        outbox.prepare();
        outbox.record(session, List.of(new DomainEvent(TOPIC, "{}")));

        assertThat(storedUsers()).containsExactly((String) null);
        outbox.relay();

        assertThat(publishedAs).containsExactly((String) null);
    }

    @Test
    void a_durable_publish_records_the_current_user_too() {
        outbox.prepare();
        try (OriginatingUser.Binding binding = OriginatingUser.bind("alice")) {
            outbox.publishToTopic(TOPIC, "{}");
        }

        assertThat(publishedAs).containsExactly("alice");
    }

    /**
     * A table an earlier version created has no user column: preparing it adds the column, and the
     * relay - possibly the first on the node to touch the table - does so too, delivering the old
     * entries without a user, as they always were.
     */
    @Test
    void a_table_created_before_the_user_column_is_migrated() throws SQLException {
        try (Connection connection = h2Connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE \"DIRIGIBLE_EVENT_OUTBOX\" (\"EVENT_ID\" VARCHAR(36) PRIMARY KEY, \"EVENT_TOPIC\" VARCHAR(255),"
                    + " \"EVENT_PAYLOAD\" CLOB, \"EVENT_CREATED_AT\" TIMESTAMP, \"EVENT_ATTEMPTS\" INTEGER,"
                    + " \"EVENT_NEXT_ATTEMPT_AT\" TIMESTAMP, \"EVENT_ERROR\" VARCHAR(2000))");
            statement.execute("INSERT INTO \"DIRIGIBLE_EVENT_OUTBOX\" VALUES ('old', '" + TOPIC
                    + "', '{}', CURRENT_TIMESTAMP, 0, DATEADD('MINUTE', -5, CURRENT_TIMESTAMP), NULL)");
        }

        outbox.relay();
        assertThat(publishedAs).containsExactly((String) null);

        try (OriginatingUser.Binding binding = OriginatingUser.bind("alice")) {
            outbox.record(session, List.of(new DomainEvent(TOPIC, "{}")));
        }
        assertThat(storedUsers()).containsExactly("alice");
    }

    private int count() throws SQLException {
        try (Connection connection = h2Connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM \"DIRIGIBLE_EVENT_OUTBOX\"")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private List<String> storedUsers() throws SQLException {
        List<String> users = new ArrayList<>();
        try (Connection connection = h2Connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT \"EVENT_USER\" FROM \"DIRIGIBLE_EVENT_OUTBOX\"")) {
            while (resultSet.next()) {
                users.add(resultSet.getString(1));
            }
        }
        return users;
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

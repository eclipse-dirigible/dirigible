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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.store.java.repository.DomainEvent;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;


/**
 * A recording participant sees the events on the write's own connection, with their outbox ids, and
 * its failure fails the write - the way webhook deliveries ride the entity events (#8689).
 */
class EventOutboxParticipantsTest {

    private final EventOutboxStore store = mock(EventOutboxStore.class);
    private final OutboxRecordingParticipant participant = mock(OutboxRecordingParticipant.class);
    private final Connection connection = mock(Connection.class);
    private final Session session = mock(Session.class);

    private final EventOutbox outbox = new EventOutbox(store,
            new StaticListableBeanFactory(Map.of("participant", participant)).getBeanProvider(OutboxRecordingParticipant.class),
            mock(TenantContext.class));

    EventOutboxParticipantsTest() {
        doAnswer(invocation -> {
            ((Work) invocation.getArgument(0)).execute(connection);
            return null;
        }).when(session)
          .doWork(any());
    }

    @Test
    void theParticipantRecordsOnTheWritesConnectionWithTheOutboxIds() throws SQLException {
        outbox.record(session, List.of(new DomainEvent("hr-Leave-Leave", "{\"Id\":1}"), new DomainEvent("hr-Leave-Leave-rekeyed", "{}")));

        ArgumentCaptor<PendingEvent> recorded = ArgumentCaptor.forClass(PendingEvent.class);
        verify(store, times(2)).insert(eq(connection), recorded.capture(), any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OutboxEntry>> entries = ArgumentCaptor.forClass(List.class);
        verify(participant).recorded(eq(connection), entries.capture());
        assertThat(entries.getValue()).containsExactly(new OutboxEntry(recorded.getAllValues()
                                                                               .get(0)
                                                                               .id(),
                "hr-Leave-Leave", "{\"Id\":1}"),
                new OutboxEntry(recorded.getAllValues()
                                        .get(1)
                                        .id(),
                        "hr-Leave-Leave-rekeyed", "{}"));
    }

    @Test
    void aFailingParticipantFailsTheWrite() throws SQLException {
        doThrow(new SQLException("no room")).when(participant)
                                            .recorded(any(), any());
        Executable write = () -> outbox.record(session, List.of(new DomainEvent("hr-Leave-Leave", "{}")));
        assertThrows(SQLException.class, write);
    }

    @Test
    void prepareLetsTheParticipantPrepare() {
        outbox.prepare();
        verify(participant).prepare();
    }
}

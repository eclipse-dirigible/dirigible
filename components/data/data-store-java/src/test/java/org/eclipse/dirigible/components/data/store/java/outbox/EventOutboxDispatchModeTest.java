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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.TestMessaging;
import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.base.context.ThreadLanguage;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.store.java.repository.DomainEvent;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Where the outbox hands a committed event to the broker (#8923,
 * {@code DIRIGIBLE_EVENT_OUTBOX_DISPATCH}): on the writing thread, where the write waits for the
 * broker's answer, or on the outbox's own, where it does not. Against a real (H2) outbox table and
 * a producer that answers when the test lets it - the messages are sent from another thread, so the
 * producer is put behind the real {@code MessagingFacade}.
 */
class EventOutboxDispatchModeTest {

    private static final String TOPIC = "outbox-dispatch-test";

    private static final long WAIT_SECONDS = 10;

    private String url;

    private EventOutboxStore store;

    private TenantContext tenantContext;

    private Session session;

    private EventOutbox outbox;

    /**
     * The payloads the broker took, in order, with the user and the language bound while it took them.
     */
    private final List<String> published = Collections.synchronizedList(new ArrayList<>());

    private final List<String> publishedAs = Collections.synchronizedList(new ArrayList<>());

    private final List<String> publishedIn = Collections.synchronizedList(new ArrayList<>());

    /** Closed while the broker does not answer. */
    private CountDownLatch brokerAnswers = new CountDownLatch(0);

    /** Opens when a message has reached the broker. */
    private final CountDownLatch reachedBroker = new CountDownLatch(1);

    /** How long the broker takes to answer once it does. */
    private volatile long brokerTakesMillis;

    private final ListAppender<ILoggingEvent> log = new ListAppender<>();

    private final Logger logger = (Logger) LoggerFactory.getLogger(EventOutbox.class);

    @BeforeEach
    void setUp() throws Exception {
        url = "jdbc:h2:mem:outbox-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        DirigibleDataSource dataSource = mock(DirigibleDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> h2Connection());
        DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(dataSource);
        tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(true);
        store = new EventOutboxStore(dataSourcesManager, tenantContext);

        session = mock(Session.class);
        doAnswer(invocation -> {
            try (Connection connection = h2Connection()) {
                ((Work) invocation.getArgument(0)).execute(connection);
            }
            return null;
        }).when(session)
          .doWork(any());

        MessageProducer producer = mock(MessageProducer.class);
        doAnswer(invocation -> {
            reachedBroker.countDown();
            if (!brokerAnswers.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the broker of this test never answered");
            }
            TimeUnit.MILLISECONDS.sleep(brokerTakesMillis);
            publishedAs.add(OriginatingUser.get()
                                           .orElse(null));
            publishedIn.add(ThreadLanguage.get()
                                          .orElse(null));
            published.add(invocation.getArgument(1));
            return null;
        }).when(producer)
          .sendMessageToTopic(anyString(), anyString());
        TestMessaging.use(producer);

        log.start();
        logger.addAppender(log);
    }

    @AfterEach
    void tearDown() {
        brokerAnswers.countDown();
        if (outbox != null) {
            outbox.stop();
        }
        logger.detachAppender(log);
        TestMessaging.use(null);
        DirigibleConfig.EVENT_OUTBOX_DISPATCH.setStringValue(DirigibleConfig.EVENT_OUTBOX_DISPATCH.getDefaultValue());
        DirigibleConfig.EVENT_OUTBOX_RELAY_GRACE_SECONDS.setStringValue(DirigibleConfig.EVENT_OUTBOX_RELAY_GRACE_SECONDS.getDefaultValue());
    }

    @Test
    void unsetTheWritingThreadHandsTheEventOver() throws SQLException {
        outbox = outbox();
        outbox.prepare();

        outbox.record(session, List.of(new DomainEvent(TOPIC, "one")))
              .dispatch();

        // Nothing to wait for: the broker had answered when the dispatch returned.
        assertThat(published).containsExactly("one");
        assertThat(count()).isZero();
    }

    @Test
    void inTheBackgroundAWriteReturnsWhileTheBrokerDoesNotAnswer() throws Exception {
        outbox = outbox("background");
        outbox.prepare();
        brokerAnswers = new CountDownLatch(1);

        EventOutbox.Batch batch;
        try (OriginatingUser.Binding binding = OriginatingUser.bind("alice", "bg")) {
            batch = outbox.record(session, List.of(new DomainEvent(TOPIC, "one"), new DomainEvent(TOPIC, "two")));
        }
        batch.dispatch();

        // Back on the writing thread, the broker has the first event and has not answered.
        assertThat(reachedBroker.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(published).isEmpty();
        assertThat(count()).isEqualTo(2);

        brokerAnswers.countDown();
        awaitOutboxEmpty();
        assertThat(published).containsExactly("one", "two");
        // From the entries, not from the outbox's thread, which has neither.
        assertThat(publishedAs).containsExactly("alice", "alice");
        assertThat(publishedIn).containsExactly("bg", "bg");
    }

    @Test
    void inTheBackgroundADurablePublishReturnsWhileTheBrokerDoesNotAnswer() throws Exception {
        outbox = outbox("background");
        outbox.prepare();
        brokerAnswers = new CountDownLatch(1);

        outbox.publishToTopic(TOPIC, "announced");

        assertThat(reachedBroker.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(published).isEmpty();
        assertThat(count()).isEqualTo(1);

        brokerAnswers.countDown();
        awaitOutboxEmpty();
        assertThat(published).containsExactly("announced");
    }

    @Test
    void inTheBackgroundWhatWasNotSentIsTheRelays() throws Exception {
        // No grace period: an entry is the relay's from the moment it is written, as one is that
        // waited in the queue for longer than the grace period.
        DirigibleConfig.EVENT_OUTBOX_RELAY_GRACE_SECONDS.setStringValue("0");
        outbox = outbox("background");
        outbox.prepare();

        outbox.record(session, List.of(new DomainEvent(TOPIC, "one")))
              .dispatch();
        outbox.stop();

        assertThat(count()).isEqualTo(1);
        outbox.relay();

        assertThat(published).containsExactly("one");
        assertThat(count()).isZero();
    }

    @Test
    void theModeIsNotCaseSensitive() {
        DirigibleConfig.EVENT_OUTBOX_DISPATCH.setStringValue(" Background ");
        assertThat(EventOutbox.backgroundDispatch()).isTrue();

        DirigibleConfig.EVENT_OUTBOX_DISPATCH.setStringValue("CALLER");
        assertThat(EventOutbox.backgroundDispatch()).isFalse();
    }

    @Test
    void unsetTheModeIsTheWritingThreads() {
        assertThat(DirigibleConfig.EVENT_OUTBOX_DISPATCH.getDefaultValue()).isEqualTo("caller");
        assertThat(EventOutbox.backgroundDispatch()).isFalse();
    }

    @Test
    void anUnknownModeStopsTheStart() {
        DirigibleConfig.EVENT_OUTBOX_DISPATCH.setStringValue("async");

        assertThatThrownBy(() -> outbox()).isInstanceOf(IllegalArgumentException.class)
                                          .hasMessageContaining("DIRIGIBLE_EVENT_OUTBOX_DISPATCH")
                                          .hasMessageContaining("[async]")
                                          .hasMessageContaining("caller")
                                          .hasMessageContaining("background");
    }

    @Test
    void aBrokerThatTakesLongToAnswerIsReported() {
        outbox = outbox();
        outbox.prepare();
        brokerTakesMillis = EventOutbox.SLOW_SEND_MILLIS + 100;

        outbox.record(session, List.of(new DomainEvent(TOPIC, "slow")))
              .dispatch();

        assertThat(published).containsExactly("slow");
        assertThat(warnings()).anySatisfy(message -> assertThat(message).contains("The broker took")
                                                                        .contains(TOPIC));
    }

    @Test
    void aBrokerThatAnswersAtOnceIsNotReported() {
        outbox = outbox();
        outbox.prepare();

        outbox.record(session, List.of(new DomainEvent(TOPIC, "fast")))
              .dispatch();

        assertThat(published).containsExactly("fast");
        assertThat(warnings()).isEmpty();
    }

    private EventOutbox outbox(String mode) {
        DirigibleConfig.EVENT_OUTBOX_DISPATCH.setStringValue(mode);
        return outbox();
    }

    private EventOutbox outbox() {
        return new EventOutbox(store, new StaticListableBeanFactory().getBeanProvider(OutboxRecordingParticipant.class), tenantContext);
    }

    private List<String> warnings() {
        return log.list.stream()
                       .filter(event -> event.getLevel() == Level.WARN)
                       .map(ILoggingEvent::getFormattedMessage)
                       .toList();
    }

    private void awaitOutboxEmpty() throws SQLException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (count() > 0 && System.nanoTime() < deadline) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertThat(count()).isZero();
    }

    private int count() throws SQLException {
        try (Connection connection = h2Connection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM \"DIRIGIBLE_EVENT_OUTBOX\"")) {
            resultSet.next();
            return resultSet.getInt(1);
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

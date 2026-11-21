/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.base.context.ThreadLanguage;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.components.listeners.config.ListenerConnectionMode;
import org.eclipse.dirigible.components.listeners.service.DestinationNameManager;
import org.eclipse.dirigible.components.listeners.service.TenantPropertyManager;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * {@link ListenerClassConsumer}'s dispatch must re-establish the sending tenant's configuration
 * overrides - not just its identity - before invoking the handler, mirroring what
 * {@code TenantConfigurationInitFilter} does for HTTP requests. Otherwise a generated
 * notification's {@code {appUrl}} (or any other tenant-overridable config value) would silently
 * read the global default for every tenant instead of its own.
 */
class ListenerClassConsumerTest {

    /** A listener that records the thread-scoped configuration visible while it runs. */
    static class RecordingHandler implements MessageHandler {

        Map<String, String> observedDuringDispatch;
        String userDuringDispatch;
        String languageDuringDispatch;
        RuntimeException failWith;
        /** Thrown one per run, before {@link #failWith} is considered. */
        final Deque<RuntimeException> failNextRuns = new ArrayDeque<>();
        int runs;
        final List<String> reportedErrors = new ArrayList<>();

        @Override
        public String destination() {
            return "notifications";
        }

        @Override
        public void onMessage(String message) {
            observedDuringDispatch = Configuration.getThreadConfiguration();
            userDuringDispatch = UserFacade.getName();
            languageDuringDispatch = UserFacade.findLanguage()
                                               .orElse(null);
            runs++;
            if (!failNextRuns.isEmpty()) {
                throw failNextRuns.poll();
            }
            if (failWith != null) {
                throw failWith;
            }
        }

        @Override
        public void onError(String error) {
            reportedErrors.add(error);
        }
    }

    private TenantConfigurationService tenantConfigurationService;
    private RecordingHandler handler;
    private MessageListener capturedListener;
    private ActiveMQConnectionArtifactsFactory connectionFactory;
    private Connection connection;
    private Queue queue;
    private ListAppender<ILoggingEvent> appender;
    private Logger consumerLogger;
    private final List<Long> pauses = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void setUp() throws Exception {
        ComponentContainer componentContainer = mock(ComponentContainer.class);
        connectionFactory = mock(ActiveMQConnectionArtifactsFactory.class);
        TenantContext tenantContext = mock(TenantContext.class);
        TenantPropertyManager tenantPropertyManager = mock(TenantPropertyManager.class);
        tenantConfigurationService = mock(TenantConfigurationService.class);
        Tenant defaultTenant = mock(Tenant.class);
        handler = new RecordingHandler();

        when(componentContainer.instanceOf(RecordingHandler.class)).thenReturn(Optional.of(handler));

        connection = mock(Connection.class);
        Session session = mock(Session.class);
        queue = mock(Queue.class);
        MessageConsumer messageConsumer = mock(MessageConsumer.class);
        when(connectionFactory.createConnection(any(), any())).thenReturn(connection);
        when(connectionFactory.createSession(connection)).thenReturn(session);
        when(session.createQueue("notifications")).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(messageConsumer);

        when(tenantPropertyManager.getCurrentTenantId(any())).thenReturn("acme");
        // Runs the dispatch inline on this test thread, the same contract the sibling
        // ScheduledClassConsumerTest relies on for TenantContext.executeForEachTenant.
        when(tenantContext.execute(any(String.class), any())).thenAnswer(
                invocation -> ((CallableResultAndException) invocation.getArgument(1)).call());
        // The subscription fan-out: one (default) tenant is all this test needs, so the logical name
        // is also the physical one. ListenerClassConsumerTenantSubscriptionTest covers the fan-out.
        Tenant currentTenant = mock(Tenant.class);
        when(currentTenant.getId()).thenReturn("default-tenant");
        when(tenantContext.getCurrentTenant()).thenReturn(currentTenant);
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            ((CallableResultAndException) invocation.getArgument(0)).call();
            return List.of();
        });
        DestinationNameManager destinationNameManager = mock(DestinationNameManager.class);
        when(destinationNameManager.toTenantName("notifications")).thenReturn("notifications");

        ListenerClassConsumer listenerConsumer = new ListenerClassConsumer(componentContainer, connectionFactory, tenantContext,
                tenantPropertyManager, defaultTenant, tenantConfigurationService, destinationNameManager, ListenerConnectionMode.SHARED);
        listenerConsumer.setDeadlockVictimPauser(pauses::add);
        listenerConsumer.onClassLoaded(new LoadedClass("sample", RecordingHandler.class.getName(), RecordingHandler.class,
                RecordingHandler.class.getClassLoader()));

        ArgumentCaptor<MessageListener> captor = ArgumentCaptor.forClass(MessageListener.class);
        verify(messageConsumer).setMessageListener(captor.capture());
        capturedListener = captor.getValue();

        appender = new ListAppender<>();
        appender.start();
        consumerLogger = (Logger) LoggerFactory.getLogger(ListenerClassConsumer.class);
        consumerLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        if (consumerLogger != null) {
            consumerLogger.detachAppender(appender);
        }
    }

    @Test
    void dispatchLoadsTheTenantsConfigOverridesBeforeInvokingTheHandler() throws Exception {
        Map<String, String> tenantOverrides = new HashMap<>();
        tenantOverrides.put("DIRIGIBLE_APP_BASE_URL", "https://acme.example.com");
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(tenantOverrides);

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        capturedListener.onMessage(message);

        assertEquals(tenantOverrides, handler.observedDuringDispatch,
                "the handler must see the sending tenant's own config overrides, not just its identity");
    }

    @Test
    void theInjectedConfigIsClearedAfterDispatchSoItNeverLeaksOntoThePooledThread() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(
                Map.of("DIRIGIBLE_APP_BASE_URL", "https://acme.example.com"));

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        capturedListener.onMessage(message);

        assertTrue(Configuration.getThreadConfiguration()
                                .isEmpty(),
                "the broker thread must not keep the previous message's tenant configuration");
    }

    /**
     * A handler that throws must leave a trace, and carry the throwable itself - logging only its
     * message loses the stack trace, which is the whole diagnostic value. The failure is now also
     * handed back to the broker (below), but that is a separate guarantee: redelivery re-runs the work,
     * it does not explain it. Without this line a failing trigger, rollup, posting or register lookup
     * is indistinguishable from one that never fired.
     */
    @Test
    void aThrowingHandlerIsReportedInsteadOfSilentlyDiscarded() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failWith = new IllegalStateException("cannot move from ISSUED to DRAFT");

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        // The failure escapes the listener now, so the report is asserted around the throw.
        assertThrows(Exception.class, () -> capturedListener.onMessage(message));

        List<ILoggingEvent> errors = appender.list.stream()
                                                  .filter(event -> event.getLevel() == Level.ERROR)
                                                  .toList();
        assertEquals(1, errors.size(), "a throwing handler must be reported exactly once");

        ILoggingEvent error = errors.get(0);
        assertTrue(error.getFormattedMessage()
                        .contains("cannot move from ISSUED to DRAFT"),
                "the report must carry the failure's message, got: " + error.getFormattedMessage());
        assertNotNull(error.getThrowableProxy(), "the throwable itself must be logged - passing only getMessage() loses the stack trace, "
                + "which is the whole diagnostic value");
    }

    /**
     * The failure must also LEAVE the listener, because that is the only thing the broker can observe.
     * A swallowed exception is indistinguishable from success: the message is acknowledged and the
     * event is gone for good, which also strands every handler whose correctness depends on a second
     * delivery - the generated posting glue repairs a half-written post on redelivery.
     */
    @Test
    void aThrowingHandlerIsHandedBackToTheBrokerRatherThanAcknowledged() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failWith = new IllegalStateException("journal line 3 of 5 violated a constraint");

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        Exception thrown = assertThrows(Exception.class, () -> capturedListener.onMessage(message),
                "the failure must escape onMessage - otherwise the broker sees a successful delivery");
        assertEquals("journal line 3 of 5 violated a constraint", thrown.getCause()
                                                                        .getMessage(),
                "the original failure must survive as the cause, not be flattened into a message");
    }

    /**
     * Reporting still happens first - handing the message back must not skip the handler's own hook.
     */
    @Test
    void theHandlersOnErrorStillRunsBeforeTheMessageIsHandedBack() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failWith = new IllegalStateException("boom");

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        assertThrows(Exception.class, () -> capturedListener.onMessage(message));

        assertEquals(List.of("boom"), handler.reportedErrors, "onError must still be invoked for the failed attempt");
    }

    /**
     * Without a policy the broker falls back to its own defaults, so the two listener paths would
     * disagree on how many times a failure is retried before it is dead-lettered.
     */
    @Test
    void theSubscriptionBoundsItsRetriesLikeTheJavaScriptListenerPath() {
        verify(connectionFactory).configureRedeliveryPolicy(connection, queue);
    }

    /**
     * The rows a posting or a reaction writes are attributed to the user whose write raised the event:
     * the handler runs with the user the platform's producer stamped bound, and the pooled broker
     * thread does not keep it (#8228).
     */
    @Test
    void theHandlerRunsAsTheUserWhoseWriteRaisedTheEvent() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");
        when(message.getObjectProperty("dirigible_originating_user")).thenReturn("alice");

        capturedListener.onMessage(message);

        assertEquals("alice", handler.userDuringDispatch);
        assertTrue(OriginatingUser.get()
                                  .isEmpty(),
                "the broker thread must not keep the previous message's user");
    }

    /**
     * The handler resolves its messages and reads multilingual rows in the language of the write that
     * raised the event, and the pooled broker thread does not keep it (#8776).
     */
    @Test
    void theHandlerRunsInTheLanguageOfTheWriteThatRaisedTheEvent() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");
        when(message.getObjectProperty("dirigible_originating_user")).thenReturn("alice");
        when(message.getObjectProperty("dirigible_originating_language")).thenReturn("bg");

        capturedListener.onMessage(message);

        assertEquals("bg", handler.languageDuringDispatch);
        assertTrue(ThreadLanguage.get()
                                 .isEmpty(),
                "the broker thread must not keep the previous message's language");
    }

    /** A message without one - a job's write, a foreign producer - is handled exactly as before. */
    @Test
    void aMessageWithoutAnOriginatingUserIsHandledAsBefore() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        capturedListener.onMessage(message);

        assertEquals("guest", handler.userDuringDispatch);
        assertNull(handler.languageDuringDispatch);
    }

    /**
     * A deadlock victim is run again in process rather than handed to the broker (#8745): the failure
     * is injected - the handler throws what the data store throws for a victim, a conflict whose cause
     * is the driver's SQLException with H2's deadlock SQLSTATE - so nothing here depends on two threads
     * racing for locks. The message is acknowledged after the run that succeeds; no redelivery is spent
     * and nothing is reported as a failure.
     */
    @Test
    void aDeadlockVictimIsRunAgainInProcessInsteadOfSpendingARedelivery() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failNextRuns.add(lockConflict("40001"));
        handler.failNextRuns.add(lockConflict("40001"));

        capturedListener.onMessage(textMessage());

        assertEquals(3, handler.runs, "two deadlocks, then the run that succeeds");
        assertEquals(2, pauses.size(), "each repeat waits for the transaction that won");
        assertTrue(handler.reportedErrors.isEmpty(), "a victim that succeeds on a repeat is no failure");
        assertTrue(errorLogs().isEmpty(), "a victim that succeeds on a repeat is no failure");
    }

    /** PostgreSQL reports a deadlock as 40P01 - production runs PostgreSQL. */
    @Test
    void aPostgresDeadlockVictimIsRunAgainInProcess() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failNextRuns.add(lockConflict("40P01"));

        capturedListener.onMessage(textMessage());

        assertEquals(2, handler.runs);
    }

    /**
     * A lock-wait timeout (H2 HYT00, PostgreSQL 55P03) is a victim too for a listener: no caller waits
     * for it, and the broker would run it again a second later anyway.
     */
    @Test
    void aLockWaitTimeoutIsRunAgainInProcess() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failNextRuns.add(lockConflict("HYT00"));
        handler.failNextRuns.add(lockConflict("55P03"));

        capturedListener.onMessage(textMessage());

        assertEquals(3, handler.runs);
    }

    /**
     * The repeats are bounded: a handler that keeps losing reaches the broker exactly as any failure
     * does - reported once, its onError run once, the message handed back for redelivery.
     */
    @Test
    void aVictimThatKeepsLosingIsHandedToTheBrokerAfterTheLastRun() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failWith = lockConflict("40001");

        Exception thrown = assertThrows(Exception.class, () -> capturedListener.onMessage(textMessage()));

        assertEquals(DeadlockVictimRetry.ATTEMPTS, handler.runs);
        assertEquals(DeadlockVictimRetry.ATTEMPTS - 1, pauses.size());
        assertTrue(DeadlockVictimRetry.isLockConflictVictim(thrown), "the last failure reaches the broker as the cause");
        assertEquals(1, handler.reportedErrors.size(), "onError runs once, for the delivery - not once per run");
        assertEquals(1, errorLogs().size(), "the failure is reported once, for the delivery");
    }

    /**
     * Any other failure is handed back at once, as before - a validation does not get better by
     * waiting.
     */
    @Test
    void aFailureThatIsNoLockConflictIsNotRunAgain() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());
        handler.failWith = new IllegalStateException("cannot move from ISSUED to DRAFT", new SQLException("constraint", "23505"));

        assertThrows(Exception.class, () -> capturedListener.onMessage(textMessage()));

        assertEquals(1, handler.runs);
        assertTrue(pauses.isEmpty());
    }

    /** The happy path is untouched: one run, no pause. */
    @Test
    void aHandlerThatSucceedsRunsOnce() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(Map.of());

        capturedListener.onMessage(textMessage());

        assertEquals(1, handler.runs);
        assertTrue(pauses.isEmpty());
    }

    /**
     * What the data store throws for a lock-conflict victim: a conflict caused by the driver's error.
     */
    private static RuntimeException lockConflict(String sqlState) {
        return new IllegalStateException("The record was changed by another write while this one was in progress",
                new SQLException("lock conflict", sqlState));
    }

    private static TextMessage textMessage() throws Exception {
        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");
        return message;
    }

    private List<ILoggingEvent> errorLogs() {
        return appender.list.stream()
                            .filter(event -> event.getLevel() == Level.ERROR)
                            .toList();
    }
}

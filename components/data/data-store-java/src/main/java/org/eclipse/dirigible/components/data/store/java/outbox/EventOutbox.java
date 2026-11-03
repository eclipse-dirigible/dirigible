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

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.data.store.java.repository.DomainEvent;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The transactional outbox for the events a generated repository publishes about its writes.
 *
 * <p>
 * Publishing straight to the broker after the row was committed is not safe: the two are separate
 * operations with nothing tying them together, so a broker outage both loses the event for good and
 * raises to a caller whose write actually succeeded — inviting a retry that duplicates the record.
 * Instead the event is recorded in the tenant's outbox table <em>on the writing transaction's own
 * connection</em>, so row and event commit together, and only then is it handed to the broker.
 *
 * <p>
 * That hand-off runs in-process right after the commit, so a healthy system publishes exactly as
 * promptly as before. When it fails, the entry simply stays in the table and
 * {@code EventOutboxRelayJob} retries it until the broker takes it. Delivery is therefore
 * at-least-once: an entry published just before the node died is republished by the relay, because
 * "sent" is only known once the row is gone.
 *
 * <p>
 * Each entry also records the user whose action raised it, and both deliveries - the in-process one
 * and the relay's - publish it with that user as the message's originating user, so whatever a
 * listener writes in reaction is attributed to that user (#8228).
 */
@Component
public class EventOutbox implements org.eclipse.dirigible.components.api.messaging.DurableMessagePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventOutbox.class);

    /** Entries handled per relay tick and per tenant. Bounds one tick, never the total. */
    private static final int RELAY_BATCH_SIZE = 100;

    /** Attempts after which an undelivered entry is reported as an error rather than a warning. */
    private static final int ATTEMPTS_BEFORE_ESCALATION = 10;

    private final EventOutboxStore store;

    EventOutbox(EventOutboxStore store) {
        this.store = store;
    }

    /**
     * Records the given events on the session's connection, inside the caller's open transaction.
     * Failing here fails that transaction — which is the point: an entity write whose event cannot be
     * recorded must not happen.
     *
     * @param session the Hibernate session whose transaction is writing the entity row
     * @param events the events to record; {@code null} or empty records nothing
     * @return the batch to {@link Batch#dispatch()} once the transaction has committed
     */
    public Batch record(Session session, List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return Batch.EMPTY;
        }
        String user = originatingUser();
        List<PendingEvent> pending = new ArrayList<>(events.size());
        for (DomainEvent event : events) {
            pending.add(new PendingEvent(UUID.randomUUID()
                                             .toString(),
                    event.topic(), event.payload(), 0, user));
        }
        // The relay leaves the entry alone until the grace period is out: the dispatch that follows this
        // transaction's commit is the fast path and owns the first attempt.
        Instant relayTakesOverAt = nextAttemptAt();
        session.doWork(connection -> {
            for (PendingEvent event : pending) {
                store.insert(connection, event, relayTakesOverAt);
            }
        });
        return new Batch(this, pending);
    }

    /**
     * The durable publish for an announcement DECOUPLED from the write it is about — deferred past a
     * synchronous chain's commit, or ordered after several transactions — where {@link #record} has no
     * transaction to join. The entry is recorded in its own short transaction and handed to the broker
     * immediately; whatever the broker refuses stays for the relay, exactly as a write-attached event
     * would. If even the recording fails (the outbox table unreachable), the message is handed to the
     * broker directly — the caller's work has already committed and an at-most-once attempt beats
     * failing a caller whose announcement machinery is down.
     *
     * @param topic the topic to publish on
     * @param payload the message body
     */
    @Override
    public void publishToTopic(String topic, String payload) {
        PendingEvent event = new PendingEvent(UUID.randomUUID()
                                                  .toString(),
                topic, payload, 0, originatingUser());
        try {
            prepare();
            store.insert(event, nextAttemptAt());
        } catch (RuntimeException | SQLException ex) {
            LOGGER.error("Failed to record a durable publish on topic [{}]; attempting the direct publish instead.", topic, ex);
            MessagingFacade.sendToTopic(topic, payload);
            return;
        }
        deliver(event);
    }

    /**
     * Makes sure the current tenant's outbox table exists. Called before the write transaction opens so
     * that the table's DDL never runs inside it.
     *
     * @throws IllegalStateException if the table is missing and cannot be created
     */
    public void prepare() {
        try {
            store.prepare();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to prepare the event outbox table [" + EventOutboxStore.TABLE_NAME + "]", ex);
        }
    }

    /**
     * Drains the current tenant's outbox of everything due for another attempt. Called by the relay job
     * inside a tenant execution scope.
     */
    void relay() {
        List<PendingEvent> due;
        try {
            if (!store.tableExists()) {
                return;
            }
            due = store.findDue(Instant.now(), RELAY_BATCH_SIZE);
        } catch (SQLException ex) {
            LOGGER.error("Failed to read the event outbox; will retry on the next tick.", ex);
            return;
        }
        Instant nextAttemptAt = nextAttemptAt();
        for (PendingEvent event : due) {
            try {
                if (!store.claim(event, nextAttemptAt)) {
                    // Another relay took it; leave it to that one.
                    continue;
                }
            } catch (SQLException ex) {
                LOGGER.error("Failed to claim outbox entry [{}]; will retry on the next tick.", event.id(), ex);
                return;
            }
            if (!deliver(event)) {
                // The broker is very likely still unavailable — stop the batch instead of failing
                // every remaining entry, and pick it up again on the next tick.
                return;
            }
        }
    }

    /**
     * Publishes one entry and, on success, drops it from the outbox.
     *
     * @param event the entry to deliver
     * @return true if the broker accepted it
     */
    private boolean deliver(PendingEvent event) {
        try {
            // Bound from the entry, not from the delivering thread: the relay runs on a job thread with
            // no user, and the producer stamps whatever is bound here onto the message.
            try (OriginatingUser.Binding binding = OriginatingUser.bind(event.user())) {
                MessagingFacade.sendToTopic(event.topic(), event.payload());
            }
        } catch (RuntimeException ex) {
            String message = "Failed to publish outbox entry [{}] on topic [{}] ([{}] failed attempts so far); "
                    + "it stays in the outbox and the relay keeps trying.";
            if (event.attempts() >= ATTEMPTS_BEFORE_ESCALATION) {
                LOGGER.error(message, event.id(), event.topic(), event.attempts(), ex);
            } else {
                LOGGER.warn(message, event.id(), event.topic(), event.attempts(), ex);
            }
            recordError(event, ex);
            return false;
        }
        try {
            store.delete(event.id());
        } catch (SQLException ex) {
            // Published but not cleared: the relay will publish it once more. At-least-once is the
            // contract, so this is a duplicate, not a loss.
            LOGGER.warn("Published outbox entry [{}] but failed to clear it; it may be delivered again.", event.id(), ex);
        }
        return true;
    }

    /**
     * The user an event recorded now is attributed to: the request's user, or - for a write a handler
     * makes in reaction to an event - the user whose action raised that one, so a chain of reactions
     * keeps naming the person who started it. {@code null} when no user is known (a job, a boot-time
     * seed); the handlers then run as any other background work does.
     *
     * @return the user, or {@code null}
     */
    private static String originatingUser() {
        return UserFacade.findName()
                         .orElse(null);
    }

    /**
     * @return when an entry attempted (or written) now becomes the relay's to take over
     */
    private static Instant nextAttemptAt() {
        return Instant.now()
                      .plus(Duration.ofSeconds(DirigibleConfig.EVENT_OUTBOX_RELAY_GRACE_SECONDS.getIntValue()));
    }

    private void recordError(PendingEvent event, RuntimeException failure) {
        try {
            store.recordError(event.id(), failure.getMessage());
        } catch (SQLException ex) {
            LOGGER.debug("Failed to record the delivery error of outbox entry [{}].", event.id(), ex);
        }
    }

    /**
     * The events one committed transaction recorded, ready to be handed to the broker.
     */
    public static final class Batch {

        private static final Batch EMPTY = new Batch(null, Collections.emptyList());

        private final EventOutbox outbox;

        private final List<PendingEvent> events;

        private Batch(EventOutbox outbox, List<PendingEvent> events) {
            this.outbox = outbox;
            this.events = events;
        }

        /**
         * Publishes the recorded events, in the order they were recorded. Never raises: everything it
         * cannot deliver stays in the outbox for the relay, because the write this batch belongs to has
         * already succeeded and must not be reported as failed.
         */
        public void dispatch() {
            for (PendingEvent event : events) {
                if (!outbox.deliver(event)) {
                    return;
                }
            }
        }
    }

}

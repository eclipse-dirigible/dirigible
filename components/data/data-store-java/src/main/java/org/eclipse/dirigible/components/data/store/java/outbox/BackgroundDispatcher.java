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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hands committed outbox entries to the broker on a thread of its own, so the write that raised
 * them does not wait for the broker's answer (#8923).
 *
 * <p>
 * Nothing here holds an event that the outbox table does not hold: an entry reaches this queue only
 * after its row committed, and its row goes only once the broker took it. So whatever this
 * dispatcher does not send - the queue was full, the entry waited too long, the instance stopped -
 * is sent by {@code EventOutboxRelayJob} when the entry's grace period is out.
 *
 * <p>
 * One thread, on purpose. It keeps the order of hand-off: the events of one transaction in the
 * order they were recorded, and the hand-offs in the order they arrived. It costs no throughput,
 * because the producer sends through one shared session already.
 */
final class BackgroundDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundDispatcher.class);

    /**
     * Entries that may wait for the thread at once. Each is its id, topic and payload, which the table
     * holds anyway.
     */
    static final int QUEUE_CAPACITY = 10_000;

    private final TenantContext tenantContext;

    /** Publishes one entry and clears it from the outbox; false when the broker did not take it. */
    private final Predicate<PendingEvent> delivery;

    private final BlockingQueue<Handoff> queue = new LinkedBlockingQueue<>();

    /** The entries in {@link #queue}, counted by entry and not by hand-off. */
    private final AtomicInteger queued = new AtomicInteger();

    /**
     * Whether the queue was reported full and has not drained since - one warning, not one per write.
     */
    private final AtomicBoolean reportedFull = new AtomicBoolean();

    private Thread thread;

    private volatile boolean stopped;

    BackgroundDispatcher(TenantContext tenantContext, Predicate<PendingEvent> delivery) {
        this.tenantContext = tenantContext;
        this.delivery = delivery;
    }

    /**
     * Takes the entries one committed transaction recorded and returns at once. Never raises: the write
     * these belong to has succeeded.
     *
     * @param events the entries, in the order they were recorded
     * @param relayTakesOverAt when the relay starts sending these entries itself
     */
    void handOff(List<PendingEvent> events, Instant relayTakesOverAt) {
        if (events.isEmpty() || stopped) {
            return;
        }
        if (queued.addAndGet(events.size()) > QUEUE_CAPACITY) {
            queued.addAndGet(-events.size());
            if (reportedFull.compareAndSet(false, true)) {
                LOGGER.warn("The event outbox's background dispatch has [{}] entries waiting for the broker and takes no more; "
                        + "the entries stay in the outbox and the relay publishes them.", QUEUE_CAPACITY);
            }
            return;
        }
        // The destination's name and the tenant stamped on the message come from the current tenant, and
        // the thread that sends has none of its own.
        Tenant tenant = tenantContext.isNotInitialized() ? null : tenantContext.getCurrentTenant();
        queue.add(new Handoff(tenant, List.copyOf(events), relayTakesOverAt));
        ensureStarted();
    }

    /**
     * Stops the thread. Nothing is drained: what is still queued is in the outbox table, and the relay
     * of the next start, or of another instance, publishes it.
     */
    synchronized void stop() {
        stopped = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private synchronized void ensureStarted() {
        if (stopped || (thread != null && thread.isAlive())) {
            return;
        }
        thread = new Thread(this::run, "event-outbox-dispatch");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        while (!stopped) {
            Handoff handoff;
            try {
                handoff = queue.take();
            } catch (InterruptedException ex) {
                return;
            }
            if (queued.addAndGet(-handoff.events()
                                         .size()) <= QUEUE_CAPACITY / 2) {
                reportedFull.set(false);
            }
            try {
                deliver(handoff);
            } catch (RuntimeException ex) {
                LOGGER.error("The event outbox's background dispatch failed; the entries stay in the outbox and the relay publishes them.",
                        ex);
            }
        }
    }

    private void deliver(Handoff handoff) {
        if (handoff.tenant() == null) {
            deliverInOrder(handoff);
            return;
        }
        tenantContext.execute(handoff.tenant(), () -> {
            deliverInOrder(handoff);
            return null;
        });
    }

    private void deliverInOrder(Handoff handoff) {
        for (PendingEvent event : handoff.events()) {
            if (stopped || !Instant.now()
                                   .isBefore(handoff.relayTakesOverAt())) {
                // The relay sends these from here on: publishing them too would only double them.
                return;
            }
            if (!delivery.test(event)) {
                // As on the writing thread: the rest of this transaction's events follow the one the
                // broker refused, through the relay, so their order holds.
                return;
            }
        }
    }

    /** What one committed transaction handed over. */
    private record Handoff(Tenant tenant, List<PendingEvent> events, Instant relayTakesOverAt) {
    }

}

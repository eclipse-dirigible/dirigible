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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The background dispatch of the outbox (#8923): a hand-off returns although the broker does not
 * answer, the entries go out in the order they were handed over, and whatever is not sent is left -
 * never lost, because every entry here is a row of the outbox table that the relay publishes.
 */
class BackgroundDispatcherTest {

    private static final long WAIT_SECONDS = 10;

    private final TenantContext tenantContext = mock(TenantContext.class);

    /** What the broker took, in the order it took it. */
    private final List<String> delivered = Collections.synchronizedList(new ArrayList<>());

    /** Closed while the broker does not answer. */
    private final CountDownLatch brokerAnswers = new CountDownLatch(1);

    /** Opens when the first entry has reached the broker and waits for its answer. */
    private final CountDownLatch firstReachedBroker = new CountDownLatch(1);

    private BackgroundDispatcher dispatcher;

    BackgroundDispatcherTest() {
        when(tenantContext.isNotInitialized()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        brokerAnswers.countDown();
        if (dispatcher != null) {
            dispatcher.stop();
        }
    }

    @Test
    void aHandOffReturnsWhileTheBrokerDoesNotAnswer() throws InterruptedException {
        dispatcher = new BackgroundDispatcher(tenantContext, this::deliverWhenTheBrokerAnswers);

        dispatcher.handOff(List.of(event("a")), inAMinute());

        // Back on this thread although the broker has the entry and has not answered.
        assertThat(firstReachedBroker.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(delivered).isEmpty();

        brokerAnswers.countDown();
        awaitDelivered(1);
        assertThat(delivered).containsExactly("a");
    }

    @Test
    void theEntriesGoOutInTheOrderTheyWereHandedOver() throws InterruptedException {
        dispatcher = new BackgroundDispatcher(tenantContext, this::deliverWhenTheBrokerAnswers);

        dispatcher.handOff(List.of(event("a1"), event("a2")), inAMinute());
        dispatcher.handOff(List.of(event("b1")), inAMinute());
        dispatcher.handOff(List.of(event("c1"), event("c2")), inAMinute());

        brokerAnswers.countDown();
        awaitDelivered(5);
        assertThat(delivered).containsExactly("a1", "a2", "b1", "c1", "c2");
    }

    @Test
    void theEntriesBehindARefusedOneAreLeftToTheRelayAndTheNextHandOffGoesOn() throws InterruptedException {
        dispatcher = new BackgroundDispatcher(tenantContext, event -> {
            if ("a2".equals(event.id())) {
                return false;
            }
            delivered.add(event.id());
            return true;
        });

        dispatcher.handOff(List.of(event("a1"), event("a2"), event("a3")), inAMinute());
        dispatcher.handOff(List.of(event("b1")), inAMinute());

        awaitDelivered(2);
        // a3 follows a2 through the relay, so the order of that transaction's events holds.
        assertThat(delivered).containsExactly("a1", "b1");
    }

    @Test
    void aFullQueueDropsTheHandOffAndReturnsAtOnce() throws InterruptedException {
        dispatcher = new BackgroundDispatcher(tenantContext, this::deliverWhenTheBrokerAnswers);
        dispatcher.handOff(List.of(event("first")), inAMinute());
        assertThat(firstReachedBroker.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        List<PendingEvent> full = new ArrayList<>();
        for (int i = 0; i < BackgroundDispatcher.QUEUE_CAPACITY; i++) {
            full.add(event("queued-" + i));
        }
        dispatcher.handOff(full, inAMinute());
        dispatcher.handOff(List.of(event("one-too-many")), inAMinute());

        brokerAnswers.countDown();
        awaitDelivered(1 + BackgroundDispatcher.QUEUE_CAPACITY);
        // Its row is in the outbox table; the relay publishes it.
        assertThat(delivered).doesNotContain("one-too-many")
                             .hasSize(1 + BackgroundDispatcher.QUEUE_CAPACITY);

        // Drained, so it takes hand-offs again.
        dispatcher.handOff(List.of(event("later")), inAMinute());
        awaitDelivered(2 + BackgroundDispatcher.QUEUE_CAPACITY);
        assertThat(delivered).endsWith("later");
    }

    @Test
    void anEntryWhoseGracePeriodRanOutWhileItWaitedIsLeftToTheRelay() throws InterruptedException {
        dispatcher = new BackgroundDispatcher(tenantContext, this::deliverWhenTheBrokerAnswers);

        dispatcher.handOff(List.of(event("blocks")), inAMinute());
        assertThat(firstReachedBroker.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        // The relay sends this one from now on: sending it here too would publish it twice.
        dispatcher.handOff(List.of(event("the-relays")), Instant.now());
        dispatcher.handOff(List.of(event("after")), inAMinute());

        brokerAnswers.countDown();
        awaitDelivered(2);
        assertThat(delivered).containsExactly("blocks", "after");
    }

    @Test
    void theEntriesAreSentInTheTenantOfTheWriteThatHandedThemOver() throws Throwable {
        Tenant tenant = mock(Tenant.class);
        AtomicReference<Tenant> scope = new AtomicReference<>();
        when(tenantContext.isNotInitialized()).thenReturn(false);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        when(tenantContext.execute(eq(tenant), any())).thenAnswer(invocation -> {
            CallableResultAndException<?, ?> work = invocation.getArgument(1);
            scope.set(tenant);
            try {
                return work.call();
            } finally {
                scope.set(null);
            }
        });
        List<Tenant> sentIn = Collections.synchronizedList(new ArrayList<>());
        dispatcher = new BackgroundDispatcher(tenantContext, event -> {
            sentIn.add(scope.get());
            return delivered.add(event.id());
        });

        dispatcher.handOff(List.of(event("a"), event("b")), inAMinute());

        awaitDelivered(2);
        // The destination's name and the tenant stamped on the message come from this scope.
        assertThat(sentIn).containsExactly(tenant, tenant);
    }

    @Test
    void aStoppedDispatcherSendsNothingMore() throws InterruptedException {
        dispatcher = new BackgroundDispatcher(tenantContext, this::deliverWhenTheBrokerAnswers);
        dispatcher.handOff(List.of(event("in-flight")), inAMinute());
        assertThat(firstReachedBroker.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        dispatcher.handOff(List.of(event("queued")), inAMinute());

        dispatcher.stop();
        dispatcher.handOff(List.of(event("after-the-stop")), inAMinute());
        brokerAnswers.countDown();

        // Nothing is drained: both are rows of the outbox table, and the relay publishes them.
        TimeUnit.MILLISECONDS.sleep(200);
        assertThat(delivered).doesNotContain("queued", "after-the-stop");
    }

    private boolean deliverWhenTheBrokerAnswers(PendingEvent event) {
        firstReachedBroker.countDown();
        try {
            if (!brokerAnswers.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException ex) {
            return false;
        }
        return delivered.add(event.id());
    }

    private void awaitDelivered(int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (delivered.size() < count && System.nanoTime() < deadline) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertThat(delivered).hasSizeGreaterThanOrEqualTo(count);
    }

    private static PendingEvent event(String id) {
        return new PendingEvent(id, "outbox-dispatch-test", "{}", 0, null, null);
    }

    private static Instant inAMinute() {
        return Instant.now()
                      .plus(Duration.ofMinutes(1));
    }
}

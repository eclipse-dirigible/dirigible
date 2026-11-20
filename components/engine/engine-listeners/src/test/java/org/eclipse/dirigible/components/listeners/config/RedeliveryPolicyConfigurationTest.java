/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.region.policy.RedeliveryPolicyMap;
import org.apache.activemq.command.ActiveMQQueue;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The broker's redelivery budget for a failed listener delivery is configurable (#8745), and its
 * defaults are the budget the platform always applied: redeliveries about 1, 6 and 31 seconds after
 * the first delivery, then the dead-letter queue.
 */
class RedeliveryPolicyConfigurationTest {

    private static final List<DirigibleConfig> KEYS =
            List.of(DirigibleConfig.MESSAGING_REDELIVERY_INITIAL_DELAY_MILLIS, DirigibleConfig.MESSAGING_REDELIVERY_BACK_OFF_MULTIPLIER,
                    DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_DELAY_MILLIS, DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES);

    @AfterEach
    void restoreTheDefaults() {
        KEYS.forEach(key -> Configuration.remove(key.getKey()));
    }

    @Test
    void theDefaultsAreTheBudgetThePlatformAlwaysApplied() {
        RedeliveryPolicy policy = ActiveMQConnectionArtifactsFactory.redeliveryPolicy();

        assertThat(policy.getMaximumRedeliveries()).isEqualTo(3);
        assertThat(delaysBeforeEachRedelivery(policy)).containsExactly(1000L, 5000L, 25000L);
    }

    @Test
    void theBudgetFollowsTheConfiguration() {
        DirigibleConfig.MESSAGING_REDELIVERY_INITIAL_DELAY_MILLIS.setStringValue("2000");
        DirigibleConfig.MESSAGING_REDELIVERY_BACK_OFF_MULTIPLIER.setStringValue("1.5");
        DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES.setStringValue("5");

        RedeliveryPolicy policy = ActiveMQConnectionArtifactsFactory.redeliveryPolicy();

        assertThat(policy.getMaximumRedeliveries()).isEqualTo(5);
        assertThat(delaysBeforeEachRedelivery(policy)).containsExactly(2000L, 3000L, 4500L, 6750L, 10125L);
    }

    @Test
    void theMaximumDelayCapsTheBackOff() {
        DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_DELAY_MILLIS.setStringValue("10000");
        DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES.setStringValue("5");

        RedeliveryPolicy policy = ActiveMQConnectionArtifactsFactory.redeliveryPolicy();

        assertThat(delaysBeforeEachRedelivery(policy)).containsExactly(1000L, 5000L, 10000L, 10000L, 10000L);
    }

    @Test
    void aMultiplierOfOneRedeliversAtAFixedInterval() {
        DirigibleConfig.MESSAGING_REDELIVERY_BACK_OFF_MULTIPLIER.setStringValue("1");

        RedeliveryPolicy policy = ActiveMQConnectionArtifactsFactory.redeliveryPolicy();

        assertThat(delaysBeforeEachRedelivery(policy)).containsExactly(1000L, 1000L, 1000L);
    }

    @Test
    void anInvalidValueFallsBackToItsDefault() {
        DirigibleConfig.MESSAGING_REDELIVERY_INITIAL_DELAY_MILLIS.setStringValue("soon");
        DirigibleConfig.MESSAGING_REDELIVERY_BACK_OFF_MULTIPLIER.setStringValue("0.5");
        DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES.setStringValue("-7");

        RedeliveryPolicy policy = ActiveMQConnectionArtifactsFactory.redeliveryPolicy();

        assertThat(policy.getMaximumRedeliveries()).isEqualTo(3);
        assertThat(delaysBeforeEachRedelivery(policy)).containsExactly(1000L, 5000L, 25000L);
    }

    /** {@code -1} is ActiveMQ's "no limit", and it is accepted as such. */
    @Test
    void unlimitedRedeliveriesAreAccepted() {
        DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES.setStringValue("-1");

        assertThat(ActiveMQConnectionArtifactsFactory.redeliveryPolicy()
                                                     .getMaximumRedeliveries()).isEqualTo(-1);
    }

    @Test
    void theSubscriptionsDestinationGetsTheConfiguredPolicy() {
        DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES.setStringValue("7");
        ActiveMQConnection connection = mock(ActiveMQConnection.class);
        RedeliveryPolicyMap policyMap = new RedeliveryPolicyMap();
        when(connection.getRedeliveryPolicyMap()).thenReturn(policyMap);
        ActiveMQQueue queue = new ActiveMQQueue("orders");

        new ActiveMQConnectionArtifactsFactory(mock(ActiveMQConnectionFactory.class)).configureRedeliveryPolicy(connection, queue);

        assertThat(policyMap.getEntryFor(queue)
                            .getMaximumRedeliveries()).isEqualTo(7);
    }

    /**
     * The delay before each redelivery, computed the way ActiveMQ's consumer computes it: the initial
     * delay for the first, then {@code getNextRedeliveryDelay} of the previous one.
     */
    private static List<Long> delaysBeforeEachRedelivery(RedeliveryPolicy policy) {
        List<Long> delays = new ArrayList<>();
        long delay = policy.getInitialRedeliveryDelay();
        for (int redelivery = 1; redelivery <= policy.getMaximumRedeliveries(); redelivery++) {
            delays.add(delay);
            delay = policy.getNextRedeliveryDelay(delay);
        }
        return delays;
    }
}

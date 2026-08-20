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
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The Class MessagingBrokerConditionsTest.
 */
@ExtendWith(MockitoExtension.class)
class MessagingBrokerConditionsTest {

    /** The broker url key. */
    private static final String BROKER_URL_KEY = DirigibleConfig.MESSAGING_BROKER_URL.getKey();

    /** The embedded condition. */
    private final EmbeddedMessagingBrokerCondition embeddedCondition = new EmbeddedMessagingBrokerCondition();

    /** The external condition. */
    private final ExternalMessagingBrokerCondition externalCondition = new ExternalMessagingBrokerCondition();

    /** The condition context - the conditions decide from the Dirigible configuration, not from it. */
    @Mock
    private ConditionContext conditionContext;

    /** The metadata. */
    @Mock
    private AnnotatedTypeMetadata metadata;

    /**
     * Tear down.
     */
    @AfterEach
    void tearDown() {
        Configuration.remove(BROKER_URL_KEY);
    }

    /**
     * Test no broker url means embedded.
     */
    @Test
    void testNoBrokerUrlMeansEmbedded() {
        assertThat(embeddedCondition.matches(conditionContext, metadata)).isTrue();
        assertThat(externalCondition.matches(conditionContext, metadata)).isFalse();
    }

    /**
     * Test broker url means external.
     */
    @Test
    void testBrokerUrlMeansExternal() {
        Configuration.set(BROKER_URL_KEY, "tcp://localhost:61616");

        assertThat(externalCondition.matches(conditionContext, metadata)).isTrue();
        assertThat(embeddedCondition.matches(conditionContext, metadata)).isFalse();
    }

    /**
     * A blank value is how a container "does not set" an environment variable.
     */
    @Test
    void testBlankBrokerUrlMeansEmbedded() {
        Configuration.set(BROKER_URL_KEY, "   ");

        assertThat(embeddedCondition.matches(conditionContext, metadata)).isTrue();
        assertThat(externalCondition.matches(conditionContext, metadata)).isFalse();
    }

    /**
     * Exactly one mode is always selected - otherwise the messaging beans would be defined twice or not
     * at all.
     */
    @Test
    void testTheModesAreMutuallyExclusive() {
        assertThat(embeddedCondition.matches(conditionContext, metadata)).isNotEqualTo(
                externalCondition.matches(conditionContext, metadata));

        Configuration.set(BROKER_URL_KEY, "failover:(tcp://one:61616,tcp://two:61616)");

        assertThat(embeddedCondition.matches(conditionContext, metadata)).isNotEqualTo(
                externalCondition.matches(conditionContext, metadata));
    }
}

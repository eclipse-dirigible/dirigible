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
import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Pins the shape of the two messaging configurations. The wiring is asserted by reflection rather
 * than by starting a context: refreshing one would start a real broker against a real datasource,
 * which is what the integration tests are for.
 */
class MessagingConfigSelectionTest {

    /**
     * Test both configurations are conditional, on opposite conditions.
     */
    @Test
    void testEachModeIsSelectedByItsOwnCondition() {
        assertThat(EmbeddedBrokerMessagingConfig.class.getAnnotation(Configuration.class)).isNotNull();
        assertThat(EmbeddedBrokerMessagingConfig.class.getAnnotation(Conditional.class)
                                                      .value()).containsExactly(EmbeddedMessagingBrokerCondition.class);

        assertThat(ExternalBrokerMessagingConfig.class.getAnnotation(Configuration.class)).isNotNull();
        assertThat(ExternalBrokerMessagingConfig.class.getAnnotation(Conditional.class)
                                                      .value()).containsExactly(ExternalMessagingBrokerCondition.class);
    }

    /**
     * Both modes must define the connection factory and the named connection and session beans - every
     * consumer of the messaging engine resolves those.
     */
    @Test
    void testBothModesDefineTheBeansTheEngineConsumes() {
        assertThat(beanNames(EmbeddedBrokerMessagingConfig.class)).contains("ActiveMQConnection", "ActiveMQSession");
        assertThat(beanNames(ExternalBrokerMessagingConfig.class)).contains("ActiveMQConnection", "ActiveMQSession");

        assertThat(beanType(EmbeddedBrokerMessagingConfig.class, "createActiveMQConnectionFactory")).isEqualTo(
                ActiveMQConnectionFactory.class);
        assertThat(beanType(ExternalBrokerMessagingConfig.class, "createActiveMQConnectionFactory")).isEqualTo(
                ActiveMQConnectionFactory.class);
    }

    /**
     * The embedded mode owns the broker; the external mode must never define one, nor order anything
     * after one, because in that mode nothing starts a broker.
     */
    @Test
    void testOnlyTheEmbeddedModeOwnsABroker() {
        assertThat(beanTypes(EmbeddedBrokerMessagingConfig.class)).contains(BrokerService.class);
        assertThat(beanNames(EmbeddedBrokerMessagingConfig.class)).contains("ActiveMQBroker");

        assertThat(beanTypes(ExternalBrokerMessagingConfig.class)).doesNotContain(BrokerService.class);
        assertThat(Arrays.stream(ExternalBrokerMessagingConfig.class.getDeclaredMethods())
                         .anyMatch(method -> null != method.getAnnotation(DependsOn.class))).isFalse();
    }

    /**
     * Bean names declared by a configuration - the explicit ones, which are the ones consumers rely on.
     *
     * @param configClass the configuration class
     * @return the declared bean names
     */
    private static Iterable<String> beanNames(Class<?> configClass) {
        return Arrays.stream(configClass.getDeclaredMethods())
                     .map(method -> method.getAnnotation(Bean.class))
                     .filter(bean -> null != bean)
                     .flatMap(bean -> Arrays.stream(bean.value()))
                     .toList();
    }

    /**
     * Types produced by a configuration's bean methods.
     *
     * @param configClass the configuration class
     * @return the produced types
     */
    private static Iterable<Class<?>> beanTypes(Class<?> configClass) {
        return Arrays.stream(configClass.getDeclaredMethods())
                     .filter(method -> null != method.getAnnotation(Bean.class))
                     .map(Method::getReturnType)
                     .toList();
    }

    /**
     * Type produced by a named bean method.
     *
     * @param configClass the configuration class
     * @param methodName the bean method name
     * @return the produced type
     */
    private static Class<?> beanType(Class<?> configClass, String methodName) {
        return Arrays.stream(configClass.getDeclaredMethods())
                     .filter(method -> methodName.equals(method.getName()) && null != method.getAnnotation(Bean.class))
                     .map(Method::getReturnType)
                     .findFirst()
                     .orElseThrow(() -> new AssertionError("No bean method [" + methodName + "] in " + configClass));
    }
}

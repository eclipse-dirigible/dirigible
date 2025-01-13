/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Class BackgroundListenerManagerFactoryTest.
 */
@ExtendWith(MockitoExtension.class)
class ListenerManagerFactoryTest {
    /** The factory. */
    @InjectMocks
    private ListenerManagerFactory factory;

    /** The asynchronous message listener factory. */
    @Mock
    private AsynchronousMessageListenerFactory asynchronousMessageListenerFactory;

    /** The connection artifacts factory. */
    @Mock
    private ActiveMQConnectionArtifactsFactory connectionArtifactsFactory;

    /** The listener. */
    @Mock
    private ListenerDescriptor listenerDescriptor;

    /**
     * Test create.
     */
    @Test
    void testCreate() {
        ListenerManager manager = factory.create(listenerDescriptor);

        assertThat(manager).isNotNull();
    }

}

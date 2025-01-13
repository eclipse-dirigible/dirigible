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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A factory for creating ListenerManager objects.
 */
@Component
public class ListenerManagerFactory {

    /** The connection artifacts factory. */
    private final ActiveMQConnectionArtifactsFactory connectionArtifactsFactory;

    /** The asynchronous message listener factory. */
    private final AsynchronousMessageListenerFactory asynchronousMessageListenerFactory;

    /**
     * Instantiates a new listener manager factory.
     *
     * @param connectionArtifactsFactory the connection artifacts factory
     * @param asynchronousMessageListenerFactory the asynchronous message listener factory
     */
    @Autowired
    public ListenerManagerFactory(ActiveMQConnectionArtifactsFactory connectionArtifactsFactory,
            AsynchronousMessageListenerFactory asynchronousMessageListenerFactory) {
        this.connectionArtifactsFactory = connectionArtifactsFactory;
        this.asynchronousMessageListenerFactory = asynchronousMessageListenerFactory;
    }

    /**
     * Creates the.
     *
     * @param listenerDescriptor the listener
     * @return the listener manager
     */
    public ListenerManager create(ListenerDescriptor listenerDescriptor) {
        return new ListenerManager(listenerDescriptor, connectionArtifactsFactory, asynchronousMessageListenerFactory);
    }
}

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;

/**
 * The Class ConnectionArtifacts.
 */
public class ConnectionArtifacts {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionArtifacts.class);

    /** The connection. */
    private final Connection connection;

    /** The session. */
    private final Session session;

    /** The message consumer. */
    private final MessageConsumer messageConsumer;

    /**
     * Instantiates a new connection artifacts.
     *
     * @param connection the connection
     * @param session the session
     * @param messageConsumer the message consumer
     */
    public ConnectionArtifacts(Connection connection, Session session, MessageConsumer messageConsumer) {
        this.connection = connection;
        this.session = session;
        this.messageConsumer = messageConsumer;
    }

    /**
     * Close all.
     */
    public void closeAll() {
        close(messageConsumer);
        close(session);
        close(connection);
    }

    /**
     * Close.
     *
     * @param closeable the closeable
     */
    private void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ex) {
            LOGGER.warn("Failed to close {}", closeable, ex);
        }

    }
}

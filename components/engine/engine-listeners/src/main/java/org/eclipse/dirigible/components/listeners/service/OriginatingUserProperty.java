/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import java.util.Optional;

import org.eclipse.dirigible.components.base.context.OriginatingUser;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

/**
 * Carries the {@link OriginatingUser} from the thread that sends a message to the thread that
 * handles it, the way {@link TenantPropertyManager} carries the tenant. Public because the client
 * Java listeners (engine-java) restore it too.
 *
 * <p>
 * The property is written only by {@link MessageProducer}, from the user the platform bound to the
 * sending thread; the messaging APIs take a destination and a body, so no caller can set it, and a
 * message without it is handled with no originating user, as before.
 */
public final class OriginatingUserProperty {

    /** The message property naming the originating user. */
    static final String PROPERTY_NAME = "dirigible_originating_user";

    private OriginatingUserProperty() {}

    /**
     * Stamps the originating user bound to the current thread, if any, onto the message.
     *
     * @param message the message about to be sent
     * @throws JMSException if the property cannot be set
     */
    static void stamp(Message message) throws JMSException {
        Optional<String> user = OriginatingUser.get();
        if (user.isPresent()) {
            message.setStringProperty(PROPERTY_NAME, user.get());
        }
    }

    /**
     * Reads the originating user the producer stamped on a received message.
     *
     * @param message the received message
     * @return the user, or {@code null} when the message carries none
     * @throws JMSException if the property cannot be read
     */
    public static String read(Message message) throws JMSException {
        Object user = message.getObjectProperty(PROPERTY_NAME);
        return user instanceof String name && !name.isBlank() ? name : null;
    }
}

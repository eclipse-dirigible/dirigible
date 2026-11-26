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
import org.eclipse.dirigible.components.base.context.ThreadLanguage;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

/**
 * Carries the {@link OriginatingUser} from the thread that sends a message to the thread that
 * handles it, the way {@link TenantPropertyManager} carries the tenant. Public because the client
 * Java listeners (engine-java) restore it too. The {@link ThreadLanguage} of the sending thread -
 * the language of that user's write - travels beside it in a property of its own (#8776).
 *
 * <p>
 * The properties are written only by {@link MessageProducer}, from what the platform bound to the
 * sending thread; the messaging APIs take a destination and a body, so no caller can set them, and
 * a message without them is handled with no originating user and no language, as before.
 */
public final class OriginatingUserProperty {

    /** The message property naming the originating user. */
    static final String PROPERTY_NAME = "dirigible_originating_user";

    /** The message property naming the originating user's language. */
    static final String LANGUAGE_PROPERTY_NAME = "dirigible_originating_language";

    private OriginatingUserProperty() {}

    /**
     * Stamps the originating user and the language bound to the current thread, each if any, onto the
     * message.
     *
     * @param message the message about to be sent
     * @throws JMSException if a property cannot be set
     */
    static void stamp(Message message) throws JMSException {
        stamp(message, PROPERTY_NAME, OriginatingUser.get());
        stamp(message, LANGUAGE_PROPERTY_NAME, ThreadLanguage.get());
    }

    private static void stamp(Message message, String property, Optional<String> value) throws JMSException {
        if (value.isPresent()) {
            message.setStringProperty(property, value.get());
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
        return readString(message, PROPERTY_NAME);
    }

    /**
     * Reads the originating user's language the producer stamped on a received message.
     *
     * @param message the received message
     * @return the language, or {@code null} when the message carries none
     * @throws JMSException if the property cannot be read
     */
    public static String readLanguage(Message message) throws JMSException {
        return readString(message, LANGUAGE_PROPERTY_NAME);
    }

    private static String readString(Message message, String property) throws JMSException {
        Object value = message.getObjectProperty(property);
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}

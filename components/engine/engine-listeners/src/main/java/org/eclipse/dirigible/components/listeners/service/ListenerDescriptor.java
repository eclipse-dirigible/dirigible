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

import java.util.Objects;

/**
 * The Class ListenerDescriptor.
 */
class ListenerDescriptor {

    /** The type. */
    private final ListenerType type;

    /** The destination. */
    private final String destination;

    /** The handler path. */
    private final String handlerPath;

    /**
     * Instantiates a new listener descriptor.
     *
     * @param type the type
     * @param destination the destination
     * @param handlerPath the handler path
     */
    ListenerDescriptor(ListenerType type, String destination, String handlerPath) {
        this.type = type;
        this.destination = destination;
        this.handlerPath = handlerPath;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    ListenerType getType() {
        return type;
    }

    /**
     * Gets the destination.
     *
     * @return the destination
     */
    String getDestination() {
        return destination;
    }

    /**
     * Gets the handler path.
     *
     * @return the handler path
     */
    String getHandlerPath() {
        return handlerPath;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Listener{" + "type=" + type + ", destination='" + destination + '\'' + ", handlerPath='" + handlerPath + '\'' + '}';
    }

    /**
     * Equals.
     *
     * @param o the o
     * @return true, if successful
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ListenerDescriptor listenerDescriptor = (ListenerDescriptor) o;
        return type == listenerDescriptor.type && Objects.equals(destination, listenerDescriptor.destination)
                && Objects.equals(handlerPath, listenerDescriptor.handlerPath);
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, destination, handlerPath);
    }
}

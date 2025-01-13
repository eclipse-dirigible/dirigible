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

import org.eclipse.dirigible.components.listeners.domain.ListenerKind;
import org.springframework.stereotype.Component;

/**
 * The Class ListenerCreator.
 */
@Component
class ListenerCreator {

    /** The destination name manager. */
    private final DestinationNameManager destinationNameManager;

    /**
     * Instantiates a new listener creator.
     *
     * @param destinationNameManager the destination name manager
     */
    ListenerCreator(DestinationNameManager destinationNameManager) {
        this.destinationNameManager = destinationNameManager;
    }

    /**
     * From entity.
     *
     * @param entity the entity
     * @return the listener descriptor
     */
    ListenerDescriptor fromEntity(org.eclipse.dirigible.components.listeners.domain.Listener entity) {
        ListenerType type = fromEntityType(entity.getKind());

        String destination = destinationNameManager.toTenantName(entity.getName());
        return new ListenerDescriptor(type, destination, entity.getHandler());
    }

    /**
     * From entity type.
     *
     * @param kind the kind
     * @return the listener type
     */
    private ListenerType fromEntityType(ListenerKind kind) {
        if (null == kind) {
            throw new IllegalArgumentException("Listener kind cannot be null");
        }
        return switch (kind) {
            case QUEUE -> ListenerType.QUEUE;
            case TOPIC -> ListenerType.TOPIC;
            default -> throw new IllegalArgumentException("Unsupported listener kind: " + kind);
        };
    }
}

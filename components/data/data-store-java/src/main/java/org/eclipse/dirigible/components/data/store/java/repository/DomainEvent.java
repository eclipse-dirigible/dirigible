/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

/**
 * A message a write wants published once — and only once — the write itself is durable.
 *
 * <p>
 * Instances are handed to the {@link JavaRepository} write methods, which record them in the
 * tenant's event outbox <em>inside the write's own transaction</em>. The row and its events
 * therefore commit or roll back together: a broker outage can no longer swallow an event whose row
 * exists, and it can no longer fail a call whose write already succeeded.
 *
 * <p>
 * Most writes publish a single event whose payload is the written row, so the repository derives it
 * from the entity and only needs the topic. This type is for the extra events a write emits about
 * <em>another</em> row — e.g. the {@code "-rekeyed"} event carrying the aggregate's previous state.
 *
 * @param topic the topic to publish on; must not be blank
 * @param payload the message body, typically the JSON of an entity
 */
public record DomainEvent(String topic, String payload) {

    /**
     * @param topic the topic to publish on; must not be blank
     * @param payload the message body, typically the JSON of an entity
     */
    public DomainEvent {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("A domain event needs a topic");
        }
    }
}

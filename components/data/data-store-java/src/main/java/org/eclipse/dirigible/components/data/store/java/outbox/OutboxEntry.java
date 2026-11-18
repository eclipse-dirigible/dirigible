/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.outbox;

/**
 * An event as the outbox recorded it, handed to every {@link OutboxRecordingParticipant} inside the
 * transaction that recorded it.
 *
 * @param id the outbox entry's identifier - unique per event, so a participant can use it as the
 *        event's stable identity
 * @param topic the topic the event is published on
 * @param payload the message body, typically the JSON of the written entity
 */
public record OutboxEntry(String id, String topic, String payload) {
}

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
 * One row of the event outbox: a message that is durably recorded but not yet acknowledged by the
 * broker.
 *
 * @param id the outbox row identifier
 * @param topic the topic to publish on
 * @param payload the message body
 * @param attempts how many delivery attempts the row has already survived; doubles as the
 *        optimistic lock the relay claims a row with
 * @param user the user whose action raised the event, or {@code null} when none was known (a job, a
 *        boot-time seed): the handlers' writes are attributed to it, see
 *        {@link org.eclipse.dirigible.components.base.context.OriginatingUser}
 */
record PendingEvent(String id, String topic, String payload, int attempts, String user) {
}

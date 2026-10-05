/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

/**
 * A message the application sent while a test ran - an entity event its repository published once
 * the write committed, or a message its own code sent. The slice has no broker: nothing receives
 * it, it is only recorded.
 *
 * @param destination the topic or queue it was sent to, e.g.
 *        {@code sample-intent-money-InvoiceLine-InvoiceLine}
 * @param payload the message body, for an entity event the row as JSON
 */
public record SentMessage(String destination, String payload) {
}

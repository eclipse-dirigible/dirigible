/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.messaging.monitoring.dto;

import java.util.Map;

/**
 * Browser representation of a JMS message. {@code body} carries the textual payload when the
 * message is a {@code TextMessage}; for other message types the field holds a hex/base64 preview,
 * while {@code bodyKind} identifies the underlying message class.
 */
public record MessageDetail(String id, String correlationId, String type, String bodyKind, String body, boolean bodyTruncated,
        long timestamp, long expiration, int priority, int redeliveryCounter, boolean persistent, long sizeBytes,
        Map<String, Object> properties) {
}

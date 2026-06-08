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

/**
 * Per-destination snapshot exposed to the browser. {@code type} is {@code "queue"} or
 * {@code "topic"}. Counters mirror the JMX {@code DestinationStatistics} bean.
 */
public record DestinationSummary(String name, String type, long size, long enqueueCount, long dequeueCount, long dispatchCount,
        long inflightCount, long expiredCount, long consumerCount, long producerCount, long memoryUsageBytes, long memoryUsageLimit) {
}

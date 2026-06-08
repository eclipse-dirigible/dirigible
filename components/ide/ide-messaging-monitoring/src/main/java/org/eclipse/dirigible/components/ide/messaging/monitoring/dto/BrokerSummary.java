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

import java.util.List;

/**
 * Full snapshot of the embedded ActiveMQ broker — broker meta plus every queue and topic. The
 * browser polls one endpoint to render the whole perspective.
 */
public record BrokerSummary(long timestamp, String brokerName, boolean started, List<DestinationSummary> queues,
        List<DestinationSummary> topics) {
}

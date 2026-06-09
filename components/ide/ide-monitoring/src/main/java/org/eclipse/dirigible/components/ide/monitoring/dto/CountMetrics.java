/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.monitoring.dto;

import java.util.List;

/**
 * Count-only runtime metrics (thread states, classloader, database pools, OS limits) rendered as
 * grouped name/value pairs by the Counters view in the Monitoring perspective.
 *
 * <p>
 * Each {@link NamedCount} carries an optional {@code max} so the UI can render "current / max"
 * style values (e.g. open file descriptors vs the OS limit, pool active vs pool size) without
 * needing a separate field per metric. {@code max} is {@code null} when no upper bound applies.
 */
public record CountMetrics(long timestamp, List<MetricGroup> groups) {

    public record MetricGroup(String title, List<NamedCount> items) {
    }

    public record NamedCount(String name, long value, Long max) {
    }
}

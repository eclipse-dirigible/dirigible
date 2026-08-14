/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.List;

/**
 * One node of the {@link LifecycleIntent state machine}: the statuses a record sitting in
 * {@code from} may move to.
 *
 * <pre>
 * - { from: ISSUED, to: [PAID, VOIDED] }
 * </pre>
 *
 * Authored by seeded name or seed id; the parser has resolved both to ids by the time this is read.
 */
public class LifecycleEdgeIntent {

    /** The SOURCE status - one edge entry per status, so the graph reads as a node list. */
    private Integer from;

    /** The statuses reachable from {@link #from}. */
    private List<Integer> to;

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public List<Integer> getTo() {
        return to == null ? List.of() : to;
    }

    public void setTo(List<Integer> to) {
        this.to = to;
    }
}

/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.perf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The statements the current thread executed since {@link #begin()}, by SQL text. Thread-bound on
 * purpose: a servlet request runs on one thread from the first filter to the response, so what is
 * counted is exactly what the request itself paid for - a posting or a roll-up recompute that a
 * listener runs after the commit, on another thread, is not part of it.
 */
final class StatementCounter {

    /** The key a batch execution is counted under: it carries no SQL of its own. */
    private static final String BATCH = "(batch)";

    private static final ThreadLocal<Map<String, Integer>> CURRENT = new ThreadLocal<>();

    private StatementCounter() {}

    /** Starts counting on the current thread. */
    static void begin() {
        CURRENT.set(new HashMap<>());
    }

    /**
     * Stops counting on the current thread.
     *
     * @return the statements executed since {@link #begin()}, by SQL text
     */
    static Map<String, Integer> end() {
        Map<String, Integer> statements = CURRENT.get();
        CURRENT.remove();
        return statements == null ? Map.of() : statements;
    }

    /**
     * Counts one execution, when the current thread is counting.
     *
     * @param sql the statement's SQL, null for a batch
     */
    static void record(String sql) {
        Map<String, Integer> statements = CURRENT.get();
        if (statements != null) {
            statements.merge(Objects.requireNonNullElse(sql, BATCH), 1, Integer::sum);
        }
    }
}

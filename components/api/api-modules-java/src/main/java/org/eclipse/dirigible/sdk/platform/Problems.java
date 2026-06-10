/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.platform;

import java.util.List;
import org.eclipse.dirigible.components.api.platform.ProblemsFacade;

/**
 * Surfaces the platform's "problems" table — the source of truth for build, synchronization, and
 * validation failures the IDE Problems perspective renders. Use this from custom synchronizers,
 * validation jobs, or compile-time tooling to record entries that should reach the developer.
 * <p>
 * Each entry pins a location ({@code project/file:line:column}) plus a severity / category, so IDE
 * navigation works without further wiring. {@link #updateStatus(Long, String)} is the mechanism for
 * marking a problem resolved without deleting it (preserving history for audits).
 */
public final class Problems {

    private Problems() {}

    public static void add(String location, String type, String line, String column, String cause, String expected, String category,
            String module, String source, String program) {
        ProblemsFacade.save(location, type, line, column, cause, expected, category, module, source, program);
    }

    public static String fetchAll() {
        return ProblemsFacade.fetchAllProblems();
    }

    public static String fetchBatch(String condition, int limit) {
        return ProblemsFacade.fetchProblemsBatch(condition, limit);
    }

    public static String find(Long id) {
        return ProblemsFacade.findProblem(id);
    }

    public static void delete(Long id) {
        ProblemsFacade.deleteProblem(id);
    }

    public static void deleteMultiple(List<Long> ids) {
        ProblemsFacade.deleteMultipleProblemsById(ids);
    }

    public static void deleteByStatus(String status) {
        ProblemsFacade.deleteAllByStatus(status);
    }

    public static void clear() {
        ProblemsFacade.clearAllProblems();
    }

    public static void updateStatus(Long id, String status) {
        ProblemsFacade.updateStatus(id, status);
    }

    public static void updateStatusMultiple(List<Long> ids, String status) {
        ProblemsFacade.updateStatusMultiple(ids, status);
    }
}

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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What one step of {@link PerformanceBaselineIT} measured: the client-side latency of each of its
 * requests, the statements each request executed, and how busy the connection pools got meanwhile.
 *
 * @param step the step's name
 * @param latenciesMillis the latency of each request
 * @param statements the statements of each request, by SQL text
 * @param pools the peak usage of each connection pool while the step ran, by data source name
 */
record StepMeasurement(String step, List<Double> latenciesMillis, List<Map<String, Integer>> statements, Map<String, PoolPeak> pools) {

    /** How many of the most repeated statements a report names. */
    private static final int REPEATED_STATEMENTS_SHOWN = 5;

    /** How much of a statement's SQL a report shows - enough to name its table and predicate. */
    private static final int SQL_SHOWN = 160;

    /**
     * The peak usage of one connection pool.
     *
     * @param maximumPoolSize the pool's configured maximum size
     * @param active the most connections in use at once
     * @param awaiting the most threads waiting for a connection at once
     */
    record PoolPeak(int maximumPoolSize, int active, int awaiting) {
    }

    /** @return the median latency */
    double p50Millis() {
        return percentile(latenciesMillis, 50);
    }

    /** @return the 95th percentile latency */
    double p95Millis() {
        return percentile(latenciesMillis, 95);
    }

    /**
     * The statements a typical request of the step executes - the median, so a first request that
     * filled a cache does not speak for the other 49.
     *
     * @return the median statement count per request
     */
    int statementsPerRequest() {
        return (int) percentile(totals(), 50);
    }

    /** @return the statement count of the costliest request */
    int maxStatementsPerRequest() {
        return (int) percentile(totals(), 100);
    }

    /**
     * The statements a typical request repeats most - where an N+1 shows up by name.
     *
     * @return {@code <count>x <sql>} lines of the median request, most repeated first, the SQL
     *         abbreviated
     */
    List<String> mostRepeatedStatements() {
        int median = statementsPerRequest();
        return statements.stream()
                         .filter(request -> total(request) == median)
                         .findFirst()
                         .orElse(Map.of())
                         .entrySet()
                         .stream()
                         .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                                          .thenComparing(Map.Entry.comparingByKey()))
                         .limit(REPEATED_STATEMENTS_SHOWN)
                         .map(entry -> entry.getValue() + "x " + abbreviated(entry.getKey()))
                         .collect(Collectors.toList());
    }

    /** One line, a query's column list elided: its table and predicate are what name an N+1. */
    private static String abbreviated(String sql) {
        String line = sql.replaceAll("\\s+", " ")
                         .strip();
        int from = line.toLowerCase(Locale.ROOT)
                       .indexOf(" from ");
        if (line.regionMatches(true, 0, "select ", 0, 7) && from > 0) {
            line = "select ..." + line.substring(from);
        }
        return line.length() <= SQL_SHOWN ? line : line.substring(0, SQL_SHOWN) + "...";
    }

    private List<Double> totals() {
        return statements.stream()
                         .map(request -> (double) total(request))
                         .toList();
    }

    private static int total(Map<String, Integer> request) {
        return request.values()
                      .stream()
                      .mapToInt(Integer::intValue)
                      .sum();
    }

    /** Nearest-rank percentile. */
    private static double percentile(List<Double> values, int percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Double> sorted = values.stream()
                                    .sorted()
                                    .toList();
        int rank = (int) Math.ceil(percentile / 100.0 * sorted.size());
        return sorted.get(Math.max(rank, 1) - 1);
    }
}

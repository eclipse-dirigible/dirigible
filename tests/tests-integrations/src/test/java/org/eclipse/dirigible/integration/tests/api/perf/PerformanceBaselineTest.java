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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PerformanceBaselineTest {

    /** The p50 / p95 of the 2026-11-04 recording, made on a fast runner. */
    private static final String BASELINE = """
            {
              "steps": {
                "listPage": { "p50Millis": 202.4, "p95Millis": 292.6, "statements": 8 },
                "search": { "p50Millis": 196.6, "p95Millis": 249.2, "statements": 8 },
                "create": { "p50Millis": 193.8, "p95Millis": 351.3, "statements": 14 },
                "transition": { "p50Millis": 203.1, "p95Millis": 360.6, "statements": 14 },
                "print": { "p50Millis": 460.3, "p95Millis": 553.9, "statements": 7 },
                "inbox": { "p50Millis": 646.4, "p95Millis": 764.0, "statements": 10 }
              }
            }
            """;

    private PerformanceBaseline baseline;

    @BeforeEach
    void readBaseline(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("PERF_BASELINE.json");
        Files.writeString(file, BASELINE, StandardCharsets.UTF_8);
        baseline = PerformanceBaseline.read(file);
    }

    @Test
    void aSlowerRunnerIsNotARegression() {
        // the nightly of 2026-11-07: four steps beyond 1.5x their recorded p95, nothing executed
        // differently
        List<StepMeasurement> nightly =
                List.of(step("listPage", 281.3, 454.4, 8), step("search", 300.0, 375.9, 8), step("create", 352.8, 531.9, 14),
                        step("transition", 355.0, 554.3, 14), step("print", 554.8, 737.4, 7), step("inbox", 822.8, 1121.7, 10));

        assertThat(baseline.machineFactor(nightly)).isBetween(1.4, 1.5);
        assertThat(baseline.regressions(nightly)).isEmpty();
    }

    @Test
    void oneStepSlowerThanTheRestIsARegression() {
        List<StepMeasurement> run =
                List.of(step("listPage", 281.3, 454.4, 8), step("search", 300.0, 375.9, 8), step("create", 352.8, 531.9, 14),
                        step("transition", 355.0, 554.3, 14), step("print", 554.8, 737.4, 7), step("inbox", 1650.0, 2300.0, 10));

        assertThat(baseline.regressions(run)).singleElement()
                                             .asString()
                                             .startsWith("[inbox] p95 2300.0 ms exceeds the baseline 764.0 ms");
    }

    @Test
    void everyStepSlowerBeyondTheCeilingIsARegression() {
        List<StepMeasurement> run =
                List.of(step("listPage", 610.0, 880.0, 8), step("search", 590.0, 750.0, 8), step("create", 580.0, 1050.0, 14),
                        step("transition", 610.0, 1080.0, 14), step("print", 1380.0, 1660.0, 7), step("inbox", 1940.0, 2290.0, 10));

        assertThat(baseline.machineFactor(run)).isGreaterThan(PerformanceBaseline.MACHINE_FACTOR_CEILING);
        assertThat(baseline.regressions(run)).first()
                                             .asString()
                                             .startsWith("every step is slower");
    }

    @Test
    void aFasterRunnerDoesNotTightenTheGate() {
        List<StepMeasurement> run =
                List.of(step("listPage", 150.0, 430.0, 8), step("search", 140.0, 240.0, 8), step("create", 150.0, 340.0, 14),
                        step("transition", 150.0, 350.0, 14), step("print", 300.0, 540.0, 7), step("inbox", 500.0, 760.0, 10));

        assertThat(baseline.machineFactor(run)).isEqualTo(1.0);
        assertThat(baseline.regressions(run)).isEmpty();
    }

    @Test
    void aStatementMoreThanTheBaselineIsARegressionOnAnyMachine() {
        List<StepMeasurement> run = List.of(step("listPage", 281.3, 454.4, 9), step("search", 300.0, 375.9, 8));

        assertThat(baseline.regressions(run)).singleElement()
                                             .asString()
                                             .startsWith("[listPage] 9 statements per request, the baseline is 8");
    }

    /** A step whose 20 requests have the given p50 and p95 (nearest rank) and statement count. */
    private static StepMeasurement step(String name, double p50, double p95, int statements) {
        List<Double> latencies = IntStream.range(0, 20)
                                          .mapToObj(i -> i < 10 ? p50 : p95)
                                          .toList();
        List<Map<String, Integer>> requests = IntStream.range(0, 20)
                                                       .mapToObj(i -> Map.of("select 1", statements))
                                                       .toList();
        return new StepMeasurement(name, latencies, requests, Map.of());
    }
}

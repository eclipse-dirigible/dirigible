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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * {@code tests/PERF_BASELINE.json}: per step, the p50 / p95 latency and the statements per request
 * a release measured, and the platform defaults the measurement ran with.
 *
 * <p>
 * The release workflow writes it ({@code -Dperf.baseline.record=true}) and commits it with the
 * development version; every other run is measured against it. A step fails when its p95 exceeds
 * the baseline's, scaled by the run's {@linkplain #machineFactor machine factor}, by more than
 * {@link #P95_TOLERANCE}, or when a typical request of it executes more statements than the
 * baseline's - a statement count does not depend on the machine, so it is held exactly, and it is
 * the number an N+1 moves first. A value the file does not carry is not gated: a baseline holding
 * statement counts alone gates statements alone.
 *
 * <p>
 * The baseline is one run on a shared CI runner, and runners of the same label differ in speed by
 * up to 2x: a baseline recorded on a fast one failed every later run on an ordinary one, each step
 * about 1.5x its p95 with no change in what it executes. So the latencies are compared relative to
 * the machine: the factor is the median, over the steps, of the measured p50 against the
 * baseline's, which a uniformly slower runner moves and a regression of one step does not. A factor
 * beyond {@link #MACHINE_FACTOR_CEILING} is itself reported, so a slowdown of every step still
 * fails.
 */
final class PerformanceBaseline {

    /** How far a step's p95 may exceed the baseline's, after the machine factor: 50 %. */
    static final double P95_TOLERANCE = 1.5;

    /**
     * How much slower than the baseline's runner a run may be as a whole before that is reported - the
     * runners measured so far differ by up to 1.6x.
     */
    static final double MACHINE_FACTOR_CEILING = 2.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
                                                      .create();

    private final Map<String, JsonObject> steps;

    private PerformanceBaseline(Map<String, JsonObject> steps) {
        this.steps = steps;
    }

    /**
     * Reads a baseline.
     *
     * @param file the baseline file
     * @return the baseline, empty when the file does not exist
     */
    static PerformanceBaseline read(Path file) {
        Map<String, JsonObject> steps = new LinkedHashMap<>();
        if (Files.exists(file)) {
            try {
                JsonObject baseline = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                                                .getAsJsonObject();
                for (Map.Entry<String, JsonElement> step : baseline.getAsJsonObject("steps")
                                                                   .entrySet()) {
                    steps.put(step.getKey(), step.getValue()
                                                 .getAsJsonObject());
                }
            } catch (IOException ex) {
                throw new UncheckedIOException("Cannot read the performance baseline [" + file + "]", ex);
            }
        }
        return new PerformanceBaseline(steps);
    }

    /**
     * Writes the measurements as the new baseline.
     *
     * @param file the baseline file
     * @param measurements the measured steps
     * @param environment the platform defaults the measurement ran with
     */
    static void write(Path file, List<StepMeasurement> measurements, JsonObject environment) {
        JsonObject baseline = new JsonObject();
        baseline.addProperty("recordedAt", Instant.now()
                                                  .toString());
        baseline.add("environment", environment);
        JsonObject steps = new JsonObject();
        for (StepMeasurement measurement : measurements) {
            JsonObject step = new JsonObject();
            step.addProperty("p50Millis", rounded(measurement.p50Millis()));
            step.addProperty("p95Millis", rounded(measurement.p95Millis()));
            step.addProperty("statements", measurement.statementsPerRequest());
            steps.add(measurement.step(), step);
        }
        baseline.add("steps", steps);
        try {
            Files.writeString(file, GSON.toJson(baseline) + "\n", StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot write the performance baseline [" + file + "]", ex);
        }
    }

    /**
     * What the measurements exceed the baseline by.
     *
     * @param measurements the measured steps of one run
     * @return one sentence per exceeded value, empty when the run is within its baseline
     */
    List<String> regressions(List<StepMeasurement> measurements) {
        List<String> regressions = new ArrayList<>();
        double machineFactor = machineFactor(measurements);
        if (machineFactor > MACHINE_FACTOR_CEILING) {
            regressions.add(String.format("every step is slower: the median p50 is %.2fx the baseline's, more than %.1fx", machineFactor,
                    MACHINE_FACTOR_CEILING));
        }
        for (StepMeasurement measurement : measurements) {
            regressions.addAll(regressions(measurement, Math.min(machineFactor, MACHINE_FACTOR_CEILING)));
        }
        return regressions;
    }

    /**
     * How much slower the machine of this run is than the baseline's: the median, over the steps the
     * baseline carries a p50 for, of the measured p50 against the baseline's - never below 1, so a
     * faster runner does not tighten the gate.
     *
     * @param measurements the measured steps of one run
     * @return the factor the baseline's latencies are scaled by
     */
    double machineFactor(List<StepMeasurement> measurements) {
        List<Double> ratios = new ArrayList<>();
        for (StepMeasurement measurement : measurements) {
            JsonObject step = steps.get(measurement.step());
            if (step != null && step.has("p50Millis") && step.get("p50Millis")
                                                             .getAsDouble() > 0) {
                ratios.add(measurement.p50Millis() / step.get("p50Millis")
                                                         .getAsDouble());
            }
        }
        return Math.max(1.0, median(ratios));
    }

    private List<String> regressions(StepMeasurement measurement, double machineFactor) {
        List<String> regressions = new ArrayList<>();
        JsonObject step = steps.get(measurement.step());
        if (step == null) {
            return regressions;
        }
        if (step.has("p95Millis")) {
            double expected = step.get("p95Millis")
                                  .getAsDouble()
                    * machineFactor;
            if (measurement.p95Millis() > expected * P95_TOLERANCE) {
                regressions.add(String.format(
                        "[%s] p95 %.1f ms exceeds the baseline %.1f ms (%.1f ms at this run's machine factor %.2f) by more than 50 %%",
                        measurement.step(), measurement.p95Millis(), step.get("p95Millis")
                                                                         .getAsDouble(),
                        expected, machineFactor));
            }
        }
        if (step.has("statements")) {
            int allowed = step.get("statements")
                              .getAsInt();
            if (measurement.statementsPerRequest() > allowed) {
                regressions.add(String.format("[%s] %d statements per request, the baseline is %d - most repeated: %s", measurement.step(),
                        measurement.statementsPerRequest(), allowed, measurement.mostRepeatedStatements()));
            }
        }
        return regressions;
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 1.0;
        }
        List<Double> sorted = values.stream()
                                    .sorted()
                                    .toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    private static double rounded(double millis) {
        return Math.round(millis * 10) / 10.0;
    }
}

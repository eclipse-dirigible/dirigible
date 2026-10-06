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
 * the baseline's by more than {@link #P95_TOLERANCE}, or when a typical request of it executes more
 * statements than the baseline's - a statement count does not depend on the machine, so it is held
 * exactly, and it is the number an N+1 moves first. A value the file does not carry is not gated: a
 * baseline holding statement counts alone gates statements alone.
 */
final class PerformanceBaseline {

    /** How far a step's p95 may exceed the baseline's: 50 %. */
    static final double P95_TOLERANCE = 1.5;

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
     * What the measurement of a step exceeds the baseline by.
     *
     * @param measurement the measured step
     * @return one sentence per exceeded value, empty when the step is within its baseline
     */
    List<String> regressions(StepMeasurement measurement) {
        List<String> regressions = new ArrayList<>();
        JsonObject step = steps.get(measurement.step());
        if (step == null) {
            return regressions;
        }
        if (step.has("p95Millis")) {
            double allowed = step.get("p95Millis")
                                 .getAsDouble()
                    * P95_TOLERANCE;
            if (measurement.p95Millis() > allowed) {
                regressions.add(String.format("[%s] p95 %.1f ms exceeds the baseline %.1f ms by more than 50 %%", measurement.step(),
                        measurement.p95Millis(), step.get("p95Millis")
                                                     .getAsDouble()));
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

    private static double rounded(double millis) {
        return Math.round(millis * 10) / 10.0;
    }
}

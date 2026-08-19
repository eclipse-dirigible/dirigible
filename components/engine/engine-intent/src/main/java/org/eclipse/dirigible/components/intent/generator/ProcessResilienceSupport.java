/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.ProcessVarIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * Declarative step resilience on a {@code delegate} service task: {@code retry: { count, every }}
 * (a Flowable failed-job retry cycle on the generated task), {@code onError: <step | end>} (an
 * error boundary event routed like a decision branch), the {@code {error}} placeholder (the failure
 * message, published as the {@link #ERROR_MESSAGE_VARIABLE} process variable when the exhausted
 * failure converts to the BPMN error), and the declared step data ({@code vars:} +
 * {@code produces:}/{@code uses:}, with {@code clearAfter} removing a value once its step
 * completes).
 *
 * <p>
 * The reading helpers here are shared by the parser's validation and the BPMN generator's emission,
 * so what is validated is exactly what is emitted. The error code and the message variable are
 * string contracts with the runtime conversion in {@code engine-bpm-flowable} (which turns a
 * delegate's final failed attempt into a caught BPMN error) - kept as literals on both sides, like
 * the {@code ${JavaTask}} delegate-expression contract.
 */
public final class ProcessResilienceSupport {

    /** The {@code <error>} definition id emitted once per process that declares an onError route. */
    public static final String ERROR_ID = "intentStepError";

    /**
     * The BPMN error code the runtime raises for a delegate's final failed attempt - the contract
     * between the generated boundary event and {@code engine-bpm-flowable}'s conversion.
     */
    public static final String ERROR_CODE = "INTENT_STEP_FAILED";

    /**
     * The process variable carrying the failure message, set by the runtime conversion just before it
     * raises the BPMN error - what a {@code setField} step's {@code {error}} value reads.
     */
    public static final String ERROR_MESSAGE_VARIABLE = "__errorMessage";

    /** The {@code setField} value token resolving to the failure message on an error route. */
    public static final String ERROR_TOKEN = "{error}";

    private ProcessResilienceSupport() {}

    /** The {@code retry} map of a step, or null when absent or not map-shaped. */
    public static Map<?, ?> retry(StepIntent step) {
        Object raw = step.getArgs() == null ? null
                : step.getArgs()
                      .get("retry");
        return raw instanceof Map<?, ?> map ? map : null;
    }

    /**
     * The {@code retry.count} as a whole number, or null when absent or not one. The YAML round-trip
     * keeps integers integral ({@code Long}), so anything rendering with a fraction is refused.
     */
    public static Integer retryCount(StepIntent step) {
        Map<?, ?> retry = retry(step);
        Object count = retry == null ? null : retry.get("count");
        if (count == null || !count.toString()
                                   .matches("\\d+")) {
            return null;
        }
        try {
            return Integer.valueOf(count.toString());
        } catch (NumberFormatException moreDigitsThanAnIntHolds) {
            return null;
        }
    }

    /** The {@code retry.every} ISO-8601 duration text, or null. */
    public static String retryEvery(StepIntent step) {
        Map<?, ?> retry = retry(step);
        Object every = retry == null ? null : retry.get("every");
        return every == null || every.toString()
                                     .isBlank() ? null
                                             : every.toString()
                                                    .trim();
    }

    /**
     * The Flowable {@code failedJobRetryTimeCycle} for a step's {@code retry}, or null when the step
     * declares none. {@code R<n>} is n TOTAL attempts (verified against Flowable's JobRetryCmd), and
     * {@code count} is how many FURTHER attempts follow the first, so the cycle is
     * {@code R<count+1>/<every>}.
     */
    public static String retryCycle(StepIntent step) {
        Integer count = retryCount(step);
        String every = retryEvery(step);
        return count == null || every == null ? null : "R" + (count + 1) + "/" + every;
    }

    /** The {@code onError} routing target of a step, or null. */
    public static String onError(StepIntent step) {
        Object raw = step.getArgs() == null ? null
                : step.getArgs()
                      .get("onError");
        return raw == null || raw.toString()
                                 .isBlank() ? null
                                         : raw.toString()
                                              .trim();
    }

    /** The error boundary event id for a step ({@code <step>Error}). */
    public static String errorBoundaryId(String stepName) {
        return stepName + "Error";
    }

    /** Whether any step of the process declares an {@code onError} route. */
    public static boolean hasErrorRouting(ProcessIntent process) {
        for (StepIntent step : process.getSteps()) {
            if (onError(step) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every step reachable from some {@code onError} target through its own routing - the steps on
     * which {@code {error}} is resolvable, since only the error boundary's conversion sets the message
     * variable.
     */
    public static Set<String> errorReachableSteps(ProcessIntent process) {
        Map<String, StepIntent> byName = new LinkedHashMap<>();
        for (StepIntent step : process.getSteps()) {
            if (step.getName() != null) {
                byName.put(step.getName(), step);
            }
        }
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        for (StepIntent step : process.getSteps()) {
            String target = onError(step);
            if (target != null) {
                pending.add(target);
            }
        }
        while (!pending.isEmpty()) {
            String name = pending.poll();
            StepIntent step = byName.get(name);
            if (step == null || !reachable.add(name)) {
                continue; // a literal (`end` / `join`), an undeclared target, or an already-walked step
            }
            pending.addAll(ProcessParallelSupport.routingTargets(step));
        }
        return reachable;
    }

    /**
     * The declared variables to clear per step ({@code clearAfter} -> var names, declaration order) -
     * what the generator turns into {@code event="end"} execution listeners on that step's element.
     */
    public static Map<String, List<String>> clearsByStep(ProcessIntent process) {
        Map<String, List<String>> clears = new LinkedHashMap<>();
        for (ProcessVarIntent var : process.getVars()) {
            if (var.getName() == null || var.getName()
                                            .isBlank()
                    || var.getClearAfter() == null || var.getClearAfter()
                                                         .isBlank()) {
                continue;
            }
            clears.computeIfAbsent(var.getClearAfter()
                                      .trim(),
                    step -> new java.util.ArrayList<>())
                  .add(var.getName()
                          .trim());
        }
        return clears;
    }
}

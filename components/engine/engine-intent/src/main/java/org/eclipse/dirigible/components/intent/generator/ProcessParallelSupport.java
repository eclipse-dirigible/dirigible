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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * A {@code kind: parallel} process step - {@code args: { branches: [stepA, stepB], next: join }} -
 * runs its declared branch steps concurrently and joins before {@code next}. It is emitted as a
 * BPMN <b>parallel gateway pair</b>: a diverging {@code parallelGateway} (the fork step itself)
 * fans an unconditioned flow to each branch, and a synthesized converging {@code parallelGateway}
 * (id {@code <fork>Join}) waits for every branch before the single flow on to {@code next}.
 *
 * <p>
 * The branch steps and the join gateway are <b>off the default linear chain</b> - like a decision's
 * {@code then}/{@code else} targets - so the generator excludes them from the linear sequence and
 * wires the fork/branch/join flows explicitly.
 *
 * <p>
 * v1 scope: each branch is a <b>single</b> declared step that joins directly, and forks do not
 * nest. Multi-step branch chains and nested parallels are a documented follow-up (see the module
 * guide).
 */
public final class ProcessParallelSupport {

    /** The id suffix of the synthesized converging join gateway. */
    public static final String JOIN_SUFFIX = "Join";

    private ProcessParallelSupport() {}

    /**
     * A parallel fork and its synthesized join.
     *
     * @param forkId the diverging {@code parallelGateway} id (the {@code parallel} step's name)
     * @param branches the step names run concurrently between the fork and the join, in declared order
     * @param next the step (or {@code end}) the join flows into, or {@code null} when unset
     * @param joinId the synthesized converging {@code parallelGateway} id
     */
    public record Fork(String forkId, List<String> branches, String next, String joinId) {
    }

    /** The synthesized converging join gateway id for a fork step. */
    public static String joinId(String forkId) {
        return forkId + JOIN_SUFFIX;
    }

    /** The parallel forks declared across a step list, in declaration order. */
    public static List<Fork> forks(List<StepIntent> steps) {
        List<Fork> forks = new ArrayList<>();
        for (StepIntent step : steps) {
            if (!"parallel".equalsIgnoreCase(step.getKind()) || step.getName() == null) {
                continue;
            }
            Map<String, Object> args = step.getArgs() == null ? Map.of() : step.getArgs();
            List<String> branches = new ArrayList<>();
            if (args.get("branches") instanceof List<?> list) {
                for (Object branch : list) {
                    if (branch != null) {
                        branches.add(branch.toString());
                    }
                }
            }
            Object next = args.get("next");
            String nextStep = next == null ? null
                    : next.toString()
                          .trim();
            forks.add(new Fork(step.getName(), branches, nextStep == null || nextStep.isEmpty() ? null : nextStep, joinId(step.getName())));
        }
        return forks;
    }

    /** Every branch step name across the given forks (excluded from the linear chain). */
    public static Set<String> branchNames(List<Fork> forks) {
        Set<String> names = new LinkedHashSet<>();
        for (Fork fork : forks) {
            names.addAll(fork.branches());
        }
        return names;
    }

    /** Every synthesized join gateway id across the given forks (excluded from the linear chain). */
    public static Set<String> joinIds(List<Fork> forks) {
        Set<String> ids = new LinkedHashSet<>();
        for (Fork fork : forks) {
            ids.add(fork.joinId());
        }
        return ids;
    }
}

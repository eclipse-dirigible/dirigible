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
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * A {@code kind: parallel} process step - {@code args: { branches: [stepA, stepB], next: join }} -
 * runs its declared branches concurrently and joins before {@code next}. It is emitted as a BPMN
 * <b>parallel gateway pair</b>: a diverging {@code parallelGateway} (the fork step itself) fans an
 * unconditioned flow to each branch, and a synthesized converging {@code parallelGateway} (id
 * {@code <fork>Join}) waits for every branch before the single flow on to {@code next}.
 *
 * <p>
 * A branch is a <b>chain</b>, not a single step: it starts at the declared branch step and
 * continues through that step's own routing ({@code next}, a decision's {@code then}/{@code else},
 * a boundary {@code timeout}/{@code expire} branch), and may itself be a nested {@code parallel}.
 * Every step reachable that way forms the <b>branch region</b>; a step in a region that declares no
 * routing is a <b>terminal</b> and flows into its enclosing join, and the literal target
 * {@link #JOIN_TARGET} converges on that join explicitly (needed when a decision inside a branch
 * must rejoin from both arms). Region steps - like a decision's {@code then}/{@code else} targets -
 * are <b>off the default linear chain</b>, so the generator wires their flows explicitly.
 *
 * <p>
 * The structural analysis here is shared with {@code IntentParser}, so the definition of a branch
 * region has exactly one implementation: what the parser validates is what the generator emits.
 */
public final class ProcessParallelSupport {

    /** The id suffix of the synthesized converging join gateway. */
    public static final String JOIN_SUFFIX = "Join";

    /** The routing literal a step inside a branch uses to converge on its enclosing join gateway. */
    public static final String JOIN_TARGET = "join";

    private ProcessParallelSupport() {}

    /**
     * A parallel fork and its synthesized join.
     *
     * @param forkId the diverging {@code parallelGateway} id (the {@code parallel} step's name)
     * @param branches the steps each branch chain starts at, in declared order
     * @param next the step (or {@code end}) the join flows into, or {@code null} when unset (a nested
     *        fork then joins into its own enclosing join)
     * @param joinId the synthesized converging {@code parallelGateway} id
     */
    public record Fork(String forkId, List<String> branches, String next, String joinId) {
    }

    /**
     * Which steps lie inside which branch of which fork.
     *
     * @param joinByStep every step inside a branch region, mapped to its <b>innermost</b> enclosing
     *        join gateway id (walk order, so iteration is deterministic)
     * @param branchByStep the same steps mapped to the branch that claims them
     *        ({@code <fork>/<branch>})
     * @param shared steps claimed by more than one branch - an authoring error, since a step reached by
     *        two concurrent tokens would run twice and leave the join waiting
     */
    public record Regions(Map<String, String> joinByStep, Map<String, String> branchByStep, Set<String> shared) {

        /** Whether the step lies inside some parallel branch. */
        public boolean contains(String step) {
            return joinByStep.containsKey(step);
        }

        /** The innermost join gateway enclosing the step, or {@code null} when it is on the main flow. */
        public String joinOf(String step) {
            return joinByStep.get(step);
        }

        /** Every step inside a branch region, in walk order. */
        public Set<String> steps() {
            return joinByStep.keySet();
        }
    }

    /** The synthesized converging join gateway id for a fork step. */
    public static String joinId(String forkId) {
        return forkId + JOIN_SUFFIX;
    }

    /** The parallel forks declared across a step list, in declaration order. */
    public static List<Fork> forks(List<StepIntent> steps) {
        List<Fork> forks = new ArrayList<>();
        for (StepIntent step : steps) {
            if (!isParallel(step) || step.getName() == null) {
                continue;
            }
            List<String> branches = new ArrayList<>();
            if (args(step).get("branches") instanceof List<?> list) {
                for (Object branch : list) {
                    if (branch != null) {
                        branches.add(branch.toString());
                    }
                }
            }
            forks.add(new Fork(step.getName(), branches, stringArg(step, "next"), joinId(step.getName())));
        }
        return forks;
    }

    /** Every synthesized join gateway id across the given forks (excluded from the linear chain). */
    public static Set<String> joinIds(List<Fork> forks) {
        Set<String> ids = new LinkedHashSet<>();
        for (Fork fork : forks) {
            ids.add(fork.joinId());
        }
        return ids;
    }

    /**
     * The branch regions of a step list: from every fork's declared branches, walk each step's routing
     * targets and claim what is reachable. A nested {@code parallel} is claimed by the enclosing branch
     * and the walk continues past it at its {@code next} - the nested fork's own branches are claimed
     * by its own walk, so the innermost fork always wins.
     */
    public static Regions regions(List<StepIntent> steps) {
        Map<String, StepIntent> byName = byName(steps);
        Map<String, String> joinByStep = new LinkedHashMap<>();
        Map<String, String> branchByStep = new LinkedHashMap<>();
        Set<String> shared = new LinkedHashSet<>();
        for (Fork fork : forks(steps)) {
            for (String head : fork.branches()) {
                String branch = fork.forkId() + "/" + head;
                Deque<String> pending = new ArrayDeque<>();
                pending.add(head);
                while (!pending.isEmpty()) {
                    String name = pending.poll();
                    StepIntent step = byName.get(name);
                    if (step == null) {
                        continue; // a literal (`end` / `join`) or an undeclared target - the parser reports it
                    }
                    String owner = branchByStep.get(name);
                    if (owner != null) {
                        if (!owner.equals(branch)) {
                            shared.add(name);
                        }
                        continue; // already claimed - by this branch (a loop back) or by another one
                    }
                    branchByStep.put(name, branch);
                    joinByStep.put(name, fork.joinId());
                    pending.addAll(routingTargets(step));
                }
            }
        }
        return new Regions(joinByStep, branchByStep, shared);
    }

    /**
     * The steps a step routes on to: a decision's {@code then} and {@code else}, any other step's
     * {@code next}, the {@code then} of a user task's {@code timeout} / {@code expire} boundary timer,
     * plus a delegate service task's {@code onError} error route. A {@code parallel} step's branches
     * are deliberately NOT routing targets - they belong to the fork's own branches, not to the chain
     * the fork sits on.
     */
    public static List<String> routingTargets(StepIntent step) {
        List<String> targets = new ArrayList<>(2);
        if ("decision".equalsIgnoreCase(step.getKind())) {
            addIfPresent(targets, stringArg(step, "then"));
            addIfPresent(targets, stringArg(step, "else"));
        } else {
            addIfPresent(targets, stringArg(step, "next"));
        }
        addIfPresent(targets, timerTarget(step, "timeout"));
        addIfPresent(targets, timerTarget(step, "expire"));
        addIfPresent(targets, stringArg(step, "onError"));
        return targets;
    }

    /** Whether the step is a parallel fork. */
    public static boolean isParallel(StepIntent step) {
        return "parallel".equalsIgnoreCase(step.getKind());
    }

    private static void addIfPresent(List<String> targets, String target) {
        if (target != null) {
            targets.add(target);
        }
    }

    /** The {@code then} branch of a boundary timer arg ({@code timeout} / {@code expire}), or null. */
    private static String timerTarget(StepIntent step, String timer) {
        if (args(step).get(timer) instanceof Map<?, ?> map) {
            Object then = map.get("then");
            if (then != null && !then.toString()
                                     .isBlank()) {
                return then.toString()
                           .trim();
            }
        }
        return null;
    }

    private static Map<String, StepIntent> byName(List<StepIntent> steps) {
        Map<String, StepIntent> byName = new LinkedHashMap<>();
        for (StepIntent step : steps) {
            if (step.getName() != null) {
                byName.put(step.getName(), step);
            }
        }
        return byName;
    }

    private static Map<String, Object> args(StepIntent step) {
        return step.getArgs() == null ? Map.of() : step.getArgs();
    }

    private static String stringArg(StepIntent step, String name) {
        Object value = args(step).get(name);
        if (value == null) {
            return null;
        }
        String text = value.toString()
                           .trim();
        return text.isEmpty() ? null : text;
    }
}

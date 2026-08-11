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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.generator.ProcessParallelSupport.Fork;
import org.eclipse.dirigible.components.intent.generator.ProcessParallelSupport.Regions;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.junit.jupiter.api.Test;

/**
 * Extracting parallel forks + the synthesized join id from a step list (#6556), and the branch
 * regions a multi-step / nested branch spans (#6568).
 */
class ProcessParallelSupportTest {

    private static StepIntent step(String name, String kind, Map<String, Object> args) {
        StepIntent step = new StepIntent();
        step.setName(name);
        step.setKind(kind);
        if (args != null) {
            step.setArgs(args);
        }
        return step;
    }

    @Test
    void extractsForkBranchesNextAndSynthesizedJoin() {
        List<StepIntent> steps = List.of(
                step("reviews", "parallel", Map.of("branches", List.of("techReview", "commercialReview"), "next", "consolidate")),
                step("techReview", "userTask", null), step("commercialReview", "userTask", null), step("consolidate", "serviceTask", null));

        List<Fork> forks = ProcessParallelSupport.forks(steps);
        assertEquals(1, forks.size());
        Fork fork = forks.get(0);
        assertEquals("reviews", fork.forkId());
        assertEquals(List.of("techReview", "commercialReview"), fork.branches());
        assertEquals("consolidate", fork.next());
        assertEquals("reviewsJoin", fork.joinId());

        assertTrue(ProcessParallelSupport.joinIds(forks)
                                         .contains("reviewsJoin"));
    }

    @Test
    void noParallelYieldsNoForks() {
        assertTrue(ProcessParallelSupport.forks(List.of(step("confirm", "userTask", null), step("done", "end", null)))
                                         .isEmpty());
    }

    @Test
    void aBranchRegionIsTheWholeChainReachableFromTheBranchStep() {
        // techReview -> techSignoff (its `next`); the second branch is a single step. `consolidate` is
        // the fork's `next`, on the main flow - a region must not swallow it.
        List<StepIntent> steps =
                List.of(step("reviews", "parallel", Map.of("branches", List.of("techReview", "commercialReview"), "next", "consolidate")),
                        step("techReview", "userTask", Map.of("next", "techSignoff")), step("techSignoff", "serviceTask", null),
                        step("commercialReview", "userTask", null), step("consolidate", "serviceTask", null));

        Regions regions = ProcessParallelSupport.regions(steps);

        assertEquals(Set.of("techReview", "techSignoff", "commercialReview"), regions.steps());
        assertEquals("reviewsJoin", regions.joinOf("techSignoff"));
        assertFalse(regions.contains("consolidate"), "the fork's `next` stays on the main flow");
        assertFalse(regions.contains("reviews"), "a top-level fork is not inside a branch itself");
        assertTrue(regions.shared()
                          .isEmpty());
    }

    @Test
    void aNestedForkOwnsItsOwnBranchesAndTheOuterChainResumesAtItsNext() {
        List<StepIntent> steps = List.of(step("reviews", "parallel", Map.of("branches", List.of("tech", "commercial"), "next", "done")),
                step("tech", "userTask", null),
                step("commercial", "parallel", Map.of("branches", List.of("pricing", "legal"), "next", "sign")),
                step("pricing", "userTask", null), step("legal", "userTask", null), step("sign", "serviceTask", null),
                step("done", "serviceTask", null));

        Regions regions = ProcessParallelSupport.regions(steps);

        // The nested fork is claimed by the outer branch; its own branches are claimed by IT, so the
        // innermost join always wins.
        assertEquals("reviewsJoin", regions.joinOf("commercial"));
        assertEquals("commercialJoin", regions.joinOf("pricing"));
        assertEquals("commercialJoin", regions.joinOf("legal"));
        // The outer branch continues after the nested join, not through the nested branches.
        assertEquals("reviewsJoin", regions.joinOf("sign"));
        assertTrue(regions.shared()
                          .isEmpty());
    }

    @Test
    void aStepReachableFromTwoBranchesIsReportedAsShared() {
        List<StepIntent> steps = List.of(step("reviews", "parallel", Map.of("branches", List.of("tech", "commercial"), "next", "done")),
                step("tech", "userTask", Map.of("next", "sign")), step("commercial", "userTask", Map.of("next", "sign")),
                step("sign", "serviceTask", null), step("done", "serviceTask", null));

        assertEquals(Set.of("sign"), ProcessParallelSupport.regions(steps)
                                                           .shared());
    }

    @Test
    void aLoopBackInsideOneBranchIsNotShared() {
        // A decision looping back to its own branch's task is legitimate - and must not walk forever.
        List<StepIntent> steps = List.of(step("reviews", "parallel", Map.of("branches", List.of("review", "legal"), "next", "done")),
                step("review", "userTask", Map.of("next", "check")),
                step("check", "decision", Map.of("if", "ok == 1", "then", "join", "else", "review")), step("legal", "userTask", null),
                step("done", "serviceTask", null));

        Regions regions = ProcessParallelSupport.regions(steps);

        assertEquals(Set.of("review", "check", "legal"), regions.steps());
        assertTrue(regions.shared()
                          .isEmpty());
    }
}

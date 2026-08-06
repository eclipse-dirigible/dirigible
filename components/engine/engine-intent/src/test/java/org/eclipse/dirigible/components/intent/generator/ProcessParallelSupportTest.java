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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.ProcessParallelSupport.Fork;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.junit.jupiter.api.Test;

/** Extracting parallel forks + the synthesized join id from a step list (#6556). */
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

        assertTrue(ProcessParallelSupport.branchNames(forks)
                                         .containsAll(List.of("techReview", "commercialReview")));
        assertTrue(ProcessParallelSupport.joinIds(forks)
                                         .contains("reviewsJoin"));
    }

    @Test
    void noParallelYieldsNoForks() {
        assertTrue(ProcessParallelSupport.forks(List.of(step("confirm", "userTask", null), step("done", "end", null)))
                                         .isEmpty());
    }
}

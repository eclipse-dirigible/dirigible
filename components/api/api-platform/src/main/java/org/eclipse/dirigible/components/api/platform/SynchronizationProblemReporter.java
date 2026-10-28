/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactProblemReporter;
import org.eclipse.dirigible.components.ide.problems.domain.Problem;
import org.eclipse.dirigible.components.ide.problems.service.ProblemService;
import org.springframework.stereotype.Component;

/**
 * Keeps the IDE Problems view in step with the artefacts the synchronizers could not reconcile: it
 * records a problem for each failed artefact and removes the ones whose artefact has healed on a
 * later pass or whose source file was deleted. Backed by {@link ProblemsFacade}'s synchronization
 * problem store, keyed by the artefact's location and type under the
 * {@link ProblemsFacade#CATEGORY_SYNCHRONIZATION} category.
 */
@Component
class SynchronizationProblemReporter implements ArtefactProblemReporter {

    /** {@code Problem.cause} is a {@code VARCHAR(1024)} column. */
    private static final int MAX_CAUSE_LENGTH = 1024;

    private final ProblemService problemService;

    SynchronizationProblemReporter(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Override
    public void reconcileSynchronizationProblems(Collection<? extends Artefact> failedArtefacts) {
        Set<String> failedKeys = new HashSet<>();
        for (Artefact artefact : failedArtefacts) {
            failedKeys.add(key(artefact.getLocation(), artefact.getType()));
        }
        // Clear the problem of any artefact no longer failing - healed on a later pass, or its source
        // removed - i.e. anything under the synchronization category the current failed set no longer
        // names.
        List<Problem> existing = problemService.findByCategory(ProblemsFacade.CATEGORY_SYNCHRONIZATION);
        for (Problem problem : existing) {
            if (!failedKeys.contains(key(problem.getLocation(), problem.getType()))) {
                problemService.deleteByLocationAndTypeAndCategory(problem.getLocation(), problem.getType(),
                        ProblemsFacade.CATEGORY_SYNCHRONIZATION);
            }
        }
        // Record or refresh a problem for each currently failing artefact, with its recorded cause.
        for (Artefact artefact : failedArtefacts) {
            ProblemsFacade.upsertArtefactSynchronizationProblem(artefact, truncate(artefact.getError()));
        }
    }

    private static String key(String location, String type) {
        return location + '\n' + type;
    }

    private static String truncate(String cause) {
        if (cause == null) {
            return "";
        }
        return cause.length() > MAX_CAUSE_LENGTH ? cause.substring(0, MAX_CAUSE_LENGTH) : cause;
    }
}

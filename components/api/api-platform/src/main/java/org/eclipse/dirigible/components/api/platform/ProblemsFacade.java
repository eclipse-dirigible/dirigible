/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import java.util.List;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.ide.problems.domain.Problem;
import org.eclipse.dirigible.components.ide.problems.service.ProblemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class ProblemsFacade.
 */
@Component
public class ProblemsFacade implements InitializingBean {

    /** The instance. */
    private static ProblemsFacade INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(ProblemsFacade.class);

    /** The problem service. */
    private ProblemService problemService;

    /**
     * Instantiates a new problem facade.
     *
     * @param problemService the problem service
     */
    @Autowired
    public ProblemsFacade(ProblemService problemService) {
        this.problemService = problemService;
    }

    /**
     * After properties set.
     */
    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the problem facade
     */
    public static ProblemsFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the problem service.
     *
     * @return the problem service
     */
    public ProblemService getProblemService() {
        return problemService;
    }

    /**
     * Save.
     *
     * @param location the location
     * @param type the type
     * @param line the line
     * @param column the column
     * @param cause the cause
     * @param expected the expected
     * @param category the category
     * @param module the module
     * @param source the source
     * @param program the program
     */
    public static final void save(String location, String type, String line, String column, String cause, String expected, String category,
            String module, String source, String program) {
        Problem problem = new Problem(location, type, line, column, cause, expected, category, module, source, program);
        ProblemsFacade.get()
                      .getProblemService()
                      .save(problem);
    }

    /**
     * Get Artefact Synchronization Problem.
     *
     * @param artefact the artefact
     *
     * @return the problem if any
     */
    public static final Problem getArtefactSynchronizationProblem(Artefact artefact) {
        List<Problem> results = ProblemsFacade.get()
                                              .getProblemService()
                                              .findByLocationAndTypeAndCategory(artefact.getLocation(), artefact.getType(),
                                                      "Synchronization");
        return results.size() > 0 ? results.get(0) : null;
    }

    /**
     * Save Artefact Synchronization Problem.
     *
     * @param artefact the artefact
     * @param errorMessage the errorMessage
     */
    public static final void saveArtefactSynchronizationProblem(Artefact artefact, String errorMessage) {
        save(artefact.getLocation(), artefact.getType(), "", "", errorMessage, "", "Synchronization", "", "", "");
    }

    /**
     * Update Artefact Synchronization Problem.
     *
     * @param artefact the artefact
     * @param errorMessage the errorMessage
     */
    public static final void updateArtefactSynchronizationProblem(Artefact artefact, String errorMessage) {
        ProblemsFacade.get()
                      .getProblemService()
                      .updateCauseByLocationAndTypeAndCategory(errorMessage, artefact.getLocation(), artefact.getType(), "Synchronization");
    }

    /**
     * Upsert Artefact Synchronization Problem.
     *
     * @param artefact the artefact
     * @param errorMessage the errorMessage
     */
    public static final void upsertArtefactSynchronizationProblem(Artefact artefact, String errorMessage) {
        try {
            Problem problem = ProblemsFacade.getArtefactSynchronizationProblem(artefact);
            if (problem != null) {
                ProblemsFacade.updateArtefactSynchronizationProblem(artefact, errorMessage);
            } else {
                ProblemsFacade.saveArtefactSynchronizationProblem(artefact, errorMessage);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error occurred while upserting artefact synchronization problem", e);
            }
        }
    }

    /**
     * Delete Artefact Synchronization Problem.
     *
     * @param artefact the artefact
     */
    public static final void deleteArtefactSynchronizationProblem(Artefact artefact) {
        ProblemsFacade.get()
                      .getProblemService()
                      .deleteByLocationAndTypeAndCategory(artefact.getLocation(), artefact.getType(), "Synchronization");
    }

    /**
     * Find problem.
     *
     * @param id the id
     * @return the string
     */
    public static final String findProblem(Long id) {
        return GsonHelper.toJson(ProblemsFacade.get()
                                               .getProblemService()
                                               .findById(id));
    }

    /**
     * Fetch all problems.
     *
     * @return the string
     */
    public static final String fetchAllProblems() {
        return GsonHelper.toJson(ProblemsFacade.get()
                                               .getProblemService()
                                               .getAll());
    }

    /**
     * Fetch problems batch.
     *
     * @param condition the condition
     * @param limit the limit
     * @return the string
     */
    public static final String fetchProblemsBatch(String condition, int limit) {
        return GsonHelper.toJson(ProblemsFacade.get()
                                               .getProblemService()
                                               .fetchProblemsBatch(condition, limit));
    }

    /**
     * Delete problem.
     *
     * @param id the id
     */
    public static final void deleteProblem(Long id) {
        ProblemsFacade.get()
                      .getProblemService()
                      .deleteById(id);
    }

    /**
     * Delete multiple problems by id.
     *
     * @param ids the ids
     */
    public static final void deleteMultipleProblemsById(List<Long> ids) {
        ProblemsFacade.get()
                      .getProblemService()
                      .deleteAllByIds(ids);
    }

    /**
     * Delete all by status.
     *
     * @param status the status
     */
    public static final void deleteAllByStatus(String status) {
        ProblemsFacade.get()
                      .getProblemService()
                      .deleteAllByStatus(status);
    }

    /**
     * Clear all problems.
     */
    public static final void clearAllProblems() {
        ProblemsFacade.get()
                      .getProblemService()
                      .deleteAll();
    }

    /**
     * Update status.
     *
     * @param id the id
     * @param status the status
     */
    public static final void updateStatus(Long id, String status) {
        ProblemsFacade.get()
                      .getProblemService()
                      .updateStatusById(id, status);
    }

    /**
     * Update status multiple.
     *
     * @param ids the ids
     * @param status the status
     */
    public static final void updateStatusMultiple(List<Long> ids, String status) {
        ProblemsFacade.get()
                      .getProblemService()
                      .updateStatusByIds(ids, status);
    }
}

/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.problems.service;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.ide.problems.domain.Problem;
import org.eclipse.dirigible.components.ide.problems.domain.Problems;
import org.eclipse.dirigible.components.ide.problems.repository.ProblemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ProblemService.
 */
@Service
@Transactional
public class ProblemService {

    /** The problem repository. */
    @Autowired
    private ProblemRepository problemRepository;

    /**
     * Gets the all.
     *
     * @return the all
     */
    @Transactional(readOnly = true)
    public List<Problem> getAll() {
        return problemRepository.findAll();
    }

    /**
     * Find all.
     *
     * @param pageable the pageable
     * @return the page
     */
    @Transactional(readOnly = true)
    public Page<Problem> getPages(Pageable pageable) {
        return problemRepository.findAll(pageable);
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the problem
     */
    @Transactional(readOnly = true)
    public Problem findById(Long id) {
        Optional<Problem> problem = problemRepository.findById(id);
        if (problem.isPresent()) {
            return problem.get();
        } else {
            throw new IllegalArgumentException("Problem with id does not exist: " + id);
        }
    }

    /**
     * Find by location, type and category.
     *
     * @param location the location
     * @param type the type
     * @param category the category
     *
     * @return the problems
     */
    @Transactional(readOnly = true)
    public List<Problem> findByLocationAndTypeAndCategory(String location, String type, String category) {
        return problemRepository.findProblemsByLocationAndTypeAndCategory(location, type, category);
    }

    /**
     * Update cause by location, type and category.
     *
     * @param cause the cause
     * @param location the location
     * @param type the type
     * @param category the category
     */
    @Transactional(readOnly = false)
    public void updateCauseByLocationAndTypeAndCategory(String cause, String location, String type, String category) {
        problemRepository.updateProblemsCauseByLocationAndTypeAndCategory(cause, location, type, category);
    }

    /**
     * Delete by location, type and category.
     *
     * @param location the location
     * @param type the type
     * @param category the category
     */
    @Transactional(readOnly = false)
    public void deleteByLocationAndTypeAndCategory(String location, String type, String category) {
        problemRepository.deleteProblemsByLocationAndTypeAndCategory(location, type, category);
    }

    /**
     * Save.
     *
     * @param problem the problem
     * @return the problem
     */
    public Problem save(Problem problem) {
        return problemRepository.saveAndFlush(problem);
    }

    /**
     * Delete.
     *
     * @param problem the problem
     */
    public void delete(Problem problem) {
        problemRepository.delete(problem);
    }

    /**
     * Delete.
     *
     */
    public void deleteAll() {
        problemRepository.deleteAll();
    }

    /**
     * Delete by id.
     *
     * @param id the id
     */
    public void deleteById(Long id) {
        problemRepository.deleteById(id);
    }

    /**
     * Delete by ids.
     *
     * @param ids the ids
     */
    public void deleteAllByIds(List<Long> ids) {
        problemRepository.deleteAllById(ids);
    }

    /**
     * Delete all by status.
     *
     * @param status the status
     */
    public void deleteAllByStatus(String status) {
        Problem filter = new Problem();
        filter.setStatus(status);
        Example<Problem> example = Example.of(filter);
        List<Problem> problems = problemRepository.findAll(example);
        problems.forEach(p -> deleteById(p.getId()));
    }

    /**
     * Update status by id.
     *
     * @param id the id
     * @param status the status
     */
    public void updateStatusById(Long id, String status) {
        Problem filter = new Problem();
        filter.setStatus(status);
        Example<Problem> example = Example.of(filter);
        Optional<Problem> problem = problemRepository.findOne(example);
        if (problem.isPresent()) {
            Problem existing = problem.get();
            existing.setStatus(status);
            save(existing);
        }
    }

    /**
     * Update status by ids.
     *
     * @param id the id
     * @param status the status
     */
    public void updateStatusByIds(List<Long> id, String status) {
        Problem filter = new Problem();
        filter.setStatus(status);
        Example<Problem> example = Example.of(filter);
        List<Problem> problems = problemRepository.findAll(example);
        problems.forEach(p -> {
            p.setStatus(status);
            save(p);
        });
    }

    /**
     * Fetch problems batch.
     *
     * @param condition the condition
     * @param limit the limit
     * @return the list
     */
    public Problems fetchProblemsBatch(String condition, int limit) {
        List<Problem> result = problemRepository.findProblemsByConditionAndLimit(condition, limit);
        return new Problems(result, result.size(), (int) problemRepository.count());
    }
}

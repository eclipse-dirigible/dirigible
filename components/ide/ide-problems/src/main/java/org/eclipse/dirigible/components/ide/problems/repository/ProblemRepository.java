/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.problems.repository;

import java.util.List;

import org.eclipse.dirigible.components.ide.problems.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * The Interface ProblemRepository.
 */
@Repository("problemRepository")
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    /**
     * Fetch problems by location, type and category.
     *
     * @param location the location
     * @param type the type
     * @param category the category
     *
     * @return the list
     */
    @Query(value = "SELECT * FROM DIRIGIBLE_PROBLEMS " + "WHERE " + "PROBLEM_LOCATION = :location " + "AND PROBLEM_TYPE = :type "
            + "AND PROBLEM_CATEGORY = :category ", nativeQuery = true)
    List<Problem> findProblemsByLocationAndTypeAndCategory(@Param("location") String location, @Param("type") String type,
            @Param("category") String category);

    /**
     * Update problems by location, type amd category.
     *
     * @param cause the cause
     * @param location the location
     * @param type the type
     * @param category the category
     */
    @Modifying
    @Query(value = "UPDATE DIRIGIBLE_PROBLEMS " + "SET PROBLEM_CAUSE = :cause " + "WHERE " + "PROBLEM_LOCATION = :location "
            + "AND PROBLEM_TYPE = :type " + "AND PROBLEM_CATEGORY = :category ", nativeQuery = true)
    void updateProblemsCauseByLocationAndTypeAndCategory(@Param("cause") String cause, @Param("location") String location,
            @Param("type") String type, @Param("category") String category);

    /**
     * Delete problems by location, type amd category.
     *
     * @param location the location
     * @param type the type
     * @param category the category
     */
    @Modifying
    @Query(value = "DELETE FROM DIRIGIBLE_PROBLEMS " + "WHERE " + "PROBLEM_LOCATION = :location " + "AND PROBLEM_TYPE = :type "
            + "AND PROBLEM_CATEGORY = :category ", nativeQuery = true)
    void deleteProblemsByLocationAndTypeAndCategory(@Param("location") String location, @Param("type") String type,
            @Param("category") String category);

    /**
     * Fetch problems batch.
     *
     * @param condition the condition
     * @param limit the limit
     *
     * @return the list
     */
    @Query(value = "SELECT * FROM DIRIGIBLE_PROBLEMS " + "WHERE " + "PROBLEM_LOCATION LIKE :condition " + "OR PROBLEM_TYPE LIKE :condition "
            + "OR PROBLEM_LINE LIKE :condition " + "OR PROBLEM_COLUMN LIKE :condition " + "OR PROBLEM_CAUSE LIKE :condition "
            + "OR PROBLEM_CREATED_BY LIKE :condition " + "OR PROBLEM_CATEGORY LIKE :condition " + "OR PROBLEM_MODULE LIKE :condition "
            + "OR PROBLEM_SOURCE LIKE :condition " + "OR PROBLEM_PROGRAM LIKE :condition " + "OR PROBLEM_STATUS LIKE :condition "
            + "LIMIT :limit", nativeQuery = true)
    List<Problem> findProblemsByConditionAndLimit(@Param("condition") String condition, @Param("limit") int limit);

}

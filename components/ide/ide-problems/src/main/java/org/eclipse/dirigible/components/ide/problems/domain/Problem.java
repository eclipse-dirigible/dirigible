/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.problems.domain;

import java.sql.Timestamp;

import org.eclipse.dirigible.components.api.security.UserFacade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class Problem.
 */
@Entity
@Table(name = "DIRIGIBLE_PROBLEMS")
public class Problem {

    /**
     * Always on insert.
     */
    public static final String ACTIVE = "ACTIVE";
    /**
     * Marked by the user as solved.
     */
    public static final String SOLVED = "SOLVED";
    /**
     * Marked by the user as ignored.
     */
    public static final String IGNORED = "IGNORED";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROBLEM_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The location. */
    @Column(name = "PROBLEM_LOCATION", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String location;

    /** The type. */
    @Column(name = "PROBLEM_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 32)
    private String type;

    /** The line. */
    @Column(name = "PROBLEM_LINE", columnDefinition = "VARCHAR", nullable = false, length = 11)
    private String line;

    /** The column. */
    @Column(name = "PROBLEM_COLUMN", columnDefinition = "VARCHAR", nullable = false, length = 11)
    private String column;

    /** The cause. */
    @Column(name = "PROBLEM_CAUSE", columnDefinition = "VARCHAR", nullable = false, length = 1024)
    private String cause;

    /** The expected. */
    @Column(name = "PROBLEM_EXPECTED", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String expected;

    /** The created at. */
    @Column(name = "PROBLEM_CREATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Timestamp createdAt;

    /** The created by. */
    @Column(name = "PROBLEM_CREATED_BY", columnDefinition = "VARCHAR", nullable = false, length = 32)
    private String createdBy;

    /** The category. */
    @Column(name = "PROBLEM_CATEGORY", columnDefinition = "VARCHAR", nullable = false, length = 32)
    private String category;

    /** The module. */
    @Column(name = "PROBLEM_MODULE", columnDefinition = "VARCHAR", nullable = false, length = 32)
    private String module;

    /** The source. */
    @Column(name = "PROBLEM_SOURCE", columnDefinition = "VARCHAR", nullable = false, length = 32)
    private String source;

    /** The program. */
    @Column(name = "PROBLEM_PROGRAM", columnDefinition = "VARCHAR", nullable = false, length = 32)
    private String program;

    /** The status. */
    @Column(name = "PROBLEM_STATUS", columnDefinition = "VARCHAR", nullable = false, length = 8)
    private String status;

    /**
     * Instantiates a new problem.
     */
    public Problem() {}

    /**
     * Instantiates a new problem.
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
    public Problem(String location, String type, String line, String column, String cause, String expected, String category, String module,
            String source, String program) {
        this.location = location;
        this.type = type;
        this.line = line;
        this.column = column;
        this.cause = cause;
        this.expected = expected;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.createdBy = UserFacade.getName();
        this.category = category;
        this.module = module;
        this.source = source;
        this.program = program;
        this.status = ACTIVE;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the location.
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location.
     *
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the line.
     *
     * @return the line
     */
    public String getLine() {
        return line;
    }

    /**
     * Sets the line.
     *
     * @param line the line to set
     */
    public void setLine(String line) {
        this.line = line;
    }

    /**
     * Gets the column.
     *
     * @return the column
     */
    public String getColumn() {
        return column;
    }

    /**
     * Sets the column.
     *
     * @param column the column to set
     */
    public void setColumn(String column) {
        this.column = column;
    }

    /**
     * Gets the cause.
     *
     * @return the cause
     */
    public String getCause() {
        return cause;
    }

    /**
     * Sets the cause.
     *
     * @param cause the cause to set
     */
    public void setCause(String cause) {
        this.cause = cause;
    }

    /**
     * Gets the expected.
     *
     * @return the expected
     */
    public String getExpected() {
        return expected;
    }

    /**
     * Sets the expected.
     *
     * @param expected the expected to set
     */
    public void setExpected(String expected) {
        this.expected = expected;
    }

    /**
     * Gets the created at.
     *
     * @return the createdAt
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the created at.
     *
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the created by.
     *
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the created by.
     *
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the category.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category.
     *
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the module.
     *
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * Sets the module.
     *
     * @param module the module to set
     */
    public void setModule(String module) {
        this.module = module;
    }

    /**
     * Gets the source.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source.
     *
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the program.
     *
     * @return the program
     */
    public String getProgram() {
        return program;
    }

    /**
     * Sets the program.
     *
     * @param program the program to set
     */
    public void setProgram(String program) {
        this.program = program;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Problem [id=" + id + ", location=" + location + ", type=" + type + ", line=" + line + ", column=" + column + ", cause="
                + cause + ", expected=" + expected + ", createdAt=" + createdAt + ", createdBy=" + createdBy + ", category=" + category
                + ", module=" + module + ", source=" + source + ", program=" + program + ", status=" + status + "]";
    }

}

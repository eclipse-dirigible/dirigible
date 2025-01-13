/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.problems.endpoint;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.eclipse.dirigible.components.ide.problems.domain.Problem;
import org.eclipse.dirigible.components.ide.problems.service.ProblemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ProblemsEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class ProblemsEndpointTest {

    /** The problem service. */
    @Autowired
    private ProblemService problemService;

    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    void setup() throws Exception {

        cleanup();

        problemService.save(createProblem("location1", "type1", "line1", "column1", "cause1", "expected1", "category1", "module1",
                "source1", "program1"));
        problemService.save(createProblem("location2", "type2", "line2", "column2", "cause2", "expected2", "category2", "module2",
                "source2", "program2"));

    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    void cleanup() throws Exception {

    }

    /**
     * Find all problems.
     */
    @Test
    void findAllProblems() {
        Integer size = 10;
        Integer page = 0;
        Pageable pageable = PageRequest.of(page, size);
        assertNotNull(problemService.getPages(pageable));
    }

    /**
     * Gets the problems.
     *
     * @return the problems
     * @throws Exception the exception
     */
    @Test
    void getProblems() throws Exception {
        mockMvc.perform(get("/services/ide/problems"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the problems by condition.
     *
     * @return the problems by condition
     * @throws Exception the exception
     */
    @Test
    void getProblemsByCondition() throws Exception {
        mockMvc.perform(get("/services/ide/problems/search?condition=co&limit=5"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Creates the problem.
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
     * @return the problem
     */
    public static Problem createProblem(String location, String type, String line, String column, String cause, String expected,
            String category, String module, String source, String program) {
        return new Problem(location, type, line, column, cause, expected, category, module, source, program);
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}

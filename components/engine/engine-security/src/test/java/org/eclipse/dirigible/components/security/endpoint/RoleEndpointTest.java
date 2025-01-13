/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.endpoint;

import org.eclipse.dirigible.components.security.repository.RoleRepository;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.eclipse.dirigible.components.security.repository.RoleRepositoryTest.createSecurityRole;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Class RoleEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RoleRepository.class})
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class RoleEndpointTest {

    /** The role repository. */
    @Autowired
    private RoleRepository roleRepository;

    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /** The spring security filter chain. */
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {

        cleanup();

        // Create test security roles
        roleRepository.save(createSecurityRole("/a/b/c/test1.role", "test1", "description"));
        roleRepository.save(createSecurityRole("/a/b/c/test2.role", "test2", "description"));
        roleRepository.save(createSecurityRole("/a/b/c/test3.role", "test3", "description"));
        roleRepository.save(createSecurityRole("/a/b/c/test4.role", "test4", "description"));
        roleRepository.save(createSecurityRole("/a/b/c/test5.role", "test5", "description"));
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // Delete test security roles
        roleRepository.deleteAll();
    }

    /**
     * Test get security roles.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetSecurityRoles() throws Exception {
        mockMvc.perform(get("/services/security/roles"))
               .andDo(print())
               .andExpect(status().isOk());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}

/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.extensions.domain.ExtensionPoint;
import org.eclipse.dirigible.components.extensions.repository.ExtensionPointRepository;
import org.eclipse.dirigible.components.extensions.repository.ExtensionPointRepositoryTest;
import org.eclipse.dirigible.components.extensions.service.ExtensionPointService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.persistence.EntityManager;

/**
 * The Class ExtensionPointEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class ExtensionPointEndpointTest {

    /** The entity manager. */
    @Autowired
    private EntityManager entityManager;

    /** The extension point service. */
    @Autowired
    private ExtensionPointService extensionPointService;

    /** The extension point repository. */
    @Autowired
    private ExtensionPointRepository extensionPointRepository;

    /** The test extension point. */
    private ExtensionPoint testExtensionPoint;

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
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {

        cleanup();

        // create test ExtensionPoints
        extensionPointService.save(ExtensionPointRepositoryTest.createExtensionPoint("/a/b/c/e1.extensionpoint", "e1", "description"));
        extensionPointService.save(ExtensionPointRepositoryTest.createExtensionPoint("/a/b/c/e2.extensionpoint", "e2", "description"));
        extensionPointService.save(ExtensionPointRepositoryTest.createExtensionPoint("/a/b/c/e3.extensionpoint", "e3", "description"));
        extensionPointService.save(ExtensionPointRepositoryTest.createExtensionPoint("/a/b/c/e4.extensionpoint", "e4", "description"));
        extensionPointService.save(ExtensionPointRepositoryTest.createExtensionPoint("/a/b/c/e5.extensionpoint", "e5", "description"));

        Page<ExtensionPoint> extensionPoints = extensionPointService.getPages(PageRequest.of(0, BaseEndpoint.DEFAULT_PAGE_SIZE));
        assertNotNull(extensionPoints);
        assertEquals(5L, extensionPoints.getTotalElements());

        testExtensionPoint = extensionPoints.getContent()
                                            .get(0);

        entityManager.refresh(testExtensionPoint);

    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        extensionPointRepository.deleteAll();
    }

    /**
     * Find all extension points.
     */
    @Test
    public void findAllExtensionPoints() {
        Integer size = 10;
        Integer page = 0;
        Pageable pageable = PageRequest.of(page, size);
        assertNotNull(extensionPointService.getPages(pageable));
    }

    /**
     * Gets the extension point by id.
     *
     * @return the extension point by id
     * @throws Exception the exception
     */
    @Test
    public void getExtensionPointById() throws Exception {
        Long id = testExtensionPoint.getId();

        mockMvc.perform(get("/services/core/extensionpoints/{id}", id))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the extension point by name.
     *
     * @return the extension point by name
     * @throws Exception the exception
     */
    @Test
    public void getExtensionPointByName() throws Exception {
        String name = testExtensionPoint.getName();

        mockMvc.perform(get("/services/core/extensionpoints/search?name={name}", name))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the pages extension points.
     *
     * @return the pages extension points
     * @throws Exception the exception
     */
    @Test
    public void getPagesExtensionPoints() throws Exception {
        String name = testExtensionPoint.getName();

        mockMvc.perform(get("/services/core/extensionpoints/pages", name))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the all extension points.
     *
     * @return the all extension points
     * @throws Exception the exception
     */
    @Test
    public void getAllExtensionPoints() throws Exception {
        String name = testExtensionPoint.getName();

        mockMvc.perform(get("/services/core/extensionpoints", name))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}

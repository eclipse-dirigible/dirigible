/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.openapi.service;

import static org.eclipse.dirigible.components.openapi.repository.OpenAPIRepositoryTest.createOpenAPI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.openapi.domain.OpenAPI;
import org.eclipse.dirigible.components.openapi.repository.OpenAPIRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class OpenAPIServiceTest.
 */
@SpringBootTest(classes = {OpenAPIRepository.class, OpenAPIService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class OpenAPIServiceTest {

    /** The open API repository. */
    @Autowired
    private OpenAPIRepository openAPIRepository;

    /** The open API service. */
    @Autowired
    private OpenAPIService openAPIService;

    @MockBean
    private TenantContext tenantContext;

    @MockBean
    @DefaultTenant
    private Tenant defaultTenant;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {

        cleanup();

        // Create test OpenAPIs
        openAPIRepository.save(createOpenAPI("/a/b/c/test1.openapi", "test1", "description"));
        openAPIRepository.save(createOpenAPI("/a/b/c/test2.openapi", "test2", "description"));
        openAPIRepository.save(createOpenAPI("/a/b/c/test3.openapi", "test3", "description"));
        openAPIRepository.save(createOpenAPI("/a/b/c/test4.openapi", "test4", "description"));
        openAPIRepository.save(createOpenAPI("/a/b/c/test5.openapi", "test5", "description"));
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // Delete test OpenAPIs
        openAPIRepository.deleteAll();
    }

    /**
     * Test get all.
     */
    @Test
    void testGetAll() {
        List<OpenAPI> openAPIList = openAPIService.getAll();
        assertEquals(5, openAPIList.size());
    }

    /**
     * Test find all.
     */
    @Test
    void testFindAll() {
        Page<OpenAPI> openApiPage = openAPIService.getPages(Pageable.ofSize(1));
        assertEquals(5, openApiPage.getTotalElements());
    }

    /**
     * Test find by id.
     */
    @Test
    void testFindById() {
        OpenAPI openAPI = new OpenAPI("/a/b/c/test.openapi", "test", "description");
        openAPIService.save(openAPI);
        OpenAPI openAPIServiceById = openAPIService.findById(openAPI.getId());
        assertEquals("test", openAPIServiceById.getName());
    }

    /**
     * Test find by name.
     */
    @Test
    void testFindByName() {
        OpenAPI openAPI = new OpenAPI("/a/b/c/test.openapi", "test", "description");
        openAPIService.save(openAPI);
        OpenAPI openAPIServiceByName = openAPIService.findByName("test");
        assertEquals(openAPI.getId(), openAPIServiceByName.getId());
    }

    /**
     * Test save.
     */
    @Test
    void testSave() {
        OpenAPI openAPI = new OpenAPI("/a/b/c/test.openapi", "test", "description");
        openAPIService.save(openAPI);
        assertNotNull(openAPIService.findByName("test"));
    }

    /**
     * Test delete.
     */
    @Test
    void testDelete() {
        OpenAPI openAPI = new OpenAPI("/a/b/c/test.openapi", "test", "description");
        openAPIService.save(openAPI);
        openAPIService.delete(openAPI);

        assertThrows(IllegalArgumentException.class, () -> {
            openAPIService.findByName("test");
        });
    }
}

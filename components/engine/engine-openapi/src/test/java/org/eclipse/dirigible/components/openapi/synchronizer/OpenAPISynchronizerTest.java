/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.openapi.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.openapi.domain.OpenAPI;
import org.eclipse.dirigible.components.openapi.repository.OpenAPIRepository;
import org.eclipse.dirigible.components.openapi.repository.OpenAPIRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class OpenAPISynchronizerTest.
 */
@SpringBootTest(classes = {OpenAPIRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class OpenAPISynchronizerTest {

    /**
     * The openapi repository.
     */
    @Autowired
    private OpenAPIRepository openAPIRepository;

    /**
     * The openapi synchronizer.
     */
    @Autowired
    private OpenAPISynchronizer openAPISynchronizer;

    /**
     * The entity manager.
     */
    @Autowired
    EntityManager entityManager;

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

        try {
            // Create test OpenAPIs
            openAPIRepository.save(OpenAPIRepositoryTest.createOpenAPI("/a/b/c/test1.openapi", "test1", "description"));
            openAPIRepository.save(OpenAPIRepositoryTest.createOpenAPI("/a/b/c/test2.openapi", "test2", "description"));
            openAPIRepository.save(OpenAPIRepositoryTest.createOpenAPI("/a/b/c/test3.openapi", "test3", "description"));
            openAPIRepository.save(OpenAPIRepositoryTest.createOpenAPI("/a/b/c/test4.openapi", "test4", "description"));
            openAPIRepository.save(OpenAPIRepositoryTest.createOpenAPI("/a/b/c/test5.openapi", "test5", "description"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        openAPIRepository.deleteAll();
    }

    /**
     * Checks if is accepted.
     */
    // @Test
    public void testIsAcceptedPath() {
        assertTrue(openAPISynchronizer.isAccepted(Path.of("/a/b/c/test.openapi"), null));
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void testIsAcceptedArtefact() {
        assertTrue(openAPISynchronizer.isAccepted(OpenAPIRepositoryTest.createOpenAPI("/a/b/c/test.openapi", "test", "description")
                                                                       .getType()));
    }

    /**
     * Load the artefact.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @Test
    public void testLoad() throws IOException, ParseException {
        byte[] content = OpenAPISynchronizer.class.getResourceAsStream("/META-INF/dirigible/test/test.openapi")
                                                  .readAllBytes();
        List<OpenAPI> list = openAPISynchronizer.parse("/META-INF/dirigible/test/test.openapi", content);
        assertNotNull(list);
        assertEquals("/META-INF/dirigible/test/test.openapi", list.get(0)
                                                                  .getLocation());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}

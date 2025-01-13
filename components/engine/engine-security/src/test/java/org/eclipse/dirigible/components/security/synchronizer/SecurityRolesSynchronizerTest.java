/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.synchronizer;

import jakarta.persistence.EntityManager;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.repository.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import static org.eclipse.dirigible.components.security.repository.RoleRepositoryTest.createSecurityRole;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class SecurityRoleSynchronizerTest.
 */
@SpringBootTest(classes = {RoleRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class SecurityRolesSynchronizerTest {
    /**
     * The entity manager.
     */
    @Autowired
    EntityManager entityManager;
    /**
     * The security role repository.
     */
    @Autowired
    private RoleRepository roleRepository;
    /**
     * The sercurity role synchronizer.
     */
    @Autowired
    private RolesSynchronizer rolesSynchronizer;


    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {

        cleanup();

        try {
            // Create test security accesses
            roleRepository.save(createSecurityRole("/a/b/c/test1.access", "test1", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test2.access", "test2", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test3.access", "test3", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test4.access", "test4", "description"));
            roleRepository.save(createSecurityRole("/a/b/c/test5.access", "test5", "description"));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * Checks if is accepted.
     */
    @Test
    public void testIsAcceptedPath() {
        assertTrue(rolesSynchronizer.isAccepted(Path.of("/a/b/c/test.roles"), null));
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void testIsAcceptedArtefact() {
        assertTrue(rolesSynchronizer.isAccepted(createSecurityRole("/a/b/c/test.roles", "test", "description").getType()));
    }

    /**
     * Load the artefact.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @Test
    public void testLoad() throws IOException, ParseException {
        byte[] content = SecurityRolesSynchronizerTest.class.getResourceAsStream("/META-INF/dirigible/test/test.roles")
                                                            .readAllBytes();
        List<Role> list = rolesSynchronizer.parse("/META-INF/dirigible/test/test.roles", content);
        assertNotNull(list);
        assertEquals("/META-INF/dirigible/test/test.roles", list.get(0)
                                                                .getLocation());
    }
}

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

import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.repository.AccessRepository;

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

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import static org.eclipse.dirigible.components.security.repository.AccessRepositoryTest.createSecurityAccess;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class AccessSynchronizerTest.
 */
@SpringBootTest(classes = {AccessRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class AccessSynchronizerTest {
    /**
     * The security access repository.
     */
    @Autowired
    private AccessRepository accessRepository;

    /**
     * The security access synchronizer.
     */
    @Autowired
    private AccessSynchronizer accessSynchronizer;

    /**
     * The entity manager.
     */
    @Autowired
    EntityManager entityManager;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {

        cleanup();

        try {
            // Create test security accesses
            accessRepository.save(createSecurityAccess("/a/b/c/test1.access", "test1", "description", "HTTP", "/a" + "/b/c/test1.txt",
                    "GET", "test_role_1"));
            accessRepository.save(createSecurityAccess("/a/b/c/test2.access", "test2", "description", "HTTP", "/a" + "/b/c/test2.txt",
                    "GET", "test_role_2"));
            accessRepository.save(createSecurityAccess("/a/b/c/test3.access", "test3", "description", "HTTP", "/a" + "/b/c/test3.txt",
                    "GET", "test_role_3"));
            accessRepository.save(createSecurityAccess("/a/b/c/test4.access", "test4", "description", "HTTP", "/a" + "/b/c/test4.txt",
                    "GET", "test_role_4"));
            accessRepository.save(createSecurityAccess("/a/b/c/test5.access", "test5", "description", "HTTP", "/a" + "/b/c/test5.txt",
                    "GET", "test_role_5"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // Delete test security accesses
        accessRepository.deleteAll();
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void testIsAcceptedPath() {
        assertTrue(accessSynchronizer.isAccepted(Path.of("/a/b/c/test.access"), null));
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void testIsAcceptedArtefact() {
        assertTrue(accessSynchronizer.isAccepted(createSecurityAccess("/a/b/c/test.access", "test", "description", "HTTP",
                "/a/b/c/test.txt", "GET", "test_role").getType()));
    }

    /**
     * Load the artefact.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @Test
    public void testLoad() throws IOException, ParseException {
        byte[] content = AccessSynchronizerTest.class.getResourceAsStream("/META-INF/dirigible/test/test.access")
                                                     .readAllBytes();
        List<Access> list = accessSynchronizer.parse("/META-INF/dirigible/test/test.access", content);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        assertEquals("/META-INF/dirigible/test/test.access", list.get(0)
                                                                 .getLocation());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}

/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.service;

import static org.eclipse.dirigible.components.security.repository.AccessRepositoryTest.createSecurityAccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.repository.AccessRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class AccessServiceTest.
 */
@SpringBootTest(classes = {AccessRepository.class, AccessService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class AccessServiceTest {

    /** The access repository. */
    @Autowired
    private AccessRepository accessRepository;

    /** The access service. */
    @Autowired
    private AccessService accessService;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {

        cleanup();

        // Create test security accesses
        accessRepository.save(
                createSecurityAccess("/a/b/c/test1.access", "test1", "description", "HTTP", "/a" + "/b/c/test1.txt", "GET", "test_role_1"));
        accessRepository.save(
                createSecurityAccess("/a/b/c/test2.access", "test2", "description", "HTTP", "/a" + "/b/c/test2.txt", "GET", "test_role_2"));
        accessRepository.save(
                createSecurityAccess("/a/b/c/test3.access", "test3", "description", "HTTP", "/a" + "/b/c/test3.txt", "GET", "test_role_3"));
        accessRepository.save(
                createSecurityAccess("/a/b/c/test4.access", "test4", "description", "HTTP", "/a" + "/b/c/test4.txt", "GET", "test_role_4"));
        accessRepository.save(
                createSecurityAccess("/a/b/c/test5.access", "test5", "description", "HTTP", "/a" + "/b/c/test5.txt", "GET", "test_role_5"));
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
     * Test get all.
     */
    @Test
    void testGetAll() {
        List<Access> securityAccessList = accessService.getAll();
        assertEquals(5, securityAccessList.size());
    }

    /**
     * Test find all.
     */
    @Test
    void testFindAll() {
        Page<Access> securityAccessPage = accessService.getPages(Pageable.ofSize(1));
        assertEquals(5, securityAccessPage.getTotalElements());
    }

    /**
     * Test find by id.
     */
    @Test
    void testFindById() {
        Access securityAccess = new Access("/a/b/c/test.access", "test", "description", "HTTP", "/a/b" + "/c/test.txt", "GET", "test_role");
        accessService.save(securityAccess);
        Access securityAccessServiceById = accessService.findById(securityAccess.getId());
        assertEquals("test", securityAccessServiceById.getName());
    }

    /**
     * Test find by name.
     */
    @Test
    void testFindByName() {
        Access securityAccess = new Access("/a/b/c/test.access", "test", "description", "HTTP", "/a/b" + "/c/test.txt", "GET", "test_role");
        accessService.save(securityAccess);
        Access securityAccessServiceByName = accessService.findByName("test");
        assertEquals(securityAccess.getId(), securityAccessServiceByName.getId());
    }

    /**
     * Test save.
     */
    @Test
    void testSave() {
        Access securityAccess = new Access("/a/b/c/test.access", "test", "description", "HTTP", "/a/b" + "/c/test.txt", "GET", "test_role");
        accessService.save(securityAccess);
        assertNotNull(accessService.findByName("test"));
    }

    /**
     * Test delete.
     */
    @Test
    void testDelete() {
        Access securityAccess = new Access("/a/b/c/test.access", "test", "description", "HTTP", "/a/b/c/test.txt", "GET", "test_role");
        accessService.save(securityAccess);
        accessService.delete(securityAccess);

        assertThrows(IllegalArgumentException.class, () -> {
            accessService.findByName("test");
        });
    }
}

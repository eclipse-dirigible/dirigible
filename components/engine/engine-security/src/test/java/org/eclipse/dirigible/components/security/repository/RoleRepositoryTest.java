/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.repository;

import org.eclipse.dirigible.components.security.domain.Role;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class RoleRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class RoleRepositoryTest {

    /** The security role repository. */
    @Autowired
    private RoleRepository securityRoleRepository;

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        // Create test security roles
        securityRoleRepository.save(createSecurityRole("/a/b/c/test1.role", "test1", "description"));
        securityRoleRepository.save(createSecurityRole("/a/b/c/test2.role", "test2", "description"));
        securityRoleRepository.save(createSecurityRole("/a/b/c/test3.role", "test3", "description"));
        securityRoleRepository.save(createSecurityRole("/a/b/c/test4.role", "test4", "description"));
        securityRoleRepository.save(createSecurityRole("/a/b/c/test5.role", "test5", "description"));
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // Delete test security roles
        securityRoleRepository.findAll()
                              .stream()
                              .forEach(securityRole -> securityRoleRepository.delete(securityRole));
    }

    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = securityRoleRepository.findAll()
                                        .get(0)
                                        .getId();
        Optional<Role> optional = securityRoleRepository.findById(id);
        Role securityRole = optional.isPresent() ? optional.get() : null;
        assertNotNull(securityRole);
        assertNotNull(securityRole.getLocation());
        assertNotNull(securityRole.getCreatedBy());
        assertEquals("SYSTEM", securityRole.getCreatedBy());
        assertNotNull(securityRole.getCreatedAt());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = securityRoleRepository.findAll()
                                        .get(0)
                                        .getId();
        Role securityRole = entityManager.getReference(Role.class, id);
        assertNotNull(securityRole);
        assertNotNull(securityRole.getLocation());
    }

    /**
     * Creates the security role.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @return the role
     */
    public static Role createSecurityRole(String location, String name, String description) {
        Role securityRole = new Role(location, name, description);
        return securityRole;
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}

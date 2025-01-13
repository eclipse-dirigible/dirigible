/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.repository;

import jakarta.persistence.EntityManager;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.components.listeners.domain.ListenerKind;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The Class ListenerRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ListenerRepositoryTest {

    /** The listener repository. */
    @Autowired
    private ListenerRepository listenerRepository;

    /** The entity manager. */
    @Autowired
    private EntityManager entityManager;

    /** The tenant context. */
    @MockBean
    private TenantContext tenantContext;

    /** The tenant. */
    @MockBean
    @DefaultTenant
    private Tenant tenant;


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

        listenerRepository.save(new Listener("/a/b/c/l1.listener", "name1", "description", "handler1", ListenerKind.QUEUE));
        listenerRepository.save(new Listener("/a/b/c/l2.listener", "name2", "description", "handler2", ListenerKind.QUEUE));
        listenerRepository.save(new Listener("/a/b/c/l3.listener", "name3", "description", "handler3", ListenerKind.QUEUE));
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        listenerRepository.deleteAll();
    }

    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        List<Listener> all = listenerRepository.findAll();
        assertEquals(3, all.size());
        Long id = all.get(0)
                     .getId();
        Optional<Listener> optional = listenerRepository.findById(id);
        Listener listener = optional.isPresent() ? optional.get() : null;
        assertNotNull(listener);
        assertEquals("/a/b/c/l1.listener", listener.getLocation());
        assertEquals("name1", listener.getName());
        assertEquals("handler1", listener.getHandler());
        assertEquals("listener", listener.getType());
        assertEquals("description", listener.getDescription());
        assertEquals("SYSTEM", listener.getCreatedBy());
        assertNotNull(listener.getCreatedAt());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = listenerRepository.findAll()
                                    .get(0)
                                    .getId();
        Listener listener = entityManager.getReference(Listener.class, id);
        assertNotNull(listener);
        assertEquals("/a/b/c/l1.listener", listener.getLocation());
    }
}

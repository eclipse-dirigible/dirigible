/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.websockets.domain.Websocket;
import org.eclipse.dirigible.components.websockets.repository.WebsocketRepository;
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

/**
 * The Class WebsocketRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class WebsocketRepositoryTest {

    /** The websocket repository. */
    @Autowired
    private WebsocketRepository websocketRepository;

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {

        cleanup();

        websocketRepository.save(new Websocket("/a/b/c/w1.websocket", "name1", "description", "endpoint1", "handler1", "engine1"));
        websocketRepository.save(new Websocket("/a/b/c/w2.websocket", "name2", "description", "endpoint2", "handler2", "engine2"));
        websocketRepository.save(new Websocket("/a/b/c/w3.websocket", "name3", "description", "endpoint3", "handler3", "engine3"));
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        websocketRepository.deleteAll();
    }

    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        List<Websocket> all = websocketRepository.findAll();
        assertEquals(3, all.size());
        Long id = all.get(0)
                     .getId();
        Optional<Websocket> optional = websocketRepository.findById(id);
        Websocket websocket = optional.isPresent() ? optional.get() : null;
        assertNotNull(websocket);
        assertEquals("/a/b/c/w1.websocket", websocket.getLocation());
        assertEquals("name1", websocket.getName());
        assertEquals("engine1", websocket.getEngine());
        assertEquals("handler1", websocket.getHandler());
        assertEquals("endpoint1", websocket.getEndpoint());
        assertEquals("websocket", websocket.getType());
        assertEquals("description", websocket.getDescription());
        assertEquals("SYSTEM", websocket.getCreatedBy());
        assertNotNull(websocket.getCreatedAt());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = websocketRepository.findAll()
                                     .get(0)
                                     .getId();
        Websocket websocket = entityManager.getReference(Websocket.class, id);
        assertNotNull(websocket);
        assertEquals("/a/b/c/w1.websocket", websocket.getLocation());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}

/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.synchronizer;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.components.listeners.domain.ListenerKind;
import org.eclipse.dirigible.components.listeners.repository.ListenerRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class BackgroundListenerSynchronizerTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ListenerSynchronizerTest {

    /** The listener synchronizer. */
    @Autowired
    private ListenerSynchronizer listenerSynchronizer;

    /** The listener repository. */
    @Autowired
    private ListenerRepository listenerRepository;

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
        // it is needed
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        listenerRepository.deleteAll();
    }

    /**
     * Checks if is accepted path.
     */
    @Test
    public void isAcceptedPath() {
        assertTrue(listenerSynchronizer.isAccepted(Path.of("/a/b/c/l1.listener"), null));
    }

    /**
     * Checks if is accepted artefact.
     */
    @Test
    public void isAcceptedArtefact() {
        assertTrue(listenerSynchronizer.isAccepted(
                new Listener("/a/b/c/l1.listener", "name1", "description", "handler1", ListenerKind.QUEUE).getType()));
    }

    /**
     * Load.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void load() throws ParseException {
        String content =
                "{\"location\":\"/control/control.listener\",\"name\":\"/control/control\",\"kind\":\"Q\",\"handler\":\"control/handler.js\",\"description\":\"Control Listener\",\"createdBy\":\"system\",\"createdAt\":\"2017-07-06T2:53:01+0000\"}";
        List<Listener> list = listenerSynchronizer.parse("/test/test.listener", content.getBytes());
        assertNotNull(list);
        assertEquals("/test/test.listener", list.get(0)
                                                .getLocation());
    }
}

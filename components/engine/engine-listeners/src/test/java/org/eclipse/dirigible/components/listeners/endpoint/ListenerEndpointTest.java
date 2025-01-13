/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.endpoint;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.components.listeners.domain.ListenerKind;
import org.eclipse.dirigible.components.listeners.repository.ListenerRepository;
import org.eclipse.dirigible.components.listeners.service.ListenerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Class ListenerEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ListenerEndpointTest {

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;
    /** The listener service. */
    @Autowired
    private ListenerService listenerService;
    /** The listener repository. */
    @Autowired
    private ListenerRepository listenerRepository;
    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

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
     * Setup.
     */
    @BeforeEach
    public void setup() {
        cleanup();

        listenerService.save(new Listener("/a/b/c/l1.listener", "name1", "description", "handler1", ListenerKind.QUEUE));
        listenerService.save(new Listener("/a/b/c/l2.listener", "name2", "description", "handler2", ListenerKind.QUEUE));
        listenerService.save(new Listener("/a/b/c/l3.listener", "name3", "description", "handler3", ListenerKind.QUEUE));
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        listenerRepository.deleteAll();
    }

    /**
     * Find all extension points.
     *
     * @throws Exception the exception
     */
    @Test
    public void findAllExtensionPoints() throws Exception {
        mockMvc.perform(get("/services/listeners"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }
}

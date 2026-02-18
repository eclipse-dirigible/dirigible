/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.endpoint;

import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.classpath.ClasspathExpander;
import org.eclipse.dirigible.components.initializers.definition.DefinitionRepository;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Class WebEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class WebEndpointTest {

    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /** The spring security filter chain. */
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    /** The synchronization processor. */
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    /** The classpath expander. */
    @Autowired
    private ClasspathExpander classpathExpander;

    /** The synchronization watcher. */
    @Autowired
    private SynchronizationWatcher synchronizationWatcher;

    /** The definition repository. */
    @MockBean
    DefinitionRepository definitionRepository;

    /** The project json. */
    private static final String projectJson = "{\n" + "    \"guid\":\"demo\",\n" + "    \"repository\":{\n" + "        \"type\":\"git\",\n"
            + "        \"branch\":\"master\",\n" + "        \"url\":\"https://github.com/dirigiblelabs/demo.git\"\n" + "    }}";

    /**
     * Load the artefact.
     *
     * @throws Exception the exception
     */
    @Test
    public void process() throws Exception {
        String registyrFolder = synchronizationProcessor.getRegistryFolder();
        Paths.get(registyrFolder, "demo")
             .toFile()
             .mkdirs();
        Paths.get(registyrFolder, "demo", "hidden")
             .toFile()
             .mkdirs();
        Paths.get(registyrFolder, "demo", "ui")
             .toFile()
             .mkdirs();
        Files.writeString(Paths.get(registyrFolder, "demo", "project.json"), projectJson, StandardOpenOption.CREATE);
        Files.writeString(Paths.get(registyrFolder, "demo", "ui", "hello-world.txt"), "Hello World!", StandardOpenOption.CREATE);
        Files.writeString(Paths.get(registyrFolder, "demo", "hidden", "hidden.txt"), "Hidden", StandardOpenOption.CREATE);
        Files.writeString(Paths.get(registyrFolder, "demo", "ui", "index.html"), "Hidden", StandardOpenOption.CREATE);
        try {
            synchronizationWatcher.force();
            synchronizationProcessor.processSynchronizers();
            mockMvc.perform(get("/services/web/demo/ui/hello-world.txt"))
                   .andDo(print())
                   .andExpect(content().string(containsString("Hello World!")))
                   .andExpect(status().is2xxSuccessful());
            mockMvc.perform(get("/services/web/demo/ui/not-existing.txt"))
                   .andDo(print())
                   .andExpect(status().isNotFound());
            mockMvc.perform(get("/services/web/demo/ui"))
                   .andDo(print())
                   .andExpect(status().isNotFound());
            mockMvc.perform(get("/services/web/demo/ui/"))
                   .andDo(print())
                   .andExpect(status().is2xxSuccessful());
        } finally {
            FileUtils.deleteDirectory(Paths.get(registyrFolder, "demo")
                                           .toFile());
            synchronizationProcessor.processSynchronizers();
        }
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}

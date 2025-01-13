/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.extensions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.components.extensions.domain.Extension;
import org.eclipse.dirigible.components.extensions.domain.ExtensionPoint;
import org.eclipse.dirigible.components.extensions.repository.ExtensionPointRepository;
import org.eclipse.dirigible.components.extensions.repository.ExtensionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
public class ExtensionsSuiteTest {

    @Autowired
    private ExtensionPointRepository extensionPointRepository;

    @Autowired
    private ExtensionRepository extensionRepository;

    @Autowired
    private JavascriptService javascriptService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext wac;

    @BeforeEach
    public void setup() throws Exception {

        cleanup();

        // create test ExtensionPoint
        extensionPointRepository.save(createExtensionPoint("/a/b/c/test_extpoint1.extensionpoint", "test_extpoint1", "description"));
        // create test Extension
        extensionRepository.save(createExtension("/a/b/c/test_extension1.extension", "test_extension1", "description", "test_extpoint1",
                "/test_ext_module1"));
    }

    @AfterEach
    public void cleanup() throws Exception {
        extensionPointRepository.deleteAll();
        extensionRepository.deleteAll();
    }

    @Test
    public void executeExtensionsTest() throws Exception {
        javascriptService.handleRequest("extensions-tests", "extensions-get-extension-points.js", null, null, false);
        javascriptService.handleRequest("extensions-tests", "extensions-get-extensions.js", null, null, false);
    }

    @Test
    public void executeExtensionsMockTest() throws Exception {
        mockMvc.perform(get("/services/js/extensions-tests/extensions-get-extension-points.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/extensions-tests/extensions-get-extensions.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    public static ExtensionPoint createExtensionPoint(String location, String name, String description) {
        ExtensionPoint extensionPoint = new ExtensionPoint(location, name, description);
        return extensionPoint;
    }

    public static Extension createExtension(String location, String name, String description, String extensionPoint, String module) {
        Extension extension = new Extension(location, name, description, extensionPoint, module, null);
        return extension;
    }

    @SpringBootApplication
    static class TestConfiguration {
    }
}

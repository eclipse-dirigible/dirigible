/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.version.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.eclipse.dirigible.components.version.domain.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * The Class VersionServiceTest.
 */
@SpringBootTest(classes = {VersionService.class})
@ExtendWith(SpringExtension.class)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
class VersionServiceTest {

    /** The version service. */
    @Autowired
    VersionService versionService;

    /**
     * Test get version.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    void testGetVersion() throws IOException {
        Version version = versionService.getVersion();
        assertEquals("dirigible", version.getProductName());
        assertEquals("0.0.1", version.getProductVersion());
        assertEquals("test", version.getProductCommitId());
        assertEquals("https://github.com/eclipse/dirigible", version.getProductRepository());
        assertEquals("all", version.getProductType());
        assertEquals("server-spring-boot", version.getInstanceName());
        // assertEquals("local", version.getDatabaseProvider());
        // assertEquals(0, version.getModules().size());
        // TODO: Аdd assertion for engines.
    }
}

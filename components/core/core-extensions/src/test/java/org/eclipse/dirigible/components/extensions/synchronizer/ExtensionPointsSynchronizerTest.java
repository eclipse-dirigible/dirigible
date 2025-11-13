/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import org.eclipse.dirigible.components.extensions.domain.ExtensionPoint;
import org.eclipse.dirigible.components.extensions.repository.ExtensionPointRepository;
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

/**
 * The Class ExtensionPointsSynchronizerTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class ExtensionPointsSynchronizerTest {

    /** The extension point repository. */
    @Autowired
    private ExtensionPointRepository extensionPointRepository;

    /** The extension points synchronizer. */
    @Autowired
    private ExtensionPointsSynchronizer extensionPointsSynchronizer;

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {

        cleanup();

        // create test ExtensionPoints
        extensionPointRepository.save(createExtensionPoint("/a/b/c/e1.extensionpoint", "e1", "description"));
        extensionPointRepository.save(createExtensionPoint("/a/b/c/e2.extensionpoint", "e2", "description"));
        extensionPointRepository.save(createExtensionPoint("/a/b/c/e3.extensionpoint", "e3", "description"));
        extensionPointRepository.save(createExtensionPoint("/a/b/c/e4.extensionpoint", "e4", "description"));
        extensionPointRepository.save(createExtensionPoint("/a/b/c/e5.extensionpoint", "e5", "description"));
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        extensionPointRepository.deleteAll();
    }



    /**
     * Checks if is accepted.
     */
    @Test
    public void isAcceptedPath() {
        assertTrue(extensionPointsSynchronizer.isAccepted(Path.of("/a/b/c/e1.extensionpoint"), null));
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void isAcceptedArtefact() {
        assertTrue(extensionPointsSynchronizer.isAccepted(createExtensionPoint("/a/b/c/e1.extensionpoint", "e1", "description").getType()));
    }

    /**
     * Load the artefact.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void load() throws ParseException {
        String content =
                "{\"location\":\"/test/test.extensionpoint\",\"name\":\"/test/test\",\"description\":\"Test Extension Point\",\"createdBy\":\"system\",\"createdAt\":\"2017-07-06T2:53:01+0000\"}";
        List<ExtensionPoint> list = extensionPointsSynchronizer.parse("/test/test.extensionpoint", content.getBytes());
        assertNotNull(list);
        assertEquals("/test/test.extensionpoint", list.get(0)
                                                      .getLocation());
    }



    /**
     * Creates the extension point.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @return the extension point
     */
    public static ExtensionPoint createExtensionPoint(String location, String name, String description) {
        ExtensionPoint extensionPoint = new ExtensionPoint(location, name, description);
        return extensionPoint;
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}

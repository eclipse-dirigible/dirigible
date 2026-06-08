/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.synchronizer;

import jakarta.persistence.EntityManager;
import org.eclipse.dirigible.components.security.domain.Scope;
import org.eclipse.dirigible.components.security.repository.ScopeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Class SecurityScopesSynchronizerTest.
 */
@SpringBootTest(classes = {ScopeRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
class SecurityScopesSynchronizerTest {

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /** The scope repository. */
    @Autowired
    private ScopeRepository scopeRepository;

    /** The scopes synchronizer. */
    @Autowired
    private ScopesSynchronizer scopesSynchronizer;

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        scopeRepository.deleteAll();
    }

    /**
     * Checks if is accepted by path.
     */
    @Test
    public void testIsAcceptedPath() {
        assertTrue(scopesSynchronizer.isAccepted(Path.of("/a/b/c/test.scopes"), null));
    }

    /**
     * Checks if is accepted by artefact type.
     */
    @Test
    public void testIsAcceptedArtefact() {
        assertTrue(scopesSynchronizer.isAccepted(Scope.ARTEFACT_TYPE));
    }

    /**
     * Load the artefact and assert it is parsed and persisted like roles.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ParseException the parse exception
     */
    @Test
    public void testLoad() throws IOException, ParseException {
        byte[] content = SecurityScopesSynchronizerTest.class.getResourceAsStream("/META-INF/dirigible/test/test.scopes")
                                                             .readAllBytes();
        List<Scope> list = scopesSynchronizer.parse("/META-INF/dirigible/test/test.scopes", content);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("/META-INF/dirigible/test/test.scopes", list.get(0)
                                                                 .getLocation());

        Scope persisted = scopeRepository.findByKey(list.get(0)
                                                        .getKey())
                                         .orElseThrow();
        assertEquals("orders-manage", persisted.getScope());
        assertEquals(2, persisted.getRoles()
                                 .size());
        assertTrue(persisted.getRoles()
                            .contains("sample-app.Orders.OrderFullAccess"));
        assertTrue(persisted.getRoles()
                            .contains("sample-app.Orders.OrderReadOnly"));
    }
}
